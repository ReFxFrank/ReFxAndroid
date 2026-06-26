package gg.refx.android.feature.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gg.refx.android.core.network.toApiException
import gg.refx.android.data.model.TicketPriority
import gg.refx.android.data.repo.SupportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateTicketUiState(
    val subject: String = "",
    val body: String = "",
    val priority: TicketPriority = TicketPriority.NORMAL,
    val submitting: Boolean = false,
    val error: String? = null,
    val createdTicketId: String? = null,
) {
    val canSubmit: Boolean get() = subject.isNotBlank() && body.isNotBlank() && !submitting
}

class CreateTicketViewModel(private val repo: SupportRepository) : ViewModel() {

    private val _state = MutableStateFlow(CreateTicketUiState())
    val state: StateFlow<CreateTicketUiState> = _state.asStateFlow()

    fun onSubjectChange(v: String) = _state.update { it.copy(subject = v, error = null) }
    fun onBodyChange(v: String) = _state.update { it.copy(body = v, error = null) }
    fun onPriorityChange(p: TicketPriority) = _state.update { it.copy(priority = p) }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            runCatching { repo.create(s.subject, s.body, s.priority.raw) }
                .onSuccess { ticket -> _state.update { it.copy(submitting = false, createdTicketId = ticket.id) } }
                .onFailure { t -> _state.update { it.copy(submitting = false, error = t.toApiException().message) } }
        }
    }
}
