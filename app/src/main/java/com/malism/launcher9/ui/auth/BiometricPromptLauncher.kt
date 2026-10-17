package com.malism.launcher9.ui.auth

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.malism.launcher9.R
import com.malism.launcher9.util.DeviceState

fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
) {
    val authenticators = DeviceState.biometricAuthenticators(activity) ?: return
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (
                    errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_CANCELED
                ) {
                    onError(errString.toString())
                }
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(activity.getString(R.string.biometric_title))
        .setSubtitle(activity.getString(R.string.biometric_subtitle))
        .setNegativeButtonText(activity.getString(R.string.use_pin))
        .setAllowedAuthenticators(authenticators)
        .build()
    prompt.authenticate(info)
}
