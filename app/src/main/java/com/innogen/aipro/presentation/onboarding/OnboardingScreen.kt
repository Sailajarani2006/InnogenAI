package com.innogen.aipro.presentation.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.google.accompanist.pager.*
import com.innogen.aipro.presentation.GradientButton
import com.innogen.aipro.presentation.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val emoji    : String,
    val title    : String,
    val subtitle : String,
    val gradient : List<Color>
)

private val pages = listOf(
    OnboardingPage(
        emoji    = "💡",
        title    = "Describe Your Idea",
        subtitle = "Simply type your app concept in plain English — no technical knowledge required.",
        gradient = listOf(Color(0xFF1A73E8), Color(0xFF0D47A1))
    ),
    OnboardingPage(
        emoji    = "🤖",
        title    = "AI Generates Full App Code",
        subtitle = "Our GPT-4 powered engine creates complete frontend, backend, database schema and documentation.",
        gradient = listOf(Color(0xFF7C4DFF), Color(0xFF3D1DB0))
    ),
    OnboardingPage(
        emoji    = "🚀",
        title    = "Deploy & Share Instantly",
        subtitle = "Export to GitHub, generate Docker configs, and deploy your app to the cloud in minutes.",
        gradient = listOf(Color(0xFF00BCD4), Color(0xFF006064))
    )
)

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScreen(onGetStarted: () -> Unit) {
    val pagerState = rememberPagerState()
    val scope      = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            count = pages.size,
            state = pagerState
        ) { pageIndex ->
            OnboardingPage(page = pages[pageIndex])
        }

        // Bottom controls
        Column(
            modifier            = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Pager indicators
            HorizontalPagerIndicator(
                pagerState     = pagerState,
                activeColor    = PrimaryBlue,
                inactiveColor  = Color.Gray.copy(alpha = 0.4f),
                indicatorWidth = 24.dp,
                indicatorShape = RoundedCornerShape(4.dp)
            )

            if (pagerState.currentPage == pages.lastIndex) {
                // Last page - show Get Started
                GradientButton(
                    text     = "Get Started 🚀",
                    onClick  = onGetStarted,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onGetStarted) {
                        Text("Skip", color = Color.Gray)
                    }
                    GradientButton(
                        text    = "Next →",
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.width(120.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(page: OnboardingPage) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(page.gradient + listOf(Color.White, Color.White))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Emoji icon in circle
            Box(
                modifier         = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = page.emoji, fontSize = 56.sp)
            }

            Text(
                text       = page.title,
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                textAlign  = TextAlign.Center
            )

            Text(
                text      = page.subtitle,
                style     = MaterialTheme.typography.bodyLarge,
                color     = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}
