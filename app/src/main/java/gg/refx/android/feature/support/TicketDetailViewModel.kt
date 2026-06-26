package gg.refx.android.feature.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gg.refx.android.core.network.toApiException
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.TicketDetail
import gg.refx.android.data.repo.SupportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TicketDetailUiState(
    val ticket: LoadState<TicketDetail> = LoadState.Loading,
    val reply: String = "",
    val sending: Boolean = false,
    val error: String? = null,
)

class TicketDetailViewModel(
    private val ticketId: String,
    private val repo: SupportRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TicketDetailUiState())
    val state: StateFlow<TicketDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(ticket = if (it.ticket.value == null) LoadState.Loading else it.ticket) }
        viewModelScope.launch {
            runCatching { repo.ticket(ticketId) }
                .onSuccess { detail -> _state.update { it.copy(ticket = LoadState.Loaded(detail)) } }
                .onFailure { t ->
                    _state.update {
                        if (it.ticket.value == null) it.copy(ticket = LoadState.Failed(t.toApiException().message)) else it
                    }
                }
        }
    }

    fun onReplyChange(value: String) = _state.update { it.copy(reply = value, error = null) }

    fun sendReply() {
        val body = _state.value.reply.trim()
        if (body.isEmpty() || _state.value.sending) return
        _state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            runCatching { repo.reply(ticketId, body) }
                .onSuccess {
                    _state.update { it.copy(reply = "", sending = false) }
                    load() // refresh thread to show the new message
                }
                .onFailure { t -> _state.update { it.copy(sending = false, error = t.toApiException().message) } }
        }
    }
}
