package com.innogen.aipro.presentation.generation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.innogen.aipro.domain.model.StepStatus
import com.innogen.aipro.presentation.*
import com.innogen.aipro.presentation.theme.*

@Composable
fun GenerationScreen(
    prompt              : String,
    onGenerationComplete: (String) -> Unit,
    onBack              : () -> Unit,
    viewModel           : GenerationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Start generation immediately
    LaunchedEffect(prompt) {
        viewModel.startGeneration(prompt)
    }

    // Navigate when done
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onGenerationComplete(uiState.generatedProjectId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF0F4FF), Color(0xFFFFFFFF))))
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // ── Brain animation ───────────────────────────────────────────────
            val infiniteTransition = rememberInfiniteTransition(label = "brain")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.9f, targetValue = 1.1f,
                animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "brainScale"
            )

            Box(
                modifier         = Modifier
                    .size(120.dp)
                    .background(
                        Brush.radialGradient(listOf(PrimaryBlue.copy(0.2f), Color.Transparent)),
                        RoundedCornerShape(60.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🤖", fontSize = (52 * scale).sp)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text       = "AI is Building Your App",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // Prompt preview
            Text(
                text      = "\"$prompt\"",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines  = 3
            )

            Spacer(Modifier.height(32.dp))

            // ── Steps ─────────────────────────────────────────────────────────
            Card(
                shape     = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    uiState.steps.forEach { step ->
                        StepItem(
                            stepNumber  = step.id,
                            title       = step.title,
                            description = step.description,
                            isComplete  = step.status == StepStatus.COMPLETE,
                            isActive    = step.status == StepStatus.IN_PROGRESS
                        )
                        if (step.id < uiState.steps.size) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 17.dp)
                                    .width(2.dp)
                                    .height(16.dp)
                                    .background(
                                        if (step.status == StepStatus.COMPLETE) SuccessGreen.copy(0.4f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Error state
            AnimatedVisibility(visible = uiState.isError) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ErrorRed.copy(0.1f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, null, tint = ErrorRed)
                        Column {
                            Text("Generation Failed", fontWeight = FontWeight.SemiBold, color = ErrorRed)
                            Text(uiState.errorMessage, style = MaterialTheme.typography.bodySmall, color = ErrorRed.copy(0.8f))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onBack) { Text("Go Back") }
                        GradientButton(
                            text    = "Retry",
                            onClick = { viewModel.startGeneration(prompt) },
                            modifier = Modifier.width(120.dp)
                        )
                    }
                }
            }

            // Progress indicator (when not done/error)
            if (!uiState.isComplete && !uiState.isError) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val activeStep = uiState.steps.firstOrNull { it.status == StepStatus.IN_PROGRESS }
                    if (activeStep != null) {
                        Text(
                            text  = activeStep.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    LinearProgressIndicator(
                        progress = uiState.steps.count { it.status == StepStatus.COMPLETE }.toFloat() / uiState.steps.size,
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color    = PrimaryBlue,
                        trackColor = PrimaryBlue.copy(0.2f)
                    )
                    Text(
                        text  = "${uiState.steps.count { it.status == StepStatus.COMPLETE }}/${uiState.steps.size} steps",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
