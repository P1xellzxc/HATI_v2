package com.hati.v2.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hati.v2.presentation.animation.AntigravityEntrance
import com.hati.v2.presentation.components.MangaCard
import com.hati.v2.presentation.components.MangaButton
import com.hati.v2.presentation.components.MangaTextField
import com.hati.v2.presentation.theme.MangaColors
import com.hati.v2.presentation.theme.MangaTypography
import androidx.compose.foundation.text.BasicText
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * LoginScreen - Entry point when no auth session exists.
 * Features Manga Finance styling with Antigravity entrance animations.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MangaColors.White)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AntigravityEntrance(delay = 0L) {
            MangaCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title
                    BasicText(
                        text = "HATI²",
                        style = MangaTypography.displayLarge.copy(color = MangaColors.Black)
                    )
                    
                    BasicText(
                        text = "MANGA FINANCE",
                        style = MangaTypography.headlineMedium.copy(color = MangaColors.Black)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Email field
                    MangaTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "EMAIL",
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Password field
                    MangaTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "PASSWORD",
                        isPassword = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Error message
                    error?.let {
                        BasicText(
                            text = it,
                            style = MangaTypography.bodySmall.copy(color = MangaColors.Black)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Login button
                    MangaButton(
                        text = if (isLoading) "LOADING..." else "LOGIN",
                        onClick = {
                            scope.launch {
                                isLoading = true
                                error = null
                                try {
                                    viewModel.login(email, password)
                                    onLoginSuccess()
                                } catch (e: Exception) {
                                    error = e.message ?: "Login failed"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ViewModel() {
    
    suspend fun login(email: String, password: String) {
        supabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }
}

/**
 * Reusable MangaButton with hard-edge styling.
 */
@Composable
fun MangaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val bgColor = if (enabled) MangaColors.Black else MangaColors.Black.copy(alpha = 0.5f)
    
    androidx.compose.foundation.clickable(
        enabled = enabled,
        onClick = onClick
    ) {
        MangaCard(
            modifier = modifier,
            backgroundColor = bgColor,
            shadowOffset = if (enabled) 4.dp else 2.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = text,
                    style = MangaTypography.headlineSmall.copy(color = MangaColors.White)
                )
            }
        }
    }
}

/**
 * Reusable MangaTextField with hard-edge styling.
 */
@Composable
fun MangaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false
) {
    MangaCard(
        modifier = modifier,
        shadowOffset = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (value.isEmpty()) {
                BasicText(
                    text = placeholder,
                    style = MangaTypography.bodyLarge.copy(
                        color = MangaColors.Black.copy(alpha = 0.5f)
                    )
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MangaTypography.bodyLarge.copy(color = MangaColors.Black),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
