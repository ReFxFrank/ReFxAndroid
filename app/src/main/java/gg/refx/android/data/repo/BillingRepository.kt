package gg.refx.android.data.repo

import gg.refx.android.core.network.PaginatedResult
import gg.refx.android.core.network.apiCall
import gg.refx.android.core.network.toPaginatedResult
import gg.refx.android.data.api.BillingApi
import gg.refx.android.data.model.BillingConfig
import gg.refx.android.data.model.CreditBalance
import gg.refx.android.data.model.Invoice
import gg.refx.android.data.model.PayInvoiceResult
import gg.refx.android.data.model.PaymentMethod
import gg.refx.android.data.model.Subscription
import gg.refx.android.data.model.SubscriptionListItem

class BillingRepository(private val apiProvider: () -> BillingApi) {

    suspend fun credit(): CreditBalance = apiCall { apiProvider().credit() }

    suspend fun invoices(page: Int, pageSize: Int = 25): PaginatedResult<Invoice> =
        apiCall { apiProvider().invoices(page, pageSize).toPaginatedResult() }

    suspend fun invoice(id: String): Invoice = apiCall { apiProvider().invoice(id) }

    suspend fun payInvoice(id: String, gateway: String? = null): PayInvoiceResult =
        apiCall { apiProvider().payInvoice(id, gateway) }

    suspend fun subscriptions(): List<SubscriptionListItem> = apiCall { apiProvider().subscriptions() }

    suspend fun cancelSubscription(id: String, atPeriodEnd: Boolean): Subscription =
        apiCall { apiProvider().cancelSubscription(id, atPeriodEnd) }

    suspend fun resumeSubscription(id: String): Subscription =
        apiCall { apiProvider().resumeSubscription(id) }

    suspend fun paymentMethods(): List<PaymentMethod> = apiCall { apiProvider().paymentMethods() }

    suspend fun setDefaultPaymentMethod(id: String): PaymentMethod =
        apiCall { apiProvider().setDefaultPaymentMethod(id) }

    suspend fun deletePaymentMethod(id: String) = apiCall { apiProvider().deletePaymentMethod(id) }

    suspend fun config(): BillingConfig = apiCall { apiProvider().config() }
}
