package com.example.coustomerapp.ui.screens.login

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.coustomerapp.ui.theme.*
import com.example.coustomerapp.ui.viewmodel.LoginState
import com.example.coustomerapp.ui.viewmodel.LoginViewModel
import com.example.coustomerapp.util.biometric.BiometricHelper

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    Log.d("LoginScreen", "LoginScreen composed")
    val context = LocalContext.current
    val biometricHelper = remember { BiometricHelper(context) }
    var loginId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showBiometricSetupDialog by remember { mutableStateOf(false) }

    val loginState by viewModel.loginState.collectAsState()

    // Animated sun rotation
    val infiniteTransition = rememberInfiniteTransition(label = "sun_anim")
    val sunRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sun_rotation"
    )

    // Pulsing glow for the logo
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    // React to login state changes
    LaunchedEffect(loginState) {
        when (loginState) {
            is LoginState.Success -> {
                val prefs = context.getSharedPreferences("swayog_settings", Context.MODE_PRIVATE)
                val hasPrompted = prefs.getBoolean("biometric_setup_prompted", false)
                if (!hasPrompted && biometricHelper.canAuthenticate()) {
                    showBiometricSetupDialog = true
                } else {
                    viewModel.resetState()
                    onLoginSuccess()
                }
            }
            else -> { /* handled in UI below */ }
        }
    }

    if (showBiometricSetupDialog) {
        AlertDialog(
            onDismissRequest = {
                context.getSharedPreferences("swayog_settings", Context.MODE_PRIVATE)
                    .edit().putBoolean("biometric_setup_prompted", true).apply()
                showBiometricSetupDialog = false
                viewModel.resetState()
                onLoginSuccess()
            },
            title = { Text("Enable Biometric Login?") },
            text = { Text("Would you like to register fingerprint unlock to sign in securely on your next launch?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        context.getSharedPreferences("swayog_settings", Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("biometric_setup_prompted", true)
                            .putBoolean("biometric_enabled", true)
                            .apply()
                        showBiometricSetupDialog = false
                        viewModel.resetState()
                        onLoginSuccess()
                    }
                ) {
                    Text("Yes, Enable", color = PrimarySolar)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        context.getSharedPreferences("swayog_settings", Context.MODE_PRIVATE)
                            .edit().putBoolean("biometric_setup_prompted", true).apply()
                        showBiometricSetupDialog = false
                        viewModel.resetState()
                        onLoginSuccess()
                    }
                ) {
                    Text("No, Skip", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            titleContentColor = Color.White,
            textContentColor = TextSecondary
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BackgroundDark,
                        Color(0xFF0D1220),
                        Color(0xFF121A30)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated Solar Logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer {
                        scaleX = glowScale
                        scaleY = glowScale
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                PrimarySolar.copy(alpha = 0.3f),
                                PrimarySolar.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(GradientStart, GradientEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.WbSunny,
                        contentDescription = "Solar Logo",
                        tint = Color.White,
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer {
                                rotationZ = sunRotation
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SWAYOG",
                style = MaterialTheme.typography.headlineLarge,
                color = PrimarySolar,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )

            Text(
                text = "Solar Energy Solutions",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Glassmorphism Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(GlassWhite)
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Sign in to monitor your solar journey",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Error message banner
                if (loginState is LoginState.Error) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = ErrorAccent.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = (loginState as LoginState.Error).message,
                            color = Color(0xFFFCA5A5),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = loginId,
                    onValueChange = { loginId = it },
                    label = { Text("Email or Login ID") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Email,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = loginState !is LoginState.Loading,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimarySolar,
                        unfocusedBorderColor = GlassBorder,
                        focusedLabelColor = PrimarySolar,
                        cursorColor = PrimarySolar,
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = loginState !is LoginState.Loading,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle password visibility",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimarySolar,
                        unfocusedBorderColor = GlassBorder,
                        focusedLabelColor = PrimarySolar,
                        cursorColor = PrimarySolar,
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = { viewModel.login(loginId, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = loginState !is LoginState.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = if (loginState is LoginState.Loading)
                                        listOf(PrimarySolar.copy(alpha = 0.5f), GradientEnd.copy(alpha = 0.5f))
                                    else
                                        listOf(PrimarySolar, GradientEnd)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (loginState is LoginState.Loading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Signing in...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            Text(
                                "Login",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Biometric button
                OutlinedButton(
                    onClick = {
                        if (biometricHelper.canAuthenticate()) {
                            biometricHelper.showBiometricPrompt(
                                activity = context as FragmentActivity,
                                onSuccess = { viewModel.loginWithSavedCredentials() },
                                onError = { _, errString ->
                                    viewModel.setErrorMessage(errString.toString())
                                },
                                onFailed = {
                                    viewModel.setErrorMessage("Biometric authentication failed")
                                }
                            )
                        } else {
                            viewModel.setErrorMessage("Biometrics not available on this device")
                        }
                    },
                    enabled = loginState !is LoginState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PrimarySolar
                    )
                ) {
                    Icon(
                        Icons.Filled.Fingerprint,
                        contentDescription = null,
                        tint = PrimarySolar,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Use Biometrics",
                        color = PrimarySolar,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
