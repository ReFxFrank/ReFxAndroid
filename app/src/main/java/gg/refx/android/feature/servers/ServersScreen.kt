package gg.refx.android.feature.servers

import androidx.compose.runtime.Composable
import gg.refx.android.core.ui.ComingSoon
import gg.refx.android.core.ui.ScreenScaffold

/**
 * Servers list (§5). Live status pills, search, attention banner, pull-to-refresh
 * and the purchasing-gated "+" land in Milestone 2.
 */
@Composable
fun ServersScreen() {
    ScreenScaffold(eyebrow = "Your fleet", title = "Servers") {
        ComingSoon("The server list and live console arrive in Milestone 2.")
    }
}
