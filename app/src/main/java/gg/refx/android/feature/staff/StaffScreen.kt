package gg.refx.android.feature.staff

import androidx.compose.runtime.Composable
import gg.refx.android.core.ui.ComingSoon
import gg.refx.android.core.ui.ScreenScaffold

/**
 * Staff / Admin hub (§5, Milestone 5). Only reachable for staff roles — the tab
 * is hidden otherwise (see TabDestination / AppShell).
 */
@Composable
fun StaffScreen() {
    ScreenScaffold(eyebrow = "Admin", title = "Staff") {
        ComingSoon("Overview, servers, nodes, users and the rest of the admin suite arrive in Milestone 5.")
    }
}
