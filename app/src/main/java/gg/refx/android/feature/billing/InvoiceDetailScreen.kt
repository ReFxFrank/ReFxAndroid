package gg.refx.android.feature.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.core.network.toApiException
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.design.GlassCard
import gg.refx.android.core.design.RefxPrimaryButton
import gg.refx.android.core.design.SectionHeader
import gg.refx.android.core.design.StatePill
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.core.ui.LoadState
import gg.refx.android.core.ui.WebLink
import gg.refx.android.data.model.Invoice
import gg.refx.android.data.model.StateColors
import gg.refx.android.data.repo.BillingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InvoiceDetailUiState(
    val invoice: LoadState<Invoice> = LoadState.Loading,
    val paying: Boolean = false,
    val checkoutUrl: String? = null,
    val error: String? = null,
)

class InvoiceDetailViewModel(
    private val invoiceId: String,
    private val repo: BillingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(InvoiceDetailUiState())
    val state: StateFlow<InvoiceDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(invoice = if (it.invoice.value == null) LoadState.Loading else it.invoice) }
        viewModelScope.launch {
            runCatching { repo.invoice(invoiceId) }
                .onSuccess { inv -> _state.update { it.copy(invoice = LoadState.Loaded(inv)) } }
                .onFailure { t ->
                    _state.update { if (it.invoice.value == null) it.copy(invoice = LoadState.Failed(t.toApiException().message)) else it }
                }
        }
    }

    fun pay() {
        _state.update { it.copy(paying = true, error = null) }
        viewModelScope.launch {
            runCatching { repo.payInvoice(invoiceId) }
                .onSuccess { result ->
                    val failure = if (result.checkoutUrl == null && !result.paid) {
                        result.reason ?: "Payment could not be completed."
                    } else {
                        null
                    }
                    _state.update { it.copy(paying = false, checkoutUrl = result.checkoutUrl, error = failure) }
                    if (result.checkoutUrl == null && result.paid) load()
                }
                .onFailure { t -> _state.update { it.copy(paying = false, error = t.toApiException().message) } }
        }
    }

    fun checkoutOpened() = _state.update { it.copy(checkoutUrl = null) }
}

@Composable
fun InvoiceDetailScreen(invoiceId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val vm: InvoiceDetailViewModel = viewModel(
        factory = viewModelFactory { initializer { InvoiceDetailViewModel(invoiceId, container.billingRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.checkoutUrl) {
        state.checkoutUrl?.let { WebLink.open(context, it); vm.checkoutOpened() }
    }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = state.invoice.value?.let { "Invoice #${it.number}" } ?: "Invoice", onBack = onBack)
        AsyncState(state = state.invoice, onRetry = vm::load) { invoice ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val (label, color) = StateColors.invoice(invoice.state)
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(invoice.total.formatted, style = MaterialTheme.typography.headlineSmall, color = DesignTokens.AppForegroundStrong)
                            StatePill(label = label, color = color)
                        }
                        AmountRow("Subtotal", invoice.subtotal.formatted)
                        if (invoice.discountMinor > 0) AmountRow("Discount", "-${invoice.discount.formatted}")
                        if (invoice.taxMinor > 0) AmountRow("Tax", invoice.tax.formatted)
                        AmountRow("Paid", invoice.amountPaid.formatted)
                        AmountRow("Outstanding", invoice.outstanding.formatted)
                    }
                }

                invoice.lineItems?.takeIf { it.isNotEmpty() }?.let { items ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SectionHeader(title = "Items")
                            items.forEach { li ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${li.quantity}× ${li.description}", color = DesignTokens.AppForeground, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text(gg.refx.android.core.network.Money(li.amountMinor, invoice.currency).formatted, color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                if (state.error != null) {
                    Text(state.error!!, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall)
                }

                // Payment initiation is gated by the purchasing flag (Play §8): hidden on prod.
                if (!invoice.isPaid && container.purchasingEnabled) {
                    RefxPrimaryButton(
                        text = "Pay on web",
                        onClick = vm::pay,
                        loading = state.paying,
                        fullWidth = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = DesignTokens.AppForeground, style = MaterialTheme.typography.bodyMedium)
    }
}
