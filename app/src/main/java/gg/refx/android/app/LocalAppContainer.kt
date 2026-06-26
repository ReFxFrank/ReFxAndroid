package gg.refx.android.app

import androidx.compose.runtime.compositionLocalOf

/** Provides the [AppContainer] down the Compose tree for manual DI in screens. */
val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
