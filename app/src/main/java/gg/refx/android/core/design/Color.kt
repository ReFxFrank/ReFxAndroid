package gg.refx.android.core.design

import androidx.compose.ui.graphics.Color

/**
 * "ReFx Glassy" color tokens — dark, glassy, deep-navy base with a blue primary
 * and accent text. Mirrors the iOS asset-catalog tokens in the iOS app's
 * Core/DesignSystem sources.
 *
 * NOTE: these hex values are a faithful approximation of the iOS palette derived
 * from the brief ("dark glassy deep-navy, blue primary, accent text"). They are
 * centralized here so they can be reconciled 1:1 against the iOS color
 * definitions once that repo is available. Keep token NAMES stable.
 */
object DesignTokens {
    val AppBackground = Color(0xFF0A0E1A)        // deep navy app base
    val AppBackgroundElevated = Color(0xFF0E1424) // gradient/sheen top
    val AppCard = Color(0xFF131A2E)              // translucent card fill
    val AppCardTranslucent = Color(0x99131A2E)   // glass card fill (alpha)
    val AppBorder = Color(0x33A6B6D9)            // subtle glass border
    val AppForeground = Color(0xFFC7D2E4)        // default body text
    val AppForegroundStrong = Color(0xFFF2F6FC)  // headings / strong text
    val AppMuted = Color(0xFF6B7794)             // muted/secondary text
    val AppLabel = Color(0xFF8A97B4)             // labels / eyebrows
    val AppPrimary = Color(0xFF3B82F6)           // blue primary
    val AppPrimaryPressed = Color(0xFF2563EB)
    val AppSecondary = Color(0xFF1E293B)         // secondary surface/button
    val AppAccentText = Color(0xFF60A5FA)        // accent/link text
    val AppSuccess = Color(0xFF22C55E)
    val AppWarning = Color(0xFFF59E0B)
    val AppDestructive = Color(0xFFEF4444)

    // Subtle blue glow used by GlassCard.
    val Glow = Color(0x333B82F6)
}
