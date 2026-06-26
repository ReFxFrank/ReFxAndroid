package gg.refx.android.feature.support

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.core.push.PushTab

/** Support tab: list → create / detail, with push deep-link consumption (§7). */
@Composable
fun SupportTab() {
    val container = LocalAppContainer.current
    val nav = rememberNavController()
    val pending by container.pushRouter.pending.collectAsStateWithLifecycle()

    LaunchedEffect(pending) {
        val route = pending ?: return@LaunchedEffect
        if (route.tab == PushTab.SUPPORT) {
            route.ticketId?.let { nav.navigate("detail/$it") }
            container.pushRouter.consume()
        }
    }

    NavHost(navController = nav, startDestination = "list") {
        composable("list") {
            SupportListScreen(
                onOpenTicket = { id -> nav.navigate("detail/$id") },
                onCreate = { nav.navigate("create") },
            )
        }
        composable("create") {
            CreateTicketScreen(
                onBack = { nav.popBackStack() },
                onCreated = { id -> nav.navigate("detail/$id") { popUpTo("list") } },
            )
        }
        composable("detail/{ticketId}") { entry ->
            TicketDetailScreen(
                ticketId = entry.arguments?.getString("ticketId").orEmpty(),
                onBack = { nav.popBackStack() },
            )
        }
    }
}
