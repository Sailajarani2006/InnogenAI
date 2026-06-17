package com.innogen.aipro.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Brand Colors ──────────────────────────────────────────────────────────────
val PrimaryBlue      = Color(0xFF1A73E8)
val PrimaryDark      = Color(0xFF0D47A1)
val SecondaryPurple  = Color(0xFF7C4DFF)
val AccentCyan       = Color(0xFF00BCD4)
val GradientStart    = Color(0xFF1A73E8)
val GradientEnd      = Color(0xFF7C4DFF)
val SurfaceLight     = Color(0xFFF8F9FA)
val SurfaceDark      = Color(0xFF121212)
val CardLight        = Color(0xFFFFFFFF)
val CardDark         = Color(0xFF1E1E1E)
val ErrorRed         = Color(0xFFD32F2F)
val SuccessGreen     = Color(0xFF388E3C)
val WarningOrange    = Color(0xFFF57C00)
val BackgroundDark   = Color(0xFF0A0A0A)

// ── Light Color Scheme ────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary            = PrimaryBlue,
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFD3E5FF),
    onPrimaryContainer = Color(0xFF001C3B),
    secondary          = SecondaryPurple,
    onSecondary        = Color.White,
    secondaryContainer = Color(0xFFECDDFF),
    onSecondaryContainer = Color(0xFF21005E),
    tertiary           = AccentCyan,
    onTertiary         = Color.White,
    background         = SurfaceLight,
    onBackground       = Color(0xFF1A1A1A),
    surface            = CardLight,
    onSurface          = Color(0xFF1A1A1A),
    surfaceVariant     = Color(0xFFF1F3F4),
    onSurfaceVariant   = Color(0xFF444746),
    error              = ErrorRed,
    onError            = Color.White,
    outline            = Color(0xFFDDE3EA)
)

// ── Dark Color Scheme ─────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary            = Color(0xFF8AB4F8),
    onPrimary          = Color(0xFF002D6E),
    primaryContainer   = PrimaryDark,
    onPrimaryContainer = Color(0xFFD3E5FF),
    secondary          = Color(0xFFCCBBFF),
    onSecondary        = Color(0xFF370083),
    secondaryContainer = Color(0xFF5600C6),
    onSecondaryContainer = Color(0xFFECDDFF),
    tertiary           = Color(0xFF80DEEA),
    onTertiary         = Color(0xFF003740),
    background         = BackgroundDark,
    onBackground       = Color(0xFFE3E3E3),
    surface            = CardDark,
    onSurface          = Color(0xFFE3E3E3),
    surfaceVariant     = Color(0xFF2A2A2A),
    onSurfaceVariant   = Color(0xFFC4C7C5),
    error              = Color(0xFFFFB4AB),
    onError            = Color(0xFF690005),
    outline            = Color(0xFF3A3A3A)
)

@Composable
fun InnoGenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = InnoGenTypography,
        shapes      = InnoGenShapes,
        content     = content
    )
}
