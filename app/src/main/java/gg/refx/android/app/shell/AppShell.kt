package gg.refx.android.app.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.app.nav.TabDestination
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.push.PushTab
import gg.refx.android.data.model.Account
import gg.refx.android.data.model.UserRole
import gg.refx.android.feature.account.AccountTab
import gg.refx.android.feature.home.HomeScreen
import gg.refx.android.feature.servers.ServersTab
import gg.refx.android.feature.staff.StaffScreen
import gg.refx.android.feature.support.SupportTab

/**
 * Authenticated shell: role-aware bottom navigation hosting one nested graph per
 * tab. Each tab keeps its own back stack via single-top + restore-state nav.
 */
@Composable
fun AppShell(account: Account?) {
    val container = LocalAppContainer.current
    val role = account?.globalRole ?: UserRole.CUSTOMER
    val tabs = TabDestination.visibleFor(role)
    val navController = rememberNavController()

    // A push deep-link selects the target tab; the nested tab then opens the entity
    // and clears the pending route (§7). Survives cold launch via PushRouter.
    val pending by container.pushRouter.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pending) {
        val route = pending ?: return@LaunchedEffect
        val targetTab = when (route.tab) {
            PushTab.SERVERS -> TabDestination.Servers
            PushTab.BILLING -> TabDestination.Account
            PushTab.SUPPORT -> TabDestination.Support
        }
        if (targetTab !in tabs) return@LaunchedEffect
        navController.navigate(targetTab.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(containerColor = DesignTokens.AppCard) {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DesignTokens.AppPrimary,
                            selectedTextColor = DesignTokens.AppForegroundStrong,
                            indicatorColor = DesignTokens.AppAccent,
                            unselectedIconColor = DesignTokens.AppMuted,
                            unselectedTextColor = DesignTokens.AppMuted,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TabDestination.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(TabDestination.Home.route) { HomeScreen(account) }
            composable(TabDestination.Servers.route) { ServersTab() }
            composable(TabDestination.Support.route) { SupportTab() }
            if (role.isStaff) {
                composable(TabDestination.Staff.route) { StaffScreen() }
            }
            composable(TabDestination.Account.route) { AccountTab(account) }
        }
    }
}
