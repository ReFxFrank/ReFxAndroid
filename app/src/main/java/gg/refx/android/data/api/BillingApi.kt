package gg.refx.android.data.api

import gg.refx.android.core.network.Page
import gg.refx.android.data.model.BillingConfig
import gg.refx.android.data.model.CreditBalance
import gg.refx.android.data.model.Invoice
import gg.refx.android.data.model.PayInvoiceResult
import gg.refx.android.data.model.PaymentMethod
import gg.refx.android.data.model.Subscription
import gg.refx.android.data.model.SubscriptionListItem
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Customer billing endpoints (parity spec §5). */
interface BillingApi {

    @GET("billing/credit")
    suspend fun credit(): CreditBalance

    @GET("billing/invoices")
    suspend fun invoices(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Page<Invoice>

    @GET("billing/invoices/{id}")
    suspend fun invoice(@Path("id") id: String): Invoice

    @POST("billing/invoices/{id}/pay")
    suspend fun payInvoice(
        @Path("id") id: String,
        @Query("gateway") gateway: String? = null,
    ): PayInvoiceResult

    @GET("billing/subscriptions")
    suspend fun subscriptions(): List<SubscriptionListItem>

    @POST("billing/subscriptions/{id}/cancel")
    suspend fun cancelSubscription(
        @Path("id") id: String,
        @Query("atPeriodEnd") atPeriodEnd: Boolean,
    ): Subscription

    @POST("billing/subscriptions/{id}/resume")
    suspend fun resumeSubscription(@Path("id") id: String): Subscription

    @GET("billing/payment-methods")
    suspend fun paymentMethods(): List<PaymentMethod>

    @POST("billing/payment-methods/{id}/default")
    suspend fun setDefaultPaymentMethod(@Path("id") id: String): PaymentMethod

    @DELETE("billing/payment-methods/{id}")
    suspend fun deletePaymentMethod(@Path("id") id: String)

    @GET("billing/config")
    suspend fun config(): BillingConfig
}
