package gg.refx.android.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The three ReFx button styles. Each takes [fullWidth] for the full-bleed variant
 * and [loading] to show an inline spinner while disabling the press.
 */

@Composable
fun RefxPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier.applyWidth(fullWidth).height(50.dp),
        enabled = enabled && !loading,
        shape = RefxControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = DesignTokens.AppPrimary,
            contentColor = Color.White,
            disabledContainerColor = DesignTokens.AppPrimary.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.7f),
        ),
    ) {
        ButtonContent(text, loading, Color.White)
    }
}

@Composable
fun RefxSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.applyWidth(fullWidth).height(50.dp),
        enabled = enabled && !loading,
        shape = RefxControlShape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = DesignTokens.AppSecondary,
            contentColor = DesignTokens.AppForegroundStrong,
        ),
    ) {
        ButtonContent(text, loading, DesignTokens.AppForegroundStrong)
    }
}

@Composable
fun RefxDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier.applyWidth(fullWidth).height(50.dp),
        enabled = enabled && !loading,
        shape = RefxControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = DesignTokens.AppDestructive,
            contentColor = Color.White,
            disabledContainerColor = DesignTokens.AppDestructive.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.7f),
        ),
    ) {
        ButtonContent(text, loading, Color.White)
    }
}

private fun Modifier.applyWidth(fullWidth: Boolean): Modifier =
    if (fullWidth) this.fillMaxWidth() else this

@Composable
private fun ButtonContent(text: String, loading: Boolean, contentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (loading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Text(text = text)
        }
    }
}
