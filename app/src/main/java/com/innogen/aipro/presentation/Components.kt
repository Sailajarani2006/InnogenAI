package com.innogen.aipro.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.innogen.aipro.presentation.theme.*

// ── Gradient Button ───────────────────────────────────────────────────────────

@Composable
fun GradientButton(
    text    : String,
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
    enabled : Boolean  = true,
    icon    : ImageVector? = null,
    gradient: Brush = Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) gradient else Brush.horizontalGradient(
                listOf(Color.Gray, Color.Gray)
            ))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text       = text,
                color      = Color.White,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── App Card ──────────────────────────────────────────────────────────────────

@Composable
fun AppCard(
    modifier : Modifier = Modifier,
    elevation: Dp       = 2.dp,
    content  : @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        content   = content
    )
}

// ── Section Header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title   : String,
    subtitle: String? = null,
    action  : (@Composable () -> Unit)? = null
) {
    Row(
        modifier       = Modifier.fillMaxWidth(),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text  = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        action?.invoke()
    }
}

// ── Loading Overlay ───────────────────────────────────────────────────────────

@Composable
fun LoadingOverlay(message: String = "Loading…") {
    Box(
        modifier         = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        AppCard(modifier = Modifier.padding(32.dp)) {
            Column(
                modifier               = Modifier.padding(32.dp),
                horizontalAlignment    = Alignment.CenterHorizontally,
                verticalArrangement    = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
                Text(text = message, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    icon    : ImageVector = Icons.Default.Folder,
    title   : String,
    subtitle: String,
    action  : (@Composable () -> Unit)? = null
) {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(40.dp)
            )
        }
        Text(
            text      = title,
            style     = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.Center
        )
        Text(
            text      = subtitle,
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign  = TextAlign.Center
        )
        action?.invoke()
    }
}

// ── Status Badge ──────────────────────────────────────────────────────────────

@Composable
fun StatusBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Pulsing dot animation ─────────────────────────────────────────────────────

@Composable
fun PulsingDot(color: Color = PrimaryBlue, size: Dp = 12.dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue  = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Box(
        modifier = Modifier
            .size(size * scale)
            .clip(CircleShape)
            .background(color)
    )
}

// ── Gradient Top Bar ──────────────────────────────────────────────────────────

@Composable
fun GradientTopBar(
    title     : String,
    subtitle  : String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions   : (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd)))
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navigationIcon?.invoke()
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(
                    text  = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text  = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            actions?.invoke()
        }
    }
}

// ── Step indicator ────────────────────────────────────────────────────────────

@Composable
fun StepItem(
    stepNumber : Int,
    title      : String,
    description: String,
    isComplete : Boolean,
    isActive   : Boolean
) {
    Row(
        modifier            = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment   = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Step circle
        Box(
            modifier         = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isComplete -> SuccessGreen
                        isActive   -> PrimaryBlue
                        else       -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isComplete) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
            } else if (isActive) {
                CircularProgressIndicator(
                    modifier  = Modifier.size(18.dp),
                    color     = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text  = stepNumber.toString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    isActive   -> PrimaryBlue
                    isComplete -> SuccessGreen
                    else       -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text  = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
