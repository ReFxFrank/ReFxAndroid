package gg.refx.android.app.push

import com.google.firebase.messaging.FirebaseMessaging
import gg.refx.android.core.session.SessionManager
import gg.refx.android.core.session.SessionState
import gg.refx.android.data.repo.AccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Registers the FCM token with the backend on the **signed-in transition** (§7).
 * The iOS bug to avoid: uploading the token before auth and never retrying — here
 * we (re)register whenever the session becomes SignedIn, and on FCM token refresh.
 *
 * All Firebase access is guarded: on a debug build without `google-services.json`
 * Firebase isn't initialized, so token fetch fails gracefully and we simply no-op.
 */
class PushTokenRegistrar(
    private val accountRepoProvider: () -> AccountRepository,
    private val session: SessionManager,
    private val scope: CoroutineScope,
) {
    @Volatile private var lastToken: String? = null
    @Volatile private var registeredToken: String? = null

    fun start() {
        scope.launch {
            session.state.collect { state ->
                when (state) {
                    is SessionState.SignedIn -> registerCurrentToken()
                    SessionState.SignedOut -> unregister()
                    SessionState.Resolving -> Unit
                }
            }
        }
    }

    /** Called from [ReFxMessagingService.onNewToken]. */
    fun onNewToken(token: String) {
        lastToken = token
        registeredToken = null
        if (session.state.value is SessionState.SignedIn) {
            scope.launch { register(token) }
        }
    }

    private suspend fun registerCurrentToken() {
        val token = lastToken ?: fetchFcmToken() ?: return
        lastToken = token
        register(token)
    }

    private suspend fun register(token: String) {
        if (registeredToken == token) return
        runCatching { accountRepoProvider().registerPushToken(token) }
            .onSuccess { registeredToken = token }
    }

    private suspend fun unregister() {
        val token = registeredToken ?: lastToken ?: return
        runCatching { accountRepoProvider().unregisterPushToken(token) }
        registeredToken = null
    }

    private suspend fun fetchFcmToken(): String? = runCatching {
        suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
    }.getOrNull()
}
