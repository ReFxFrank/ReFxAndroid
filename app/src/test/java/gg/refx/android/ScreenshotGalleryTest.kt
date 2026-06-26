package gg.refx.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Upgrade
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig.Companion.PIXEL_5
import app.cash.paparazzi.Paparazzi
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.design.Eyebrow
import gg.refx.android.core.design.GlassCard
import gg.refx.android.core.design.ManageRow
import gg.refx.android.core.design.ReFxTheme
import gg.refx.android.core.design.RefxDestructiveButton
import gg.refx.android.core.design.RefxPrimaryButton
import gg.refx.android.core.design.RefxSecondaryButton
import gg.refx.android.core.design.ResourceGauge
import gg.refx.android.core.design.RoleBadge
import gg.refx.android.core.design.SectionHeader
import gg.refx.android.core.design.SkeletonBlock
import gg.refx.android.core.design.StatCard
import gg.refx.android.core.design.StatePill
import gg.refx.android.core.design.StatusChip
import org.junit.Rule
import org.junit.Test

/**
 * Visual gallery snapshots of the ReFx Glassy design system, rendered headlessly
 * by Paparazzi (no device/emulator). These render the real theme + components with
 * sample data so the UI can be reviewed as PNGs. Record with:
 *   ./gradlew recordPaparazziDebug
 * Images land in app/src/test/snapshots/images/.
 */
class ScreenshotGalleryTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = PIXEL_5)

    private fun snap(content: @Composable () -> Unit) {
        paparazzi.snapshot {
            ReFxTheme {
                Surface(color = DesignTokens.AppBackground) {
                    Column(
                        Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) { content() }
                }
            }
        }
    }

    @Test fun servers_list() = snap {
        Eyebrow("Your fleet")
        Text("Servers", style = MaterialTheme.typography.headlineMedium, color = DesignTokens.AppForegroundStrong)
        GlassCard(Modifier.fillMaxWidth()) {
            Text("2 servers need attention", color = DesignTokens.AppWarning, style = MaterialTheme.typography.bodyMedium)
        }
        ServerRow("Vanilla SMP", "node-fra-1", "Running", DesignTokens.AppSuccess)
        ServerRow("Modded Pack — Large", "node-fra-1", "Starting", DesignTokens.AppWarning)
        ServerRow("Creative Build", "node-nyc-2", "Offline", DesignTokens.AppMuted)
        ServerRow("Survival Hardcore", "node-fra-1", "Running", DesignTokens.AppSuccess)
    }

    @Test fun server_detail() = snap {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Vanilla SMP", style = MaterialTheme.typography.headlineSmall, color = DesignTokens.AppForegroundStrong, modifier = Modifier.weight(1f))
            StatePill(label = "Running", color = DesignTokens.AppSuccess)
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                ResourceGauge(fraction = 0.42f, label = "CPU", value = "42%")
                ResourceGauge(fraction = 0.67f, label = "RAM", value = "2.7 GB")
                ResourceGauge(fraction = 0.31f, label = "Disk", value = "9.4 GB")
            }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Column {
                SectionHeader(title = "Manage")
                ManageRow(title = "Files", leadingIcon = Icons.Outlined.Folder)
                ManageRow(title = "Backups", leadingIcon = Icons.Outlined.Backup)
                ManageRow(title = "Databases", leadingIcon = Icons.Outlined.Storage)
                ManageRow(title = "Upgrade / resize", leadingIcon = Icons.Outlined.Upgrade)
            }
        }
    }

    @Test fun billing() = snap {
        Eyebrow("Billing")
        Text("Billing", style = MaterialTheme.typography.headlineMedium, color = DesignTokens.AppForegroundStrong)
        StatCard(label = "Store credit", value = "$24.50", modifier = Modifier.fillMaxWidth())
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(title = "Subscriptions")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Modded Pack — Large", style = MaterialTheme.typography.titleMedium, color = DesignTokens.AppForegroundStrong, modifier = Modifier.weight(1f))
                    StatusChip(text = "Active", color = DesignTokens.AppSuccess)
                }
                Text("Renews Jul 14, 2026", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
                RefxSecondaryButton(text = "Cancel at period end", onClick = {}, fullWidth = true)
            }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "Invoices")
                InvoiceRow("#1043", "$18.00", "Paid", DesignTokens.AppSuccess)
                InvoiceRow("#1041", "$18.00", "Paid", DesignTokens.AppSuccess)
                InvoiceRow("#1038", "$6.50", "Open", DesignTokens.AppWarning)
            }
        }
    }

    @Test fun components() = snap {
        SectionHeader(title = "Buttons")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RefxPrimaryButton(text = "Save", onClick = {})
            RefxSecondaryButton(text = "Cancel", onClick = {})
        }
        RefxDestructiveButton(text = "Delete server", onClick = {}, fullWidth = true)
        SectionHeader(title = "Status")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(text = "Running", color = DesignTokens.AppSuccess)
            StatusChip(text = "Pending", color = DesignTokens.AppWarning)
            StatusChip(text = "Error", color = DesignTokens.AppDestructive)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StatePill(label = "Online", color = DesignTokens.AppSuccess)
            RoleBadge(role = "Admin")
            RoleBadge(role = "Owner")
        }
        SectionHeader(title = "Loading")
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonBlock(height = 56.dp, cornerRadius = 16.dp)
                SkeletonBlock(height = 56.dp, cornerRadius = 16.dp)
            }
        }
    }

    @Composable
    private fun ServerRow(name: String, node: String, state: String, color: androidx.compose.ui.graphics.Color) {
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(name, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium)
                    Text(node, color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
                }
                StatePill(label = state, color = color)
            }
        }
    }

    @Composable
    private fun InvoiceRow(number: String, amount: String, state: String, color: androidx.compose.ui.graphics.Color) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(number, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.bodyMedium)
                Text(amount, color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
            }
            StatusChip(text = state, color = color)
        }
    }
}
