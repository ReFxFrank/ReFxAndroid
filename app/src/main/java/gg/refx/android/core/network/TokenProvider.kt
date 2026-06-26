package gg.refx.android.core.network

/** A pair of bearer tokens. */
data class TokenPair(val accessToken: String, val refreshToken: String)

/**
 * Read/write access to the persisted auth tokens. Implemented over
 * EncryptedSharedPreferences (§3.2). The networking layer depends only on this
 * interface so it stays free of Android storage details.
 */
interface TokenProvider {
    fun current(): TokenPair?
    fun accessToken(): String? = current()?.accessToken
    fun refreshToken(): String? = current()?.refreshToken
    fun save(tokens: TokenPair)
    fun clear()
    val isSignedIn: Boolean get() = current() != null
}
