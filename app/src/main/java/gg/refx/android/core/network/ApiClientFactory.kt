package gg.refx.android.core.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the single networking stack: OkHttp (auth interceptor + 401 refresh
 * authenticator) + Retrofit (envelope-unwrapping kotlinx-serialization converter).
 *
 * The [config] is read live via [configProvider] so a Settings-screen origin
 * change takes effect without rebuilding the client. Retrofit needs a fixed
 * base URL at build time; we use the current origin and rebuild on change.
 */
object ApiClientFactory {

    fun okHttpClient(
        tokens: TokenProvider,
        refresher: TokenRefresher,
        onSignedOut: () -> Unit,
        enableLogging: Boolean,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokens))
            .authenticator(TokenAuthenticator(tokens, refresher, onSignedOut))

        if (enableLogging) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
            )
        }
        return builder.build()
    }

    fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        val kotlinxFactory = RefxJson.asConverterFactory(contentType)
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(EnvelopeConverterFactory(kotlinxFactory))
            .build()
    }
}
