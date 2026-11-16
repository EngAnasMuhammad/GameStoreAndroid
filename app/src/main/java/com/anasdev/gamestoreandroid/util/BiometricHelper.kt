package com.anasdev.gamestoreandroid.util

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Recursively unwraps Context until reaching a [FragmentActivity], or null if not found.
 * Safe for use inside Compose sub-hierarchies like ModalBottomSheet where Context may be wrapped.
 */
tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

object BiometricHelper {

    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

    /**
     * Checks if biometric authentication is available and enrolled on the device.
     */
    fun isBiometricAvailable(activity: FragmentActivity): Boolean {
        return BiometricManager.from(activity).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Authenticates the user with biometric prompt, executing [onSuccess] upon success.
     * If authentication fails or is cancelled/error, [onError] is called (if provided) and checkout is aborted.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Confirm Purchase",
        subtitle: String = "Verify your identity to complete checkout",
        negativeButtonText: String = "Cancel",
        onSuccess: () -> Unit,
        onError: ((errorCode: Int, errString: CharSequence) -> Unit)? = null,
        onFailed: (() -> Unit)? = null,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .setNegativeButtonText(negativeButtonText)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError?.invoke(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed?.invoke()
                }
            },
        )

        biometricPrompt.authenticate(promptInfo)
    }
}
