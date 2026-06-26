package gg.refx.android.feature.servers

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Nested navigation for the Servers tab: list → detail. Keeping the graph inside
 * the tab preserves the bottom-bar selection and a per-tab back stack (mirrors the
 * iOS tab-root deep-push pattern).
 */
@Composable
fun ServersTab() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "list") {
        composable("list") {
            ServersListScreen(onOpenServer = { id -> nav.navigate("detail/$id") })
        }
        composable("detail/{serverId}") { entry ->
            val id = entry.arguments?.getString("serverId").orEmpty()
            ServerDetailScreen(serverId = id, onBack = { nav.popBackStack() })
        }
    }
}
