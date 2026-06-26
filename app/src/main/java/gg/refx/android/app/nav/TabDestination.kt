package gg.refx.android.app.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.ui.graphics.vector.ImageVector
import gg.refx.android.data.model.UserRole

/**
 * Role-aware bottom navigation tabs: Home, Servers, Support, (Staff), Account.
 * The Staff tab is only present for staff roles (§5).
 */
enum class TabDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val staffOnly: Boolean = false,
) {
    Home("tab/home", "Home", Icons.Outlined.Home),
    Servers("tab/servers", "Servers", Icons.Outlined.Dns),
    Support("tab/support", "Support", Icons.Outlined.SupportAgent),
    Staff("tab/staff", "Staff", Icons.Outlined.Shield, staffOnly = true),
    Account("tab/account", "Account", Icons.Outlined.AccountCircle);

    companion object {
        fun visibleFor(role: UserRole): List<TabDestination> =
            entries.filter { !it.staffOnly || role.isStaff }
    }
}
