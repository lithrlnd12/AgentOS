package com.agentos.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// AgentOS Brand Colors
object AgentOSColors {
    // Cyan/Teal - Primary accent
    val CyanPrimary = Color(0xFF00D9FF)
    val CyanLight = Color(0xFF67E8FF)
    val CyanDark = Color(0xFF00A8CC)

    // Navy Blue - Background
    val NavyDark = Color(0xFF0D1B2A)
    val NavyPrimary = Color(0xFF1B2838)
    val NavyLight = Color(0xFF1E3A5F)
    val NavySurface = Color(0xFF243B53)

    // Silver/Gray - Secondary
    val Silver = Color(0xFFB8C5D6)
    val SilverLight = Color(0xFFD4DEE8)
    val SilverDark = Color(0xFF8A99AB)

    // Semantic
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFF9800)
    val Error = Color(0xFFFF5252)
}

// AgentOS always uses dark theme to match branding
private val AgentOSDarkColorScheme = darkColorScheme(
    // Primary - Cyan accent
    primary = AgentOSColors.CyanPrimary,
    onPrimary = AgentOSColors.NavyDark,
    primaryContainer = AgentOSColors.CyanDark,
    onPrimaryContainer = AgentOSColors.CyanLight,

    // Secondary - Silver
    secondary = AgentOSColors.Silver,
    onSecondary = AgentOSColors.NavyDark,
    secondaryContainer = AgentOSColors.NavySurface,
    onSecondaryContainer = AgentOSColors.SilverLight,

    // Tertiary - Lighter cyan
    tertiary = AgentOSColors.CyanLight,
    onTertiary = AgentOSColors.NavyDark,
    tertiaryContainer = AgentOSColors.NavyLight,
    onTertiaryContainer = AgentOSColors.CyanLight,

    // Error
    error = AgentOSColors.Error,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    // Background & Surface - Navy
    background = AgentOSColors.NavyDark,
    onBackground = AgentOSColors.SilverLight,
    surface = AgentOSColors.NavyPrimary,
    onSurface = AgentOSColors.SilverLight,
    surfaceVariant = AgentOSColors.NavySurface,
    onSurfaceVariant = AgentOSColors.Silver,

    // Outline
    outline = AgentOSColors.SilverDark,
    outlineVariant = AgentOSColors.NavyLight
)

@Composable
fun AgentOSTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = AgentOSDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
