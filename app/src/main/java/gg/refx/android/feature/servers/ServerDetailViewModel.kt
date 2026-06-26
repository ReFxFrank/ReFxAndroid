package gg.refx.android.feature.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gg.refx.android.core.network.toApiException
import gg.refx.android.core.realtime.ConsoleSocket
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.LiveStats
import gg.refx.android.data.model.PowerSignal
import gg.refx.android.data.model.Server
import gg.refx.android.data.model.ServerState
import gg.refx.android.data.repo.ServersRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServerDetailUiState(
    val server: LoadState<Server> = LoadState.Loading,
    val stats: LiveStats? = null,
    val liveState: ServerState? = null,
    val optimisticState: ServerState? = null,
    val powerBusy: Boolean = false,
    val pendingConfirm: PowerSignal? = null,
    val errorMessage: String? = null,
) {
    /** Effective state: optimistic → live (socket) → loaded server state. */
    val effectiveState: ServerState
        get() = optimisticState ?: liveState ?: server.value?.state ?: ServerState.UNKNOWN
}

/**
 * Server detail (parity spec §8): load server → stats → console connect. Power
 * actions are optimistic with a 0.8s debounce and confirm dialogs for destructive
 * signals; the effective state prefers the live socket state.
 */
class ServerDetailViewModel(
    private val serverId: String,
    private val repo: ServersRepository,
    val console: ConsoleSocket,
) : ViewModel() {

    private val _state = MutableStateFlow(ServerDetailUiState())
    val state: StateFlow<ServerDetailUiState> = _state.asStateFlow()

    init {
        load()
        observeConsole()
    }

    fun load() {
        _state.update { it.copy(server = if (it.server.value == null) LoadState.Loading else it.server) }
        viewModelScope.launch {
            runCatching { repo.get(serverId) }
                .onSuccess { server ->
                    _state.update { it.copy(server = LoadState.Loaded(server)) }
                    console.connect(serverId)
                    fetchStats()
                }
                .onFailure { t ->
                    _state.update {
                        if (it.server.value == null) {
                            it.copy(server = LoadState.Failed(t.toApiException().message))
                        } else {
                            it
                        }
                    }
                }
        }
    }

    private fun fetchStats() {
        viewModelScope.launch {
            runCatching { repo.stats(serverId) }
                .onSuccess { stats -> _state.update { it.copy(stats = stats) } }
        }
    }

    private fun observeConsole() {
        viewModelScope.launch {
            console.liveState.collect { live ->
                if (live != null) {
                    // A real state arrived → drop the optimistic guess.
                    _state.update { it.copy(liveState = live, optimisticState = null) }
                }
            }
        }
        viewModelScope.launch {
            console.stats.collect { frame ->
                if (frame != null) {
                    _state.update { s ->
                        val prev = s.stats
                        s.copy(
                            stats = LiveStats(
                                state = frame.state ?: prev?.state,
                                cpuPct = frame.cpuPct,
                                memUsedMb = frame.memUsedMb,
                                memTotalMb = prev?.memTotalMb,
                                diskUsedMb = frame.diskUsedMb,
                                netRxBytes = frame.netRxBytes,
                                netTxBytes = frame.netTxBytes,
                                players = frame.players ?: prev?.players,
                                uptimeMs = prev?.uptimeMs,
                            ),
                        )
                    }
                }
            }
        }
    }

    fun requestPower(signal: PowerSignal) {
        if (signal == PowerSignal.START) {
            performPower(signal)
        } else {
            _state.update { it.copy(pendingConfirm = signal) }
        }
    }

    fun confirmPower() {
        val signal = _state.value.pendingConfirm ?: return
        _state.update { it.copy(pendingConfirm = null) }
        performPower(signal)
    }

    fun cancelConfirm() = _state.update { it.copy(pendingConfirm = null) }

    private fun performPower(signal: PowerSignal) {
        val optimistic = when (signal) {
            PowerSignal.START, PowerSignal.RESTART -> ServerState.STARTING
            PowerSignal.STOP, PowerSignal.KILL -> ServerState.STOPPING
        }
        _state.update { it.copy(powerBusy = true, optimisticState = optimistic, errorMessage = null) }
        viewModelScope.launch {
            val result = runCatching { repo.power(serverId, signal) }
            result.onFailure { t ->
                _state.update { it.copy(optimisticState = null, errorMessage = t.toApiException().message) }
            }
            // 0.8s debounce before re-enabling the controls.
            delay(POWER_DEBOUNCE_MS)
            _state.update { it.copy(powerBusy = false) }
        }
    }

    fun sendCommand(command: String) = console.sendCommand(command)

    fun dismissError() = _state.update { it.copy(errorMessage = null) }

    override fun onCleared() {
        console.dispose()
    }

    private companion object {
        const val POWER_DEBOUNCE_MS = 800L
    }
}
