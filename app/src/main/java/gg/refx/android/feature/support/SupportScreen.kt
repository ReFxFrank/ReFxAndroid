package gg.refx.android.feature.support

import androidx.compose.runtime.Composable
import gg.refx.android.core.ui.ComingSoon
import gg.refx.android.core.ui.ScreenScaffold

/** Support — ticket list, create, thread + reply (§5, Milestone 3). */
@Composable
fun SupportScreen() {
    ScreenScaffold(eyebrow = "Help", title = "Support") {
        ComingSoon("Tickets arrive in Milestone 3.")
    }
}
