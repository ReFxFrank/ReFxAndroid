package gg.refx.android.core.design

import androidx.compose.ui.graphics.Color

/**
 * "ReFx Glassy" color tokens — exact values from the iOS `Theme.swift` (parity
 * spec §1a). The app is **dark-only**. Alpha-bearing tokens encode the iOS opacity
 * in the high byte (e.g. white @ 0.08 → 0x14FFFFFF).
 */
object DesignTokens {
    // Backgrounds / surfaces
    val AppBackground = Color(0xFF0A111D)        // screen base
    val AppBackgroundDeep = Color(0xFF070B12)    // gradient floor
    val AppPanel = Color(0xFF0F1828)             // glass card base (non-elevated)
    val AppCard = Color(0xFF101A2B)              // elevated base
    val AppCardElevated = Color(0xFF13203A)      // skeleton fill
    val AppPopover = Color(0xFF0C1422)           // field background

    // Brand
    val AppPrimary = Color(0xFF0072FF)           // brand blue
    val AppPrimaryPressed = Color(0xFF0052CC)    // appPrimaryDeep
    val AppSecondary = Color(0xFF58A7D3)
    val AppAccent = Color(0xFF13203A)

    // Text
    val AppForeground = Color(0xFFEEF6FF)        // bright text
    val AppForegroundStrong = Color(0xFFF3F8FF)
    val AppAccentText = Color(0xFF7DB7FF)        // accent/link text
    val AppHighlight = Color(0xFF9DCCFF)
    val AppTextSecondary = Color(0xB8D8EAFF)     // #D8EAFF @ 0.72
    val AppMuted = Color(0x8FBCD8FF)             // #BCD8FF @ 0.56
    val AppLabel = Color(0xB38CC4FF)             // #8CC4FF @ 0.70

    // Borders
    val AppBorder = Color(0x14FFFFFF)            // white @ 0.08
    val AppBorderSoft = Color(0x0DFFFFFF)        // white @ 0.05
    val AppBorderBlue = Color(0x380072FF)        // appPrimary @ 0.22

    // Status
    val AppSuccess = Color(0xFF3FB9A6)           // teal-green
    val AppWarning = Color(0xFFF5A623)           // amber
    val AppDestructive = Color(0xFFE5565B)       // red

    // Subtle blue glow used by GlassCard (appPrimary @ 0.35).
    val Glow = Color(0x590072FF)
}
