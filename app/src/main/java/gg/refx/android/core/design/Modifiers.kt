package gg.refx.android.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/** Corner radius used across the ReFx surfaces. */
val RefxCardShape = RoundedCornerShape(16.dp)
val RefxControlShape = RoundedCornerShape(12.dp)

/**
 * Translucent glass card fill + subtle border. Equivalent to the iOS
 * `cardSurface()` modifier.
 */
fun Modifier.cardSurface(shape: androidx.compose.ui.graphics.Shape = RefxCardShape): Modifier =
    this
        .clip(shape)
        .background(DesignTokens.AppCardTranslucent)
        .border(1.dp, DesignTokens.AppBorder, shape)

/**
 * App background gradient/sheen — deep navy fading to a slightly elevated tone.
 * Equivalent to the iOS `screenBackground()`.
 */
fun Modifier.screenBackground(): Modifier =
    this
        .fillMaxSize()
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    DesignTokens.AppBackgroundElevated,
                    DesignTokens.AppBackground,
                ),
            ),
        )
