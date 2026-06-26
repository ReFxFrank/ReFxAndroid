package gg.refx.android.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * ReFx is dark-only (matching the iOS "ReFx Glassy" system). We expose a single
 * dark Material 3 scheme derived from [DesignTokens] and ignore light mode.
 */
private val RefxColorScheme = darkColorScheme(
    primary = DesignTokens.AppPrimary,
    onPrimary = Color.White,
    primaryContainer = DesignTokens.AppPrimaryPressed,
    onPrimaryContainer = Color.White,
    secondary = DesignTokens.AppSecondary,
    onSecondary = DesignTokens.AppForegroundStrong,
    background = DesignTokens.AppBackground,
    onBackground = DesignTokens.AppForeground,
    surface = DesignTokens.AppCard,
    onSurface = DesignTokens.AppForeground,
    surfaceVariant = DesignTokens.AppSecondary,
    onSurfaceVariant = DesignTokens.AppMuted,
    outline = DesignTokens.AppBorder,
    error = DesignTokens.AppDestructive,
    onError = Color.White,
)

@Composable
fun ReFxTheme(
    // The app is dark regardless of system setting; param kept for previews/tests.
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = RefxColorScheme,
        typography = RefxTypography,
        content = content,
    )
}
