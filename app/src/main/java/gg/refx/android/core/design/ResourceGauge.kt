package gg.refx.android.core.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Circular resource gauge (parity spec §2, iOS `ResourceGauge`). 80×80, track
 * white @ 0.06 lineWidth 9, round-capped progress arc tinted by [fraction]:
 * `< 0.7` primary, `< 0.9` warning, else destructive.
 */
@Composable
fun ResourceGauge(
    fraction: Float,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    gaugeSize: Dp = 80.dp,
) {
    val clamped = fraction.coerceIn(0f, 1f)
    val tint = when {
        clamped < 0.7f -> DesignTokens.AppPrimary
        clamped < 0.9f -> DesignTokens.AppWarning
        else -> DesignTokens.AppDestructive
    }
    val trackColor = Color.White.copy(alpha = 0.06f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(gaugeSize)) {
                val stroke = 9.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                if (clamped > 0f) {
                    drawArc(
                        color = tint,
                        startAngle = -90f,
                        sweepAngle = 360f * clamped,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Text(
                text = value,
                color = DesignTokens.AppForegroundStrong,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
        Eyebrow(label, modifier = Modifier.padding(top = 6.dp))
    }
}
