package gg.refx.android.feature.billing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.design.GlassCard
import gg.refx.android.core.design.RefxDestructiveButton
import gg.refx.android.core.design.RefxSecondaryButton
import gg.refx.android.core.design.SectionHeader
import gg.refx.android.core.design.StatusChip
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.core.ui.WebLink
import gg.refx.android.data.model.Invoice
import gg.refx.android.data.model.StateColors
import gg.refx.android.data.model.SubscriptionListItem

@Composable
fun BillingScreen(onBack: () -> Unit, onOpenInvoice: (String) -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val vm: BillingViewModel = viewModel(
        factory = viewModelFactory { initializer { BillingViewModel(container.billingRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.pendingCheckoutUrl) {
        state.pendingCheckoutUrl?.let { url ->
            WebLink.open(context, url)
            vm.checkoutOpened()
        }
    }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Billing", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.actionError?.let {
                Text(it, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { vm.dismissError() })
            }

            // Store credit
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(title = "Store credit")
                    AsyncState(state = state.credit, onRetry = vm::load, skeleton = { Text("Loading…", color = DesignTokens.AppMuted) }) { credit ->
                        Text(credit.balance.formatted, style = MaterialTheme.typography.headlineSmall, color = DesignTokens.AppAccentText)
                        credit.transactions.take(5).forEach { tx ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(tx.reason.name.lowercase().replace('_', ' '), color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
                                Text(tx.signedLabel, color = if (tx.isCredit) DesignTokens.AppSuccess else DesignTokens.AppForeground, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Subscriptions
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(title = "Subscriptions")
                    AsyncState(
                        state = state.subscriptions,
                        isEmpty = { it.isEmpty() },
                        emptyTitle = "No subscriptions",
                        emptyMessage = "Plans you purchase appear here.",
                        onRetry = vm::load,
                        skeleton = { Text("Loading…", color = DesignTokens.AppMuted) },
                    ) { subs ->
                        subs.forEach { sub ->
                            SubscriptionRow(
                                sub = sub,
                                busy = state.busySubId == sub.id,
                                onCancel = { vm.cancelSubscription(sub.id) },
                                onResume = { vm.resumeSubscription(sub.id) },
                            )
                        }
                    }
                }
            }

            // Invoices
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(title = "Invoices")
                    AsyncState(
                        state = state.invoices,
                        isEmpty = { it.isEmpty() },
                        emptyTitle = "No invoices",
                        emptyMessage = "Your invoices will appear here.",
                        onRetry = vm::load,
                        skeleton = { Text("Loading…", color = DesignTokens.AppMuted) },
                    ) { invoices ->
                        invoices.take(10).forEach { invoice ->
                            InvoiceRow(invoice = invoice, onClick = { onOpenInvoice(invoice.id) })
                        }
                    }
                }
            }

            // Payment methods (hosted add-card on web — §8)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(title = "Payment methods")
                    AsyncState(
                        state = state.paymentMethods,
                        isEmpty = { it.isEmpty() },
                        emptyTitle = "No cards",
                        emptyMessage = "Add a card on the web to pay automatically.",
                        onRetry = vm::load,
                        skeleton = { Text("Loading…", color = DesignTokens.AppMuted) },
                    ) { methods ->
                        methods.forEach { pm ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(pm.display, color = DesignTokens.AppForeground, style = MaterialTheme.typography.bodyMedium)
                                if (pm.isDefault) StatusChip("Default", DesignTokens.AppPrimary)
                            }
                        }
                    }
                    // External payment surface is gated by the purchasing flag (Play §8): hidden on prod.
                    if (container.purchasingEnabled) {
                        RefxSecondaryButton(
                            text = "Add card on web",
                            onClick = { WebLink.open(context, "${container.config.webOrigin}/billing/payment-methods") },
                            fullWidth = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionRow(
    sub: SubscriptionListItem,
    busy: Boolean,
    onCancel: () -> Unit,
    onResume: () -> Unit,
) {
    val (stateLabel, stateColor) = StateColors.subscription(sub.state)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(sub.productName, style = MaterialTheme.typography.titleMedium, color = DesignTokens.AppForegroundStrong, modifier = Modifier.weight(1f))
            StatusChip(stateLabel, stateColor)
        }
        Text(
            sub.renewalLabel,
            color = DesignTokens.AppMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        if (sub.canResume) {
            RefxSecondaryButton(text = "Resume", onClick = onResume, loading = busy, fullWidth = true)
        } else {
            RefxDestructiveButton(text = "Cancel at period end", onClick = onCancel, loading = busy, fullWidth = true)
        }
    }
}

@Composable
private fun InvoiceRow(invoice: Invoice, onClick: () -> Unit) {
    val (label, color) = StateColors.invoice(invoice.state)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("#${invoice.number}", color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(invoice.total.formatted, color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
        }
        StatusChip(label, color)
    }
}
