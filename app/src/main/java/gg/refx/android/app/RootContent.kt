package gg.refx.android.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gg.refx.android.app.shell.AppShell
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.session.SessionState
import gg.refx.android.feature.auth.AuthFlow

/**
 * The auth gate. Chooses between the login flow and the authenticated shell based
 * on [SessionState]. On a cold start with a stored token ([SessionState.Resolving])
 * it confirms the session by loading the account, signing out on failure.
 */
@Composable
fun RootContent() {
    val container = LocalAppContainer.current
    val state by container.session.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is SessionState.Resolving) {
            runCatching { container.authRepository.loadAccount() }
                .onFailure { container.session.signOut() }
        }
    }

    when (val s = state) {
        SessionState.Resolving -> LoadingScreen()
        SessionState.SignedOut -> AuthFlow()
        is SessionState.SignedIn -> AppShell(account = s.account)
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = DesignTokens.AppPrimary)
    }
}
