package gg.refx.android.feature.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import gg.refx.android.BuildConfig
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.design.GlassCard
import gg.refx.android.core.design.ManageRow
import gg.refx.android.core.design.RefxDestructiveButton
import gg.refx.android.core.design.RoleBadge
import gg.refx.android.core.design.SectionHeader
import gg.refx.android.core.ui.ScreenScaffold
import gg.refx.android.core.ui.WebLink
import gg.refx.android.data.model.Account
import kotlinx.coroutines.launch

/**
 * Account tab (§5): profile header, store-credit display, About & legal link-outs,
 * app-version row and Sign out. Security/Sessions/Notifications screens are wired
 * in Milestone 3; their entry rows are present here.
 */
@Composable
fun AccountScreen(account: Account?) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val config = container.config

    ScreenScaffold(eyebrow = "Your profile", title = account?.displayName ?: "Account") {

        // Profile + credit
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = account?.email ?: "—",
                        style = MaterialTheme.typography.bodyLarge,
                        color = DesignTokens.AppForegroundStrong,
                    )
                    account?.let { RoleBadge(it.role.name) }
                }
                val credit = account?.creditBalance
                if (credit != null) {
                    Text(
                        text = "Store credit: ${credit.formatted}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DesignTokens.AppAccentText,
                    )
                }
            }
        }

        // Security & notifications (entry points; screens land in Milestone 3)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SectionHeader(title = "Security")
                ManageRow(title = "Two-factor authentication", subtitle = "Coming in Milestone 3")
                ManageRow(title = "API keys", subtitle = "Coming in Milestone 3")
                ManageRow(title = "Active sessions", subtitle = "Coming in Milestone 3")
                ManageRow(title = "Notifications", subtitle = "Coming in Milestone 3")
            }
        }

        // About & legal
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SectionHeader(title = "About & legal")
                ManageRow(
                    title = "Privacy Policy",
                    leadingIcon = Icons.Outlined.PrivacyTip,
                    modifier = Modifier.clickable { WebLink.open(context, config.webUrl("privacy")) },
                )
                ManageRow(
                    title = "Terms of Service",
                    leadingIcon = Icons.Outlined.Description,
                    modifier = Modifier.clickable { WebLink.open(context, config.webUrl("terms")) },
                )
                ManageRow(
                    title = "Help & Support",
                    leadingIcon = Icons.AutoMirrored.Outlined.HelpOutline,
                    modifier = Modifier.clickable { WebLink.open(context, config.webUrl("support")) },
                )
                ManageRow(
                    title = "App version",
                    trailing = {
                        Text(
                            text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.bodySmall,
                            color = DesignTokens.AppMuted,
                        )
                    },
                )
            }
        }

        RefxDestructiveButton(
            text = "Sign out",
            onClick = { scope.launch { container.authRepository.logout() } },
            fullWidth = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
