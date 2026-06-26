package gg.refx.android.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.design.Eyebrow

/**
 * Standard screen shell: an eyebrow + large title header over scrollable content.
 * Keeps every tab visually consistent with the iOS app's section headers.
 */
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    scrollable: Boolean = true,
    content: @Composable () -> Unit,
) {
    val base = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)
        .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
        .then(modifier)

    Column(base, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (eyebrow != null) {
                Eyebrow(eyebrow, modifier = Modifier.padding(top = 24.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = DesignTokens.AppForegroundStrong,
                modifier = if (eyebrow == null) Modifier.padding(top = 24.dp) else Modifier,
            )
        }
        content()
    }
}

/** A simple "this section is on the roadmap" placeholder for not-yet-built tabs. */
@Composable
fun ComingSoon(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = DesignTokens.AppMuted,
        modifier = Modifier.padding(top = 8.dp),
    )
}
