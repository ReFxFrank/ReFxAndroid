package gg.refx.android.feature.support

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.design.RefxPrimaryButton
import gg.refx.android.core.design.StatusChip
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.data.model.TicketPriority

@Composable
fun CreateTicketScreen(onBack: () -> Unit, onCreated: (String) -> Unit) {
    val container = LocalAppContainer.current
    val vm: CreateTicketViewModel = viewModel(
        factory = viewModelFactory { initializer { CreateTicketViewModel(container.supportRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.createdTicketId) {
        state.createdTicketId?.let(onCreated)
    }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "New ticket", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.subject,
                onValueChange = vm::onSubjectChange,
                label = { Text("Subject") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.body,
                onValueChange = vm::onBodyChange,
                label = { Text("How can we help?") },
                modifier = Modifier.fillMaxWidth().height(160.dp),
            )

            Text("Priority", style = MaterialTheme.typography.labelLarge, color = DesignTokens.AppMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(TicketPriority.LOW, TicketPriority.NORMAL, TicketPriority.HIGH, TicketPriority.URGENT).forEach { p ->
                    val selected = state.priority == p
                    val color = if (selected) DesignTokens.AppPrimary else DesignTokens.AppMuted
                    Box(modifier = Modifier.clickable { vm.onPriorityChange(p) }) {
                        StatusChip(text = p.name.lowercase().replaceFirstChar { it.uppercase() }, color = color)
                    }
                }
            }

            if (state.error != null) {
                Text(state.error!!, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall)
            }

            RefxPrimaryButton(
                text = "Submit ticket",
                onClick = vm::submit,
                enabled = state.canSubmit,
                loading = state.submitting,
                fullWidth = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
