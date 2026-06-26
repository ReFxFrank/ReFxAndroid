package gg.refx.android.core.session

import gg.refx.android.core.network.TokenProvider
import gg.refx.android.data.model.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App-wide auth state. */
sealed interface SessionState {
    /** Startup: a token exists but the account hasn't been confirmed yet. */
    data object Resolving : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val account: Account?) : SessionState
}

/**
 * Single source of truth for whether the user is signed in. Bridges the
 * networking layer (which can force a sign-out on refresh failure) and the UI
 * (which observes [state] to choose the auth gate vs. the app shell).
 */
class SessionManager(private val tokens: TokenProvider) {

    private val _state = MutableStateFlow<SessionState>(
        if (tokens.isSignedIn) SessionState.Resolving else SessionState.SignedOut,
    )
    val state: StateFlow<SessionState> = _state.asStateFlow()

    val account: Account? get() = (_state.value as? SessionState.SignedIn)?.account

    fun onSignedIn(account: Account?) {
        _state.value = SessionState.SignedIn(account)
    }

    fun updateAccount(account: Account) {
        _state.value = SessionState.SignedIn(account)
    }

    /** Called on explicit sign-out and on forced sign-out (refresh failure §3.2). */
    fun signOut() {
        tokens.clear()
        _state.value = SessionState.SignedOut
    }
}
