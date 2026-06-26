package gg.refx.android.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gg.refx.android.core.design.StatCard
import gg.refx.android.core.ui.ComingSoon
import gg.refx.android.core.ui.ScreenScaffold
import gg.refx.android.data.model.Account

/**
 * Home / Dashboard — glance view: servers needing attention + quick stats (§5).
 * Stats are placeholders until the servers/billing summaries are wired
 * (Milestones 2–3).
 */
@Composable
fun HomeScreen(account: Account?) {
    ScreenScaffold(
        eyebrow = "Welcome back",
        title = account?.displayName ?: "ReFx",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(label = "Servers", value = "—", modifier = Modifier.weight(1f))
            StatCard(label = "Attention", value = "—", modifier = Modifier.weight(1f))
        }
        ComingSoon("Your dashboard summary will appear here once servers and billing are connected.")
    }
}
