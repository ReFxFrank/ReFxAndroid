package gg.refx.android.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.core.push.PushTab
import gg.refx.android.data.model.Account
import gg.refx.android.feature.billing.BillingScreen
import gg.refx.android.feature.billing.InvoiceDetailScreen

/**
 * Account tab: account root → billing / invoice / security / sessions / password /
 * notifications. Consumes push deep-links for the Billing tab (invoice detail, §7).
 */
@Composable
fun AccountTab(account: Account?) {
    val container = LocalAppContainer.current
    val nav = rememberNavController()
    val pending by container.pushRouter.pending.collectAsStateWithLifecycle()

    LaunchedEffect(pending) {
        val route = pending ?: return@LaunchedEffect
        if (route.tab == PushTab.BILLING) {
            nav.navigate("billing")
            route.invoiceId?.let { nav.navigate("invoice/$it") }
            container.pushRouter.consume()
        }
    }

    NavHost(navController = nav, startDestination = "account") {
        composable("account") {
            AccountScreen(
                account = account,
                onBilling = { nav.navigate("billing") },
                onSecurity = { nav.navigate("security") },
                onSessions = { nav.navigate("sessions") },
                onChangePassword = { nav.navigate("password") },
                onNotifications = { nav.navigate("notifications") },
            )
        }
        composable("billing") {
            BillingScreen(onBack = { nav.popBackStack() }, onOpenInvoice = { id -> nav.navigate("invoice/$id") })
        }
        composable("invoice/{id}") { entry ->
            InvoiceDetailScreen(invoiceId = entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
        }
        composable("security") { SecurityScreen(onBack = { nav.popBackStack() }) }
        composable("sessions") { SessionsScreen(onBack = { nav.popBackStack() }) }
        composable("password") { ChangePasswordScreen(onBack = { nav.popBackStack() }) }
        composable("notifications") { NotificationsScreen(onBack = { nav.popBackStack() }) }
    }
}
