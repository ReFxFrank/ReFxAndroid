package gg.refx.android.app

import android.content.Context
import gg.refx.android.BuildConfig
import gg.refx.android.core.network.ApiClientFactory
import gg.refx.android.core.network.ApiConfig
import gg.refx.android.core.network.TokenRefresher
import gg.refx.android.core.session.SessionManager
import gg.refx.android.core.storage.AppPreferences
import gg.refx.android.core.storage.SecureTokenStore
import gg.refx.android.data.api.AccountApi
import gg.refx.android.data.api.AuthApi
import gg.refx.android.data.repo.AuthRepository
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
    }

    private inline fun <reified T> service(): T = retrofit.create(T::class.java)

    fun authApi(): AuthApi = service()
    fun accountApi(): AccountApi = service()

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            authApiProvider = ::authApi,
            tokens = tokenStore,
            session = session,
        )
    }
}
