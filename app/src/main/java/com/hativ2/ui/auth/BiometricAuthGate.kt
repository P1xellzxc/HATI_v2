package com.hativ2.ui.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.hativ2.ui.components.MangaButton
import com.hativ2.ui.theme.MangaBlack
import com.hativ2.ui.theme.NotionWhite
import com.hativ2.ui.theme.NotionYellow

/**
 * Authentication gate shown on app launch.
 *
 * Why biometric instead of PIN-only:
 *   - Biometric is faster and harder to shoulder-surf than a PIN.
 *   - We use BiometricManager.Authenticators.BIOMETRIC_STRONG | DEVICE_CREDENTIAL
 *     which allows the user to fall back to their device PIN/pattern/password.
 *   - This covers devices without fingerprint hardware.
 *
 * Why a Composable wrapper instead of doing this in Activity.onCreate:
 *   - Compose navigation is the single source of truth for screen flow.
 *   - Putting auth in a composable keeps the logic testable and co-located
 *     with the UI state it gates.
 */

sealed interface AuthState {
    data object Locked : AuthState
    data object Unlocked : AuthState
    data object NoBiometric : AuthState
    data object Error : AuthState
}

@Composable
fun BiometricAuthGate(
    onAuthenticated: @Composable () -> Unit
) {
    val context = LocalContext.current
    var authState by remember { mutableStateOf<AuthState>(AuthState.Locked) }

    // Check biometric availability on first composition
    LaunchedEffect(Unit) {
        val biometricManager = BiometricManager.from(context)
        // Why BIOMETRIC_STRONG | DEVICE_CREDENTIAL:
        // BIOMETRIC_STRONG covers fingerprint and face unlock at Class 3 security.
        // DEVICE_CREDENTIAL allows PIN/pattern/password fallback, ensuring the app
        // remains accessible even on devices without biometric hardware.
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        when (canAuthenticate) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                showBiometricPrompt(context as FragmentActivity) { success ->
                    authState = if (success) AuthState.Unlocked else AuthState.Error
                }
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                // No biometric available — skip auth gate entirely.
                // Why: Blocking the user when they have no way to authenticate
                // would make the app unusable. The device lock screen is their
                // primary defense in this case.
                authState = AuthState.NoBiometric
            }
            else -> {
                authState = AuthState.NoBiometric
            }
        }
    }

    when (authState) {
        AuthState.Unlocked, AuthState.NoBiometric -> {
            onAuthenticated()
        }
        AuthState.Locked -> {
            LockScreen(onRetry = {
                showBiometricPrompt(context as FragmentActivity) { success ->
                    authState = if (success) AuthState.Unlocked else AuthState.Error
                }
            })
        }
        AuthState.Error -> {
            LockScreen(
                errorMessage = "Authentication failed. Please try again.",
                onRetry = {
                    showBiometricPrompt(context as FragmentActivity) { success ->
                        authState = if (success) AuthState.Unlocked else AuthState.Error
                    }
                }
            )
        }
    }
}

@Composable
private fun LockScreen(
    errorMessage: String? = null,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NotionWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Lock icon box
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(NotionYellow, RoundedCornerShape(4.dp))
                    .border(2.dp, MangaBlack, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🔒",
                    style = MaterialTheme.typography.displayMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "HATI",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MangaBlack
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Unlock to access your finances",
                style = MaterialTheme.typography.bodyMedium,
                color = MangaBlack.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            MangaButton(onClick = onRetry) {
                Text("Unlock")
            }
        }
    }
}

private fun showBiometricPrompt(
    activity: FragmentActivity,
    onResult: (Boolean) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onResult(true)
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            // Why we still unlock on ERROR_NEGATIVE_BUTTON and ERROR_USER_CANCELED:
            // These mean the user explicitly dismissed the prompt, not a security
            // failure. We show the lock screen with a retry button instead of
            // force-closing the app.
            onResult(false)
        }

        override fun onAuthenticationFailed() {
            // Single attempt failed (e.g. wrong fingerprint). The system prompt
            // handles retries internally; we only get called here for logging.
        }
    }

    val prompt = BiometricPrompt(activity, executor, callback)
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock HATI")
        .setSubtitle("Verify your identity to access your finances")
        // Why setAllowedAuthenticators instead of setNegativeButtonText:
        // Using DEVICE_CREDENTIAL as a fallback removes the need for a custom
        // negative button. The system automatically shows a "Use PIN" option
        // when DEVICE_CREDENTIAL is included.
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

    prompt.authenticate(promptInfo)
}
