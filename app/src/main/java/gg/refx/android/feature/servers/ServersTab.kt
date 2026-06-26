package gg.refx.android.feature.servers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.core.push.PushTab

/**
 * Nested navigation for the Servers tab: list → detail. Keeping the graph inside
 * the tab preserves the bottom-bar selection and a per-tab back stack (mirrors the
 * iOS tab-root deep-push pattern). Consumes push deep-links to a server (§7).
 */
@Composable
fun ServersTab() {
    val container = LocalAppContainer.current
    val nav = rememberNavController()
    val pending by container.pushRouter.pending.collectAsStateWithLifecycle()

    LaunchedEffect(pending) {
        val route = pending ?: return@LaunchedEffect
        if (route.tab == PushTab.SERVERS) {
            route.serverId?.let { nav.navigate("detail/$it") }
            container.pushRouter.consume()
        }
    }

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
