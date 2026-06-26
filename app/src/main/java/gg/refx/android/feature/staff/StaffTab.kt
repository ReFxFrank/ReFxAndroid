package gg.refx.android.feature.staff

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import gg.refx.android.data.model.UserRole
import gg.refx.android.feature.support.TicketDetailScreen

/**
 * Staff/Admin tab: role-aware home → overview / queue / servers (+create) / users
 * (+detail) / nodes (+detail/add) / alerts / audit / config reads. SUPPORT sees
 * only the queue; ADMIN/OWNER see everything (parity spec §8 StaffHomeView router).
 */
@Composable
fun StaffTab(role: UserRole) {
    val nav = rememberNavController()
    fun back() = nav.popBackStack()

    NavHost(navController = nav, startDestination = "home") {
        composable("home") { StaffHome(role = role, onOpen = { route -> nav.navigate(route) }) }

        composable("overview") { OverviewScreen(onBack = ::back) }

        composable("servers") { AdminServersScreen(onBack = ::back, onCreate = { nav.navigate("servers/create") }) }
        composable("servers/create") { AdminCreateServerScreen(onBack = ::back) }

        composable("users") { AdminUsersScreen(onBack = ::back, onOpenUser = { id -> nav.navigate("user/$id") }) }
        composable("user/{id}") { e -> AdminUserDetailScreen(userId = e.arguments?.getString("id").orEmpty(), onBack = ::back) }

        composable("nodes") {
            AdminNodesScreen(
                onBack = ::back,
                onOpenNode = { id -> nav.navigate("node/$id") },
                onAddNode = { nav.navigate("nodes/create") },
            )
        }
        composable("node/{id}") { e -> AdminNodeDetailScreen(nodeId = e.arguments?.getString("id").orEmpty(), onBack = ::back) }
        composable("nodes/create") { AddNodeScreen(onBack = ::back) }

        composable("queue") { QueueScreen(onBack = ::back, onOpenTicket = { id -> nav.navigate("queueticket/$id") }) }
        composable("queueticket/{id}") { e -> TicketDetailScreen(ticketId = e.arguments?.getString("id").orEmpty(), onBack = ::back) }

        composable("alerts") { AlertsScreen(onBack = ::back) }
        composable("audit") { AuditScreen(onBack = ::back) }
        composable("coupons") { CouponsScreen(onBack = ::back) }
        composable("giftcards") { GiftCardsScreen(onBack = ::back) }
        composable("locations") { LocationsScreen(onBack = ::back) }
        composable("roles") { RolesScreen(onBack = ::back) }
    }
}
