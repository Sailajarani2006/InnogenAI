package com.innogen.aipro.presentation.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.innogen.aipro.R
import com.innogen.aipro.presentation.GradientButton
import com.innogen.aipro.presentation.theme.*

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel    : AuthViewModel = hiltViewModel()
) {
    val uiState   by viewModel.uiState.collectAsState()
    var isSignIn  by remember { mutableStateOf(true) }
    var email     by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var name      by remember { mutableStateOf("") }
    var showPwd   by remember { mutableStateOf(false) }

    val context      = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Google Sign-In launcher
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                account.idToken?.let { viewModel.signInWithGoogle(it) }
            } catch (e: ApiException) {
                /* handled by uiState */
            }
        }
    }

    // React to success
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onAuthSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF8F9FF), Color(0xFFEEF2FF))))
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // Header
            Box(
                modifier         = Modifier
                    .size(80.dp)
                    .background(
                        Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) { Text("⚡", fontSize = 40.sp) }

            Spacer(Modifier.height(16.dp))

            Text(
                text       = if (isSignIn) "Welcome Back!" else "Create Account",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text  = if (isSignIn) "Sign in to continue building amazing apps"
                        else "Join thousands of innovators today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // ── Form Card ─────────────────────────────────────────────────────
            Card(
                shape     = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Name field (sign up only)
                    AnimatedVisibility(visible = !isSignIn) {
                        OutlinedTextField(
                            value         = name,
                            onValueChange = { name = it },
                            label         = { Text("Full Name") },
                            leadingIcon   = { Icon(Icons.Default.Person, null) },
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth(),
                            shape         = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                    }

                    // Email field
                    OutlinedTextField(
                        value         = email,
                        onValueChange = { email = it },
                        label         = { Text("Email Address") },
                        leadingIcon   = { Icon(Icons.Default.Email, null) },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction    = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )

                    // Password field
                    OutlinedTextField(
                        value         = password,
                        onValueChange = { password = it },
                        label         = { Text("Password") },
                        leadingIcon   = { Icon(Icons.Default.Lock, null) },
                        trailingIcon  = {
                            IconButton(onClick = { showPwd = !showPwd }) {
                                Icon(
                                    if (showPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPwd) "Hide" else "Show"
                                )
                            }
                        },
                        visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            if (isSignIn) viewModel.signIn(email, password)
                            else viewModel.signUp(email, password, name)
                        })
                    )

                    // Error message
                    AnimatedVisibility(visible = uiState.errorMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ErrorRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Error, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                            Text(
                                text  = uiState.errorMessage ?: "",
                                color = ErrorRed,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Submit button
                    GradientButton(
                        text    = if (isSignIn) "Sign In" else "Create Account",
                        onClick = {
                            if (isSignIn) viewModel.signIn(email, password)
                            else viewModel.signUp(email, password, name)
                        },
                        enabled  = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Divider
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Divider(modifier = Modifier.weight(1f))
                        Text("or", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Divider(modifier = Modifier.weight(1f))
                    }

                    // Google Sign-In
                    OutlinedButton(
                        onClick  = {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(context.getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                            val client = GoogleSignIn.getClient(context, gso)
                            googleLauncher.launch(client.signInIntent)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text("G", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF4285F4))
                            Text("Continue with Google", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Toggle sign in / sign up
            TextButton(onClick = {
                isSignIn = !isSignIn
                viewModel.clearError()
            }) {
                Text(
                    text  = if (isSignIn) "Don't have an account? Sign Up"
                            else "Already have an account? Sign In",
                    color = PrimaryBlue,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Loading overlay
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        }
    }
}
