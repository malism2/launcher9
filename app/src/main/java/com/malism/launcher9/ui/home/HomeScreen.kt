package com.malism.launcher9.ui.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.malism.launcher9.LauncherApp
import com.malism.launcher9.R
import com.malism.launcher9.domain.model.LaunchableApp
import com.malism.launcher9.ui.AppViewModelFactory
import com.malism.launcher9.ui.components.AppCell
import com.malism.launcher9.ui.components.HomeDock
import com.malism.launcher9.ui.components.LargeButton
import com.malism.launcher9.ui.components.PinPad
import com.malism.launcher9.ui.icon.AppIcon
import com.malism.launcher9.ui.theme.SystemBarIcons
import com.malism.launcher9.util.ExternalLaunch
import com.malism.launcher9.util.SystemIntents
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onAllApps: () -> Unit,
    onSettings: () -> Unit,
    onRemove: (LaunchableApp) -> Unit,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as LauncherApp).container
    val viewModel: HomeViewModel = viewModel(factory = AppViewModelFactory(container))
    val placements by viewModel.placements.collectAsStateWithLifecycle()
    val pinConfigured by viewModel.pinConfigured.collectAsStateWithLifecycle()
    val isDefaultHome by viewModel.isDefaultHome.collectAsStateWithLifecycle()
    val showPermissionWarning by viewModel.showPermissionWarning.collectAsStateWithLifecycle()
    var showPinSetup by remember { mutableStateOf(false) }
    var pendingAfterPin by remember { mutableStateOf<(() -> Unit)?>(null) }
    val homeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { }

    fun requirePin(action: () -> Unit) {
        if (pinConfigured) {
            action()
        } else {
            pendingAfterPin = action
            viewModel.resetPinSetup()
            showPinSetup = true
        }
    }

    SystemBarIcons(lightBackground = false)

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        HomeClockHeader(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(end = 12.dp),
        )
        if (!isDefaultHome) {
            HomeBanner(
                message = stringResource(R.string.home_default_banner),
                action = stringResource(R.string.home_default_action),
                onAction = {
                    container.parentalRepository.beginExternalSession()
                    homeLauncher.launch(SystemIntents.requestHomeRole(context))
                },
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        if (!pinConfigured) {
            HomeBanner(
                message = stringResource(R.string.home_pin_banner),
                action = stringResource(R.string.home_pin_action),
                onAction = {
                    pendingAfterPin = null
                    viewModel.resetPinSetup()
                    showPinSetup = true
                },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (showPermissionWarning) {
            HomeBanner(
                message = stringResource(R.string.permission_banner),
                action = stringResource(R.string.permission_banner_action),
                onAction = { requirePin(onSettings) },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        HomeWorkspace(
            placements = placements,
            onLaunch = viewModel::launch,
            onRemove = { app -> requirePin { onRemove(app) } },
            onMove = viewModel::moveApp,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 20.dp, bottom = 20.dp),
        )
        HomeDock(
            onSettings = { requirePin(onSettings) },
            onCamera = {
                val started = ExternalLaunch.start(
                    context = context,
                    intent = SystemIntents.camera(context),
                    parental = container.parentalRepository,
                    allowWhateverOpens = true,
                )
                if (!started) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.camera_unavailable),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
            onAllApps = { requirePin(onAllApps) },
            onAdd = { requirePin(onAdd) },
            iconSize = HomeIconSize,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }

    if (showPinSetup) {
        PinSetupDialog(
            confirming = viewModel.pinConfirming,
            pin = if (viewModel.pinConfirming) viewModel.pinConfirm else viewModel.pinDraft,
            error = when (viewModel.pinError) {
                null -> null
                HomePinError.Short -> stringResource(R.string.setup_pin_short)
                HomePinError.Mismatch -> stringResource(R.string.setup_pin_mismatch)
            },
            onDigit = viewModel::appendPinDigit,
            onDelete = viewModel::deletePinDigit,
            onSubmit = {
                viewModel.submitPinSetup {
                    showPinSetup = false
                    val next = pendingAfterPin
                    pendingAfterPin = null
                    next?.invoke()
                }
            },
            onDismiss = {
                showPinSetup = false
                pendingAfterPin = null
                viewModel.resetPinSetup()
            },
        )
    }
}

@Composable
private fun HomeClockHeader(modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            val millisIntoMinute = now % 60_000L
            delay((60_000L - millisIntoMinute).coerceAtLeast(1_000L))
        }
    }
    val locale = Locale.getDefault()
    val datePattern = stringResource(R.string.home_date_pattern)
    val timeFormat = remember(locale) { SimpleDateFormat("HH:mm", locale) }
    val dateFormat = remember(locale, datePattern) { SimpleDateFormat(datePattern, locale) }
    val instant = Date(now)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            text = timeFormat.format(instant),
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 1.5.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = Color(0x66000000),
                    offset = Offset(0f, 2f),
                    blurRadius = 10f,
                ),
            ),
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            text = dateFormat.format(instant),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = Color(0x88000000),
                    offset = Offset(0f, 1f),
                    blurRadius = 6f,
                ),
            ),
            modifier = Modifier
                .padding(start = 10.dp)
                .alignByBaseline(),
        )
    }
}

@Composable
private fun HomeBanner(
    message: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.38f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = message,
            color = Color.White,
            fontSize = 17.sp,
            lineHeight = 24.sp,
        )
        TextButton(
            onClick = onAction,
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Text(action, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PinSetupDialog(
    confirming: Boolean,
    pin: String,
    error: String?,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F1EA))
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(
                stringResource(
                    if (confirming) R.string.setup_pin_confirm_title else R.string.setup_pin_title,
                ),
                style = MaterialTheme.typography.headlineMedium,
            )
            if (!confirming) {
                Text(
                    stringResource(R.string.setup_pin_body),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            PinPad(
                pin = pin,
                error = error,
                enabled = true,
                showBiometric = false,
                submitLabel = stringResource(if (confirming) R.string.ok else R.string.next),
                biometricLabel = "",
                deleteLabel = stringResource(R.string.delete),
                onDigit = onDigit,
                onDelete = onDelete,
                onSubmit = onSubmit,
                onBiometric = {},
                modifier = Modifier.padding(top = 8.dp),
            )
            LargeButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

private val HomeColumns = 4
private val HomeIconSize = 56.dp
private val HomeLabelSize = 12.sp
private val HomeCellMinHeight = 88.dp
private const val DragThresholdPx = 24f

@Composable
private fun HomeWorkspace(
    placements: List<HomePlacement>,
    onLaunch: (LaunchableApp) -> Unit,
    onRemove: (LaunchableApp) -> Unit,
    onMove: (packageName: String, toSlot: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val rows = (maxHeight / HomeCellMinHeight).toInt().coerceAtLeast(1)
        val pageSize = HomeColumns * rows
        val pageWidthPx = with(density) { maxWidth.toPx() }
        val cellWidthPx = pageWidthPx / HomeColumns

        var slots by remember { mutableStateOf(placements.associate { it.slot to it.app }) }
        var draggingSlot by remember { mutableStateOf<Int?>(null) }
        var draggingApp by remember { mutableStateOf<LaunchableApp?>(null) }
        var menuApp by remember { mutableStateOf<LaunchableApp?>(null) }
        // After a drop, keep optimistic slots until Room Flow matches (avoids old-cell flash).
        var awaitingPlacementConfirm by remember { mutableStateOf(false) }
        LaunchedEffect(placements) {
            if (draggingSlot != null) return@LaunchedEffect
            val incoming = placements.associate { it.slot to it.app }
            if (awaitingPlacementConfirm) {
                val incomingByPkg = placements.associate { it.app.packageName to it.slot }
                val localByPkg = slots.map { (slot, app) -> app.packageName to slot }.toMap()
                if (incomingByPkg != localByPkg) return@LaunchedEffect
                awaitingPlacementConfirm = false
            }
            slots = incoming
        }
        LaunchedEffect(awaitingPlacementConfirm) {
            if (!awaitingPlacementConfirm) return@LaunchedEffect
            delay(750)
            awaitingPlacementConfirm = false
        }

        var dragOffsetX by remember { mutableFloatStateOf(0f) }
        var dragOffsetY by remember { mutableFloatStateOf(0f) }
        var dragOriginX by remember { mutableFloatStateOf(0f) }
        var dragOriginY by remember { mutableFloatStateOf(0f) }
        var didDrag by remember { mutableStateOf(false) }
        var hoverSlot by remember { mutableStateOf<Int?>(null) }
        var gridHeightPx by remember { mutableIntStateOf(0) }

        val maxSlot = maxOf(slots.keys.maxOrNull() ?: -1, draggingSlot ?: -1)
        val pageCount = ((maxSlot + 1 + pageSize - 1) / pageSize).coerceAtLeast(1)
        val pagerState = rememberPagerState(pageCount = { pageCount })

        val latestSlots = rememberUpdatedState(slots)
        val latestOnMove = rememberUpdatedState(onMove)
        val latestOnLaunch = rememberUpdatedState(onLaunch)

        fun slotToLocalIndex(row: Int, column: Int): Int {
            return (rows - 1 - row) * HomeColumns + column
        }

        fun hitSlot(page: Int, x: Float, y: Float, gridH: Float): Int? {
            if (gridH <= 0f || cellWidthPx <= 0f) return null
            val cellH = gridH / rows
            val col = (x / cellWidthPx).toInt().coerceIn(0, HomeColumns - 1)
            val row = (y / cellH).toInt().coerceIn(0, rows - 1)
            val local = slotToLocalIndex(row, col)
            return page * pageSize + local
        }

        fun clampToGrid(x: Float, y: Float, gridH: Float): Offset {
            val cellH = if (rows > 0) gridH / rows else gridH
            val halfW = cellWidthPx / 2f
            val halfH = cellH / 2f
            return Offset(
                x.coerceIn(halfW, (pageWidthPx - halfW).coerceAtLeast(halfW)),
                y.coerceIn(halfH, (gridH - halfH).coerceAtLeast(halfH)),
            )
        }

        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onSizeChanged { gridHeightPx = it.height },
            ) {
                val gridH = gridHeightPx.toFloat()
                val cellH = if (rows > 0 && gridH > 0f) gridH / rows else with(density) { HomeCellMinHeight.toPx() }

                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = draggingSlot == null,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    Column(Modifier.fillMaxSize()) {
                        repeat(rows) { row ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            ) {
                                repeat(HomeColumns) { column ->
                                    val localIndex = slotToLocalIndex(row, column)
                                    val slot = page * pageSize + localIndex
                                    val app = slots[slot]
                                    val isOrigin = draggingSlot == slot
                                    val heldApp = when {
                                        isOrigin -> draggingApp
                                        else -> app
                                    }
                                    val isHoverTarget = hoverSlot == slot &&
                                        draggingSlot != null &&
                                        draggingSlot != slot
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (heldApp != null && (app != null || isOrigin)) {
                                            Box(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .pointerInput(heldApp.packageName, slot, page, rows) {
                                                        detectTapGestures(
                                                            onTap = {
                                                                if (draggingSlot == null) {
                                                                    latestOnLaunch.value(heldApp)
                                                                }
                                                            },
                                                        )
                                                    }
                                                    .pointerInput(heldApp.packageName, slot, page, rows) {
                                                        detectDragGesturesAfterLongPress(
                                                            onDragStart = {
                                                                val lifted = latestSlots.value[slot] ?: return@detectDragGesturesAfterLongPress
                                                                menuApp = null
                                                                draggingSlot = slot
                                                                draggingApp = lifted
                                                                hoverSlot = slot
                                                                didDrag = false
                                                                dragOffsetX = 0f
                                                                dragOffsetY = 0f
                                                                dragOriginX = (column + 0.5f) * cellWidthPx
                                                                dragOriginY = (row + 0.5f) * cellH
                                                                // Keep icon in place until movement — menu shows on release.
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                dragOffsetX += dragAmount.x
                                                                dragOffsetY += dragAmount.y
                                                                if (!didDrag &&
                                                                    (dragOffsetX * dragOffsetX + dragOffsetY * dragOffsetY) >=
                                                                    DragThresholdPx * DragThresholdPx
                                                                ) {
                                                                    didDrag = true
                                                                    menuApp = null
                                                                    // Lift only once the user actually drags.
                                                                    val from = draggingSlot
                                                                    if (from != null) {
                                                                        slots = latestSlots.value.toMutableMap().apply {
                                                                            remove(from)
                                                                        }
                                                                    }
                                                                }
                                                                if (!didDrag) return@detectDragGesturesAfterLongPress
                                                                val currentGridH = gridHeightPx.toFloat()
                                                                val finger = clampToGrid(
                                                                    dragOriginX + dragOffsetX,
                                                                    dragOriginY + dragOffsetY,
                                                                    currentGridH,
                                                                )
                                                                dragOffsetX = finger.x - dragOriginX
                                                                dragOffsetY = finger.y - dragOriginY
                                                                hoverSlot = hitSlot(
                                                                    page = pagerState.currentPage,
                                                                    x = finger.x,
                                                                    y = finger.y,
                                                                    gridH = currentGridH,
                                                                )
                                                            },
                                                            onDragCancel = {
                                                                // If onDragEnd already cleared the drag, a cancel from
                                                                // disposing the gesture host must not restore stale slots.
                                                                val restored = draggingApp
                                                                val from = draggingSlot
                                                                val wasDragging = didDrag
                                                                if (restored == null || from == null) {
                                                                    return@detectDragGesturesAfterLongPress
                                                                }
                                                                awaitingPlacementConfirm = false
                                                                draggingSlot = null
                                                                draggingApp = null
                                                                hoverSlot = null
                                                                didDrag = false
                                                                dragOffsetX = 0f
                                                                dragOffsetY = 0f
                                                                if (wasDragging) {
                                                                    slots = latestSlots.value.toMutableMap().apply {
                                                                        put(from, restored)
                                                                    }
                                                                }
                                                            },
                                                            onDragEnd = {
                                                                val from = draggingSlot
                                                                val movedApp = draggingApp
                                                                val target = hoverSlot
                                                                val dragged = didDrag
                                                                if (from == null || movedApp == null) {
                                                                    awaitingPlacementConfirm = false
                                                                    draggingSlot = null
                                                                    draggingApp = null
                                                                    hoverSlot = null
                                                                    didDrag = false
                                                                    return@detectDragGesturesAfterLongPress
                                                                }
                                                                if (!dragged) {
                                                                    // Long-press + release → show menu; icon never left the cell.
                                                                    awaitingPlacementConfirm = false
                                                                    menuApp = movedApp
                                                                } else if (target != null && target != from) {
                                                                    // Valid drop → land on target immediately.
                                                                    val current = latestSlots.value.toMutableMap()
                                                                    val occupant = current.remove(target)
                                                                    current[target] = movedApp
                                                                    if (occupant != null) {
                                                                        current[from] = occupant
                                                                    }
                                                                    slots = current
                                                                    awaitingPlacementConfirm = true
                                                                    latestOnMove.value(movedApp.packageName, target)
                                                                } else {
                                                                    // Invalid / same cell → return to origin.
                                                                    slots = latestSlots.value.toMutableMap().apply {
                                                                        put(from, movedApp)
                                                                    }
                                                                    awaitingPlacementConfirm = false
                                                                }
                                                                draggingSlot = null
                                                                draggingApp = null
                                                                hoverSlot = null
                                                                didDrag = false
                                                                dragOffsetX = 0f
                                                                dragOffsetY = 0f
                                                            },
                                                        )
                                                    },
                                            ) {
                                                if (app != null) {
                                                    AppCell(
                                                        app = app,
                                                        onWallpaper = true,
                                                        iconSize = HomeIconSize,
                                                        labelSize = HomeLabelSize,
                                                        onClick = {},
                                                        interactionEnabled = false,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .graphicsLayer {
                                                                // Hide in-place icon while the floating preview is up.
                                                                alpha = when {
                                                                    isOrigin -> 0f
                                                                    isHoverTarget -> 0.7f
                                                                    else -> 1f
                                                                }
                                                            },
                                                    )
                                                }
                                                if (app != null) {
                                                    DropdownMenu(
                                                        expanded = menuApp?.packageName == app.packageName,
                                                        onDismissRequest = { menuApp = null },
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    stringResource(R.string.remove_confirm),
                                                                    fontSize = 18.sp,
                                                                )
                                                            },
                                                            onClick = {
                                                                menuApp = null
                                                                onRemove(app)
                                                            },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val dragApp = draggingApp
                if (dragApp != null) {
                    val finger = clampToGrid(
                        dragOriginX + dragOffsetX,
                        dragOriginY + dragOffsetY,
                        gridH,
                    )
                    val iconPx = with(density) { HomeIconSize.toPx() }
                    AppIcon(
                        component = dragApp.component,
                        label = dragApp.label,
                        size = HomeIconSize,
                        modifier = Modifier
                            .zIndex(1f)
                            .offset {
                                IntOffset(
                                    (finger.x - iconPx / 2f).roundToInt(),
                                    (finger.y - iconPx / 2f).roundToInt(),
                                )
                            }
                            .graphicsLayer {
                                scaleX = 1.18f
                                scaleY = 1.18f
                            },
                    )
                }
            }
            if (pageCount > 1) {
                PageDots(
                    count = pageCount,
                    current = pagerState.currentPage,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun PageDots(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val active = index == current
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (active) 8.dp else 6.dp)
                    .background(
                        if (active) Color.White else Color.White.copy(alpha = 0.45f),
                        CircleShape,
                    ),
            )
        }
    }
}
