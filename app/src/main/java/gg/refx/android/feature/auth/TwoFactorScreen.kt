package gg.refx.android.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.design.Eyebrow
import gg.refx.android.core.design.GlassCard
import gg.refx.android.core.design.RefxPrimaryButton
import gg.refx.android.core.design.RefxSecondaryButton

@Composable
fun TwoFactorScreen(
    state: AuthUiState,
    onTotpChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Eyebrow("Two-factor")
        Text(
            text = "Enter your code",
            style = MaterialTheme.typography.headlineMedium,
            color = DesignTokens.AppForegroundStrong,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
        Text(
            text = "Open your authenticator app and enter the 6-digit code.",
            style = MaterialTheme.typography.bodyMedium,
            color = DesignTokens.AppMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.totp,
                    onValueChange = onTotpChange,
                    label = { Text("Authentication code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                )

                if (state.error != null) {
                    Text(
                        text = state.error,
                        color = DesignTokens.AppDestructive,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                RefxPrimaryButton(
                    text = "Verify",
                    onClick = onSubmit,
                    enabled = state.canSubmitTotp,
                    loading = state.loading,
                    fullWidth = true,
                )
                RefxSecondaryButton(
                    text = "Back",
                    onClick = onBack,
                    enabled = !state.loading,
                    fullWidth = true,
                )
            }
        }
    }
}
