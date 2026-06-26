package gg.refx.android.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gg.refx.android.app.LocalAppContainer

/**
 * The unauthenticated flow: email/password login, escalating to a TOTP prompt when
 * the server requires 2FA. On success the [gg.refx.android.core.session.SessionManager]
 * flips to SignedIn and the root swaps to the app shell.
 */
@Composable
fun AuthFlow() {
    val container = LocalAppContainer.current
    val vm: AuthViewModel = viewModel(
        factory = viewModelFactory { initializer { AuthViewModel(container.authRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    when (state.stage) {
        AuthStage.Login -> LoginScreen(
            state = state,
            onEmailChange = vm::onEmailChange,
            onPasswordChange = vm::onPasswordChange,
            onSubmit = vm::login,
        )
        AuthStage.TwoFactor -> TwoFactorScreen(
            state = state,
            onTotpChange = vm::onTotpChange,
            onSubmit = vm::verifyTotp,
            onBack = vm::backToLogin,
        )
    }
}
