package gg.refx.android.feature.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.design.GlassCard
import gg.refx.android.core.design.RoleBadge
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.data.model.TicketMessage

@Composable
fun TicketDetailScreen(ticketId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: TicketDetailViewModel = viewModel(
        factory = viewModelFactory { initializer { TicketDetailViewModel(ticketId, container.supportRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = state.ticket.value?.subject ?: "Ticket", onBack = onBack)

        AsyncState(state = state.ticket, onRetry = vm::load, modifier = Modifier.weight(1f)) { detail ->
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    detail.messages.forEach { msg -> MessageBubble(msg) }
                    if (detail.messages.isEmpty()) {
                        Text("No messages yet.", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                ReplyBar(
                    value = state.reply,
                    sending = state.sending,
                    error = state.error,
                    onChange = vm::onReplyChange,
                    onSend = vm::sendReply,
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: TicketMessage) {
    val author = msg.author
    val isStaff = author?.isStaff == true
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = author?.displayName ?: "You",
                    style = MaterialTheme.typography.labelLarge,
                    color = DesignTokens.AppForegroundStrong,
                )
                if (isStaff) RoleBadge("Staff")
            }
            Text(text = msg.body, style = MaterialTheme.typography.bodyMedium, color = DesignTokens.AppForeground)
        }
    }
}

@Composable
private fun ReplyBar(
    value: String,
    sending: Boolean,
    error: String?,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        if (error != null) {
            Text(error, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Write a reply") },
                enabled = !sending,
            )
            IconButton(onClick = onSend, enabled = !sending && value.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send reply", tint = DesignTokens.AppPrimary)
            }
        }
    }
}
