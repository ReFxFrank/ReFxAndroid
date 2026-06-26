package gg.refx.android.feature.staff

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.core.design.GlassCard
import gg.refx.android.core.design.ManageRow
import gg.refx.android.core.design.SectionHeader
import gg.refx.android.core.ui.ScreenScaffold
import gg.refx.android.core.ui.WebLink
import gg.refx.android.data.model.UserRole

/** A staff hub entry: either a native [route] or a web [webPath] link-out. */
enum class StaffSection(
    val label: String,
    val icon: ImageVector,
    val route: String? = null,
    val webPath: String? = null,
    val supportVisible: Boolean = false,
) {
    OVERVIEW("Overview", Icons.Outlined.QueryStats, route = "overview"),
    QUEUE("Support queue", Icons.Outlined.SupportAgent, route = "queue", supportVisible = true),
    SERVERS("Servers", Icons.Outlined.Dns, route = "servers"),
    USERS("Users", Icons.Outlined.Group, route = "users"),
    NODES("Nodes", Icons.Outlined.Dns, route = "nodes"),
    ALERTS("Platform alerts", Icons.Outlined.Campaign, route = "alerts"),
    AUDIT("Audit log", Icons.Outlined.History, route = "audit"),
    COUPONS("Coupons", Icons.Outlined.LocalOffer, route = "coupons"),
    GIFT_CARDS("Gift cards", Icons.Outlined.CardGiftcard, route = "giftcards"),
    LOCATIONS("Locations", Icons.Outlined.Place, route = "locations"),
    ROLES("Roles", Icons.Outlined.Security, route = "roles"),
    PRODUCTS("Products", Icons.Outlined.Inventory2, webPath = "admin/products"),
    TEMPLATES("Game templates", Icons.Outlined.VideogameAsset, webPath = "admin/templates"),
    SETTINGS("Platform settings", Icons.Outlined.Settings, webPath = "admin/settings"),
    BILLING_ADMIN("Billing admin", Icons.AutoMirrored.Outlined.ReceiptLong, webPath = "admin/billing");

    companion object {
        fun visibleFor(role: UserRole): List<StaffSection> {
            // SUPPORT sees only the queue; ADMIN/OWNER see everything.
            val isAdmin = role == UserRole.ADMIN || role == UserRole.OWNER
            return entries.filter { if (isAdmin) true else it.supportVisible }
        }
    }
}

@Composable
fun StaffHome(role: UserRole, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val webOrigin = LocalAppContainer.current.config.webOrigin
    val sections = StaffSection.visibleFor(role)

    ScreenScaffold(eyebrow = "Admin", title = "Staff") {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SectionHeader(title = "Manage")
                sections.forEach { section ->
                    ManageRow(
                        title = section.label,
                        leadingIcon = section.icon,
                        subtitle = if (section.route == null) "Opens on the web" else null,
                        modifier = Modifier.clickable {
                            if (section.route != null) {
                                onOpen(section.route)
                            } else if (section.webPath != null) {
                                WebLink.open(context, "$webOrigin/${section.webPath}")
                            }
                        },
                    )
                }
            }
        }
    }
}
