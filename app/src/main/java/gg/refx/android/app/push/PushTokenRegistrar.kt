package gg.refx.android.app.push

import com.google.firebase.messaging.FirebaseMessaging
import gg.refx.android.core.session.SessionManager
import gg.refx.android.core.session.SessionState
import gg.refx.android.data.repo.AccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val registerMutex = Mutex()

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
        // Mutate token state and decide-to-register entirely inside the mutex so a
        // sign-out racing this rotation can't be overtaken by a stale register, and
        // so `registeredToken` is never nulled outside the lock (unregister reads it).
        scope.launch {
            registerMutex.withLock {
                lastToken = token
                registeredToken = null
                if (session.state.value is SessionState.SignedIn) registerLocked(token)
            }
        }
    }

    private suspend fun registerCurrentToken() {
        val token = lastToken ?: fetchFcmToken() ?: return
        lastToken = token
        register(token)
    }

    private suspend fun register(token: String) {
        registerMutex.withLock { registerLocked(token) }
    }

    /** Caller must hold [registerMutex]. */
    private suspend fun registerLocked(token: String) {
        // Re-check under the lock: if a sign-out already landed, it wins and we bail
        // out instead of binding a live token to a signed-out account.
        if (session.state.value !is SessionState.SignedIn) return
        if (registeredToken == token) return
        runCatching { accountRepoProvider().registerPushToken(token) }
            .onSuccess { registeredToken = token }
    }

    private suspend fun unregister() {
        registerMutex.withLock {
            val token = registeredToken ?: lastToken ?: return
            runCatching { accountRepoProvider().unregisterPushToken(token) }
            registeredToken = null
        }
    }

    private suspend fun fetchFcmToken(): String? = runCatching {
        suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
    }.getOrNull()
}
