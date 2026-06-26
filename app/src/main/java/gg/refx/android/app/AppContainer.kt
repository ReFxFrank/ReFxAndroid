package gg.refx.android.app

import android.content.Context
import gg.refx.android.BuildConfig
import gg.refx.android.core.network.ApiClientFactory
import gg.refx.android.core.network.ApiConfig
import gg.refx.android.app.push.PushTokenRegistrar
import gg.refx.android.core.network.TokenRefresher
import gg.refx.android.core.push.PushRouter
import gg.refx.android.core.realtime.ConsoleSocket
import gg.refx.android.core.session.SessionManager
import gg.refx.android.core.storage.AppPreferences
import gg.refx.android.core.storage.SecureTokenStore
import gg.refx.android.data.api.AccountApi
import gg.refx.android.data.api.AuthApi
import gg.refx.android.data.api.BackupsApi
import gg.refx.android.data.api.BillingApi
import gg.refx.android.data.api.DatabasesApi
import gg.refx.android.data.api.FilesApi
import gg.refx.android.data.api.ServersApi
import gg.refx.android.data.api.SupportApi
import gg.refx.android.data.repo.AccountRepository
import gg.refx.android.data.repo.AuthRepository
import gg.refx.android.data.repo.BackupsRepository
import gg.refx.android.data.repo.BillingRepository
import gg.refx.android.data.repo.DatabasesRepository
import gg.refx.android.data.repo.FilesRepository
import gg.refx.android.data.repo.ServersRepository
import gg.refx.android.data.repo.SupportRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit

/**
 * Manual dependency container (no Hilt — keeps the module light). Owns the single
 * networking stack and the per-domain repositories. The API origin is overridable
 * at runtime (§3.1): when [AppPreferences] emits a new origin, Retrofit is rebuilt
 * so subsequent calls hit the new base URL.
 */
class AppContainer(
    context: Context,
    appScope: CoroutineScope,
) {
    val tokenStore = SecureTokenStore(context)
    val preferences = AppPreferences(context)
    val session = SessionManager(tokenStore)
    val pushRouter = PushRouter()

    val purchasingEnabled: Boolean get() = BuildConfig.PURCHASING_ENABLED

    @Volatile
    var config: ApiConfig = ApiConfig(BuildConfig.DEFAULT_API_ORIGIN, BuildConfig.DEFAULT_WEB_ORIGIN)
        private set

    private val refresher = TokenRefresher { config }

    private val okHttpClient = ApiClientFactory.okHttpClient(
        tokens = tokenStore,
        refresher = refresher,
        onSignedOut = { session.signOut() },
        enableLogging = BuildConfig.DEBUG,
    )

    @Volatile
    private var retrofit: Retrofit = ApiClientFactory.retrofit(config.restBaseUrl, okHttpClient)

    val pushRegistrar = PushTokenRegistrar(
        accountRepoProvider = { accountRepository },
        session = session,
        scope = appScope,
    )

    init {
        // Seed from persisted origins and rebuild Retrofit whenever they change.
        appScope.launch {
            preferences.config.collect { newConfig ->
                if (newConfig != config) {
                    config = newConfig
                    retrofit = ApiClientFactory.retrofit(newConfig.restBaseUrl, okHttpClient)
                }
            }
        }
        // Register/unregister the FCM token on the signed-in/out transitions (§7).
        pushRegistrar.start()
    }

    private inline fun <reified T> service(): T = retrofit.create(T::class.java)

    fun authApi(): AuthApi = service()
    fun accountApi(): AccountApi = service()
    fun serversApi(): ServersApi = service()
    fun billingApi(): BillingApi = service()
    fun supportApi(): SupportApi = service()
    fun filesApi(): FilesApi = service()
    fun backupsApi(): BackupsApi = service()
    fun databasesApi(): DatabasesApi = service()

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            authApiProvider = ::authApi,
            tokens = tokenStore,
            session = session,
        )
    }

    val serversRepository: ServersRepository by lazy {
        ServersRepository(apiProvider = ::serversApi)
    }

    val accountRepository: AccountRepository by lazy {
        AccountRepository(apiProvider = ::accountApi)
    }

    val billingRepository: BillingRepository by lazy {
        BillingRepository(apiProvider = ::billingApi)
    }

    val supportRepository: SupportRepository by lazy {
        SupportRepository(apiProvider = ::supportApi)
    }

    val filesRepository: FilesRepository by lazy { FilesRepository(apiProvider = ::filesApi) }
    val backupsRepository: BackupsRepository by lazy { BackupsRepository(apiProvider = ::backupsApi) }
    val databasesRepository: DatabasesRepository by lazy { DatabasesRepository(apiProvider = ::databasesApi) }

    /** A fresh console socket per server-detail screen; the owner disposes it. */
    fun createConsoleSocket(): ConsoleSocket =
        ConsoleSocket(configProvider = { config }, tokens = tokenStore, refresher = refresher)
}
