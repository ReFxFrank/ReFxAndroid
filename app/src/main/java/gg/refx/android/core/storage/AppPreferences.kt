package gg.refx.android.core.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import gg.refx.android.BuildConfig
import gg.refx.android.core.network.ApiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "refx_prefs")

/**
 * Non-secret preferences in DataStore: the overridable API/web origins (§3.1) and
 * UI prefs like app-lock. Tokens live in [SecureTokenStore], never here.
 */
class AppPreferences(private val context: Context) {

    private val apiOriginKey = stringPreferencesKey("api_origin")
    private val webOriginKey = stringPreferencesKey("web_origin")
    private val appLockKey = booleanPreferencesKey("app_lock_enabled")

    val config: Flow<ApiConfig> = context.dataStore.data.map { prefs ->
        ApiConfig(
            apiOrigin = prefs[apiOriginKey] ?: BuildConfig.DEFAULT_API_ORIGIN,
            webOrigin = prefs[webOriginKey] ?: BuildConfig.DEFAULT_WEB_ORIGIN,
        )
    }

    val appLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[appLockKey] ?: false }

    suspend fun setOrigins(apiOrigin: String, webOrigin: String) {
        context.dataStore.edit {
            it[apiOriginKey] = apiOrigin
            it[webOriginKey] = webOrigin
        }
    }

    suspend fun resetOrigins() {
        context.dataStore.edit {
            it.remove(apiOriginKey)
            it.remove(webOriginKey)
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[appLockKey] = enabled }
    }
}
