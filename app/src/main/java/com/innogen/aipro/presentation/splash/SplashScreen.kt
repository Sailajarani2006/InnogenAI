package com.innogen.aipro.presentation.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.google.firebase.auth.FirebaseAuth
import com.innogen.aipro.presentation.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToDashboard : () -> Unit
) {
    // Animation states
    var logoVisible    by remember { mutableStateOf(false) }
    var taglineVisible by remember { mutableStateOf(false) }
    var dotsVisible    by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue  = if (logoVisible) 1f else 0.3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label        = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue  = if (logoVisible) 1f else 0f,
        animationSpec = tween(600),
        label        = "logoAlpha"
    )

    LaunchedEffect(Unit) {
        delay(300);  logoVisible    = true
        delay(500);  taglineVisible = true
        delay(400);  dotsVisible    = true
        delay(1200)
        // Navigate based on auth state
        if (FirebaseAuth.getInstance().currentUser != null) {
            onNavigateToDashboard()
        } else {
            onNavigateToOnboarding()
        }
    }

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Logo ──────────────────────────────────────────────────────────
            Box(
                modifier         = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
                    .size(100.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text     = "⚡",
                    fontSize = 52.sp
                )
            }

            // ── App name ──────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = logoVisible,
                enter   = fadeIn() + slideInVertically { it / 2 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = "InnoGen AI Pro",
                        style      = MaterialTheme.typography.headlineMedium,
                        color      = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Tagline ───────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = taglineVisible,
                enter   = fadeIn(tween(600)) + expandVertically()
            ) {
                Text(
                    text      = "Turn Ideas into Apps Instantly",
                    style     = MaterialTheme.typography.titleMedium,
                    color     = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Loading dots ──────────────────────────────────────────────────
            AnimatedVisibility(visible = dotsVisible, enter = fadeIn()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { index ->
                        BouncingDot(delayMs = index * 150)
                    }
                }
            }
        }
    }
}

@Composable
private fun BouncingDot(delayMs: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce$delayMs")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = -12f,
        animationSpec = infiniteRepeatable(
            animation  = tween(400, delayMillis = delayMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )
    Box(
        modifier = Modifier
            .offset(y = offsetY.dp)
            .size(10.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.8f))
    )
}
