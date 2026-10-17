package com.malism.launcher9.data.repository

import android.content.Context
import android.os.SystemClock
import com.malism.launcher9.data.local.LauncherDatabase
import com.malism.launcher9.data.local.entity.HomeAppEntity
import com.malism.launcher9.data.local.entity.ParentalSettingEntity
import com.malism.launcher9.domain.model.BootState
import com.malism.launcher9.domain.model.LaunchableApp
import com.malism.launcher9.domain.model.PinResult
import com.malism.launcher9.util.DeviceState
import com.malism.launcher9.util.PinHasher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class ParentalRepository(
    private val context: Context,
    database: LauncherDatabase,
    appScope: CoroutineScope,
) {
    private val homeDao = database.homeAppDao()
    private val settingDao = database.parentalSettingDao()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val homePackages = MutableStateFlow<Set<String>>(emptySet())
    private val sessionAllowed = MutableStateFlow<Set<String>>(emptySet())
    private val temporaryAllowed = MutableStateFlow<Set<String>>(emptySet())
    private val setupCompleteState = MutableStateFlow(false)
    private val boot = MutableStateFlow(BootState.Loading)

    private val externalSession = AtomicBoolean(false)
    private var externalStartedAt = 0L

    private val lock = Any()
    private var failures = 0
    private var lockedUntil = 0L

    val homeApps: Flow<List<HomeAppEntity>> = homeDao.observe()
    val settings: Flow<ParentalSettingEntity?> = settingDao.observe()
    val setupComplete: StateFlow<Boolean> = setupCompleteState
    val bootState: StateFlow<BootState> = boot
    val pinConfigured: StateFlow<Boolean> = settings
        .map { isPinConfigured(it) }
        .stateIn(appScope, SharingStarted.Eagerly, false)

    init {
        appScope.launch {
            homeDao.observe().collect { apps ->
                homePackages.value = apps.map { it.packageName }.toSet()
            }
        }
        appScope.launch {
            settingDao.observe().collect { setting ->
                val ready = setting?.setupComplete == true
                setupCompleteState.value = ready
                boot.value = when {
                    setting == null -> BootState.Loading
                    ready -> BootState.Ready
                    else -> BootState.Loading
                }
            }
        }
    }

    suspend fun ensureReady() {
        val current = settingDao.get()
        if (current == null) {
            settingDao.upsert(
                ParentalSettingEntity(
                    pinSalt = ByteArray(0),
                    pinHash = ByteArray(0),
                    biometricEnabled = false,
                    setupComplete = true,
                ),
            )
        } else if (!current.setupComplete) {
            settingDao.upsert(current.copy(setupComplete = true))
        }
    }

    suspend fun seedDefaultHomeAppsIfNeeded(apps: List<LaunchableApp>) {
        if (prefs.getBoolean(KEY_DEFAULT_SEEDED, false)) return
        prefs.edit().putBoolean(KEY_DEFAULT_SEEDED, true).apply()
        if (homeDao.maxSort() > 0) return
        if (apps.isEmpty()) return
        addToHome(apps)
    }

    suspend fun isSetupComplete(): Boolean = settingDao.get()?.setupComplete == true

    suspend fun setPin(pin: String, biometricEnabled: Boolean = false) {
        val (salt, hash) = withContext(Dispatchers.Default) { PinHasher.create(pin) }
        settingDao.upsert(
            ParentalSettingEntity(
                pinSalt = salt,
                pinHash = hash,
                biometricEnabled = biometricEnabled,
                setupComplete = true,
            ),
        )
        synchronized(lock) {
            failures = 0
            lockedUntil = 0L
        }
    }

    suspend fun replacePin(pin: String) {
        val current = settingDao.get() ?: return
        val (salt, hash) = withContext(Dispatchers.Default) { PinHasher.create(pin) }
        settingDao.upsert(current.copy(pinSalt = salt, pinHash = hash))
        synchronized(lock) {
            failures = 0
            lockedUntil = 0L
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        val current = settingDao.get() ?: return
        settingDao.upsert(current.copy(biometricEnabled = enabled))
    }

    suspend fun markSetupComplete() {
        val current = settingDao.get() ?: return
        settingDao.upsert(current.copy(setupComplete = true))
    }

    suspend fun addToHome(apps: List<LaunchableApp>) {
        val occupied = homeDao.getAll().map { it.sortOrder }.toMutableSet()
        fun nextSlot(): Int {
            var slot = 0
            while (slot in occupied) slot += 1
            occupied += slot
            return slot
        }
        val existing = homePackages.value
        val now = System.currentTimeMillis()
        val entities = apps
            .filter { it.packageName !in existing }
            .map { app ->
                HomeAppEntity(
                    packageName = app.packageName,
                    label = app.label,
                    sortOrder = nextSlot(),
                    addedAt = now,
                )
            }
        if (entities.isNotEmpty()) homeDao.insert(entities)
    }

    suspend fun removeFromHome(packageName: String) {
        homeDao.delete(packageName)
        revokeSession(packageName)
    }

    suspend fun moveHomeApp(packageName: String, toSlot: Int) {
        if (toSlot < 0) return
        val all = homeDao.getAll()
        val moving = all.find { it.packageName == packageName } ?: return
        if (moving.sortOrder == toSlot) return
        val occupant = all.find { it.sortOrder == toSlot && it.packageName != packageName }
        val updates = buildList {
            add(moving.copy(sortOrder = toSlot))
            if (occupant != null) {
                add(occupant.copy(sortOrder = moving.sortOrder))
            }
        }
        homeDao.insert(updates)
    }

    suspend fun verifyPin(pin: String): PinResult {
        val now = SystemClock.elapsedRealtime()
        synchronized(lock) {
            if (now < lockedUntil) {
                val seconds = ((lockedUntil - now) / 1000L).toInt().coerceAtLeast(1)
                return PinResult.Locked(seconds)
            }
        }
        val setting = settingDao.get() ?: return PinResult.NotSet
        if (!isPinConfigured(setting)) return PinResult.NotSet
        val ok = withContext(Dispatchers.Default) {
            PinHasher.verify(pin, setting.pinSalt, setting.pinHash)
        }
        synchronized(lock) {
            if (ok) {
                failures = 0
                return PinResult.Ok
            }
            failures += 1
            if (failures >= 5) {
                lockedUntil = SystemClock.elapsedRealtime() + 30_000L
                failures = 0
                return PinResult.Locked(30)
            }
            return PinResult.Wrong
        }
    }

    fun allowSession(packageName: String) {
        sessionAllowed.update { it + packageName }
    }

    fun revokeSession(packageName: String) {
        sessionAllowed.update { it - packageName }
    }

    fun revokeAllSessions() {
        sessionAllowed.value = emptySet()
    }

    fun allowTemporary(packageName: String) {
        if (packageName.isBlank()) return
        temporaryAllowed.update { it + packageName }
    }

    fun beginExternalSession() {
        externalStartedAt = SystemClock.elapsedRealtime()
        externalSession.set(true)
    }

    fun endExternalSession() {
        if (SystemClock.elapsedRealtime() - externalStartedAt < 800L) return
        externalSession.set(false)
        temporaryAllowed.value = emptySet()
    }

    fun cancelExternalSession() {
        externalStartedAt = 0L
        externalSession.set(false)
    }

    fun onForegroundChanged(previous: String?, current: String?) {
        if (previous == null || current == null || previous == current) return
        if (current == context.packageName) return
        revokeSession(previous)
    }

    fun shouldBlock(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        if (!setupCompleteState.value) return false
        if (DeviceState.isExemptPackage(packageName, context.packageName)) return false
        if (externalSession.get()) {
            val age = SystemClock.elapsedRealtime() - externalStartedAt
            if (age > EXTERNAL_SESSION_MS) {
                externalSession.set(false)
            } else {
                allowTemporary(packageName)
                return false
            }
        }
        if (packageName in homePackages.value) return false
        if (packageName in sessionAllowed.value) return false
        if (packageName in temporaryAllowed.value) return false
        return true
    }

    private fun isPinConfigured(setting: ParentalSettingEntity?): Boolean {
        return setting != null && setting.pinSalt.isNotEmpty() && setting.pinHash.isNotEmpty()
    }

    private companion object {
        const val EXTERNAL_SESSION_MS = 3 * 60_000L
        const val PREFS_NAME = "launcher9"
        const val KEY_DEFAULT_SEEDED = "default_home_seeded"
    }
}
