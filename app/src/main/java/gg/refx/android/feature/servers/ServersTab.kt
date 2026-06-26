package gg.refx.android.feature.servers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.core.push.PushTab
import gg.refx.android.core.ui.WebLink
import gg.refx.android.feature.servers.sections.BackupsScreen
import gg.refx.android.feature.servers.sections.DatabasesScreen
import gg.refx.android.feature.servers.sections.FilesScreen
import gg.refx.android.feature.servers.sections.SchedulesScreen
import gg.refx.android.feature.servers.sections.ServerSettingsScreen
import gg.refx.android.feature.servers.sections.SubUsersScreen
import gg.refx.android.feature.servers.sections.SwitchGameScreen
import gg.refx.android.feature.servers.sections.UpgradeScreen

/**
 * Nested navigation for the Servers tab: list → detail. Keeping the graph inside
 * the tab preserves the bottom-bar selection and a per-tab back stack (mirrors the
 * iOS tab-root deep-push pattern). Consumes push deep-links to a server (§7).
 */
@Composable
fun ServersTab() {
    val container = LocalAppContainer.current
    val context = LocalContext.current
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
            ServerDetailScreen(
                serverId = id,
                onBack = { nav.popBackStack() },
                onOpenSection = { section, shortId ->
                    when (section) {
                        ServerSection.FILES -> nav.navigate("files/$id")
                        ServerSection.BACKUPS -> nav.navigate("backups/$id")
                        ServerSection.DATABASES -> nav.navigate("databases/$id")
                        ServerSection.SCHEDULES -> nav.navigate("schedules/$id")
                        ServerSection.SUB_USERS -> nav.navigate("subusers/$id")
                        ServerSection.SETTINGS -> nav.navigate("settings/$id")
                        ServerSection.SWITCH_GAME -> nav.navigate("switchgame/$id")
                        ServerSection.UPGRADE -> nav.navigate("upgrade/$id")
                        else -> WebLink.open(context, "${container.config.webOrigin}/servers/$shortId/${section.webPath}")
                    }
                },
            )
        }
        composable("files/{serverId}") { e ->
            FilesScreen(serverId = e.arguments?.getString("serverId").orEmpty(), onBack = { nav.popBackStack() })
        }
        composable("backups/{serverId}") { e ->
            BackupsScreen(serverId = e.arguments?.getString("serverId").orEmpty(), onBack = { nav.popBackStack() })
        }
        composable("databases/{serverId}") { e ->
            DatabasesScreen(serverId = e.arguments?.getString("serverId").orEmpty(), onBack = { nav.popBackStack() })
        }
        composable("schedules/{serverId}") { e ->
            SchedulesScreen(serverId = e.arguments?.getString("serverId").orEmpty(), onBack = { nav.popBackStack() })
        }
        composable("subusers/{serverId}") { e ->
            SubUsersScreen(serverId = e.arguments?.getString("serverId").orEmpty(), onBack = { nav.popBackStack() })
        }
        composable("settings/{serverId}") { e ->
            ServerSettingsScreen(serverId = e.arguments?.getString("serverId").orEmpty(), onBack = { nav.popBackStack() })
        }
        composable("switchgame/{serverId}") { e ->
            SwitchGameScreen(serverId = e.arguments?.getString("serverId").orEmpty(), onBack = { nav.popBackStack() })
        }
        composable("upgrade/{serverId}") { e ->
            UpgradeScreen(serverId = e.arguments?.getString("serverId").orEmpty(), onBack = { nav.popBackStack() })
        }
    }
}
