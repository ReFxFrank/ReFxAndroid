package gg.refx.android.feature.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gg.refx.android.core.network.toApiException
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.CreditBalance
import gg.refx.android.data.model.Invoice
import gg.refx.android.data.model.PaymentMethod
import gg.refx.android.data.model.SubscriptionListItem
import gg.refx.android.data.repo.BillingRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BillingUiState(
    val credit: LoadState<CreditBalance> = LoadState.Loading,
    val subscriptions: LoadState<List<SubscriptionListItem>> = LoadState.Loading,
    val invoices: LoadState<List<Invoice>> = LoadState.Loading,
    val paymentMethods: LoadState<List<PaymentMethod>> = LoadState.Loading,
    val isRefreshing: Boolean = false,
    val busySubId: String? = null,
    val actionError: String? = null,
    val pendingCheckoutUrl: String? = null,
)

/**
 * Billing overview (parity spec §8): loads credit + subscriptions + invoices +
 * payment methods in parallel. Subscriptions can be cancelled/resumed; invoices
 * are paid on the web (returns a checkout URL to open).
 */
class BillingViewModel(private val repo: BillingRepository) : ViewModel() {

    private val _state = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val creditD = async { runCatching { repo.credit() } }
            val subsD = async { runCatching { repo.subscriptions() } }
            val invoicesD = async { runCatching { repo.invoices(1).items } }
            val methodsD = async { runCatching { repo.paymentMethods() } }

            val credit = creditD.await()
            val subs = subsD.await()
            val invoices = invoicesD.await()
            val methods = methodsD.await()

            fun <T> Result<T>.toLoad(): LoadState<T> =
                fold({ LoadState.Loaded(it) }, { LoadState.Failed(it.toApiException().message) })

            _state.update { s ->
                s.copy(
                    credit = credit.toLoad(),
                    subscriptions = subs.toLoad(),
                    invoices = invoices.toLoad(),
                    paymentMethods = methods.toLoad(),
                    isRefreshing = false,
                )
            }
        }
    }

    fun cancelSubscription(id: String, atPeriodEnd: Boolean = true) = mutateSubscription(id) {
        repo.cancelSubscription(id, atPeriodEnd)
    }

    fun resumeSubscription(id: String) = mutateSubscription(id) {
        repo.resumeSubscription(id)
    }

    private fun mutateSubscription(id: String, action: suspend () -> Any) {
        _state.update { it.copy(busySubId = id, actionError = null) }
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess {
                    _state.update { it.copy(busySubId = null) }
                    reloadSubscriptions()
                }
                .onFailure { t -> _state.update { it.copy(busySubId = null, actionError = t.toApiException().message) } }
        }
    }

    private fun reloadSubscriptions() {
        viewModelScope.launch {
            runCatching { repo.subscriptions() }
                .onSuccess { subs -> _state.update { it.copy(subscriptions = LoadState.Loaded(subs)) } }
        }
    }

    fun payInvoice(id: String) {
        viewModelScope.launch {
            runCatching { repo.payInvoice(id) }
                .onSuccess { result ->
                    if (result.checkoutUrl != null) {
                        _state.update { it.copy(pendingCheckoutUrl = result.checkoutUrl) }
                    } else if (result.paid) {
                        load()
                    }
                }
                .onFailure { t -> _state.update { it.copy(actionError = t.toApiException().message) } }
        }
    }

    fun checkoutOpened() = _state.update { it.copy(pendingCheckoutUrl = null) }
    fun dismissError() = _state.update { it.copy(actionError = null) }
}
