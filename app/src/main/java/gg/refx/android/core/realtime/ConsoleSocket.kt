package gg.refx.android.core.realtime

import gg.refx.android.core.network.ApiConfig
import gg.refx.android.core.network.RefxJson
import gg.refx.android.core.network.TokenProvider
import gg.refx.android.core.network.TokenRefresher
import gg.refx.android.data.model.ServerState
import gg.refx.android.data.model.StatsFrame
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URI

/** A single console output line. `isError` is true for stderr. */
data class ConsoleLine(val text: String, val stream: String) {
    val isError: Boolean get() = stream.equals("stderr", ignoreCase = true)
}

/** Console connection lifecycle (mirrors iOS `ConsoleSocket.ConnectionState`). */
sealed interface ConsoleConnectionState {
    data object Idle : ConsoleConnectionState
    data object Connecting : ConsoleConnectionState
    data object Connected : ConsoleConnectionState
    data object Reconnecting : ConsoleConnectionState
    /** The user lacks console access to this server. */
    data object Forbidden : ConsoleConnectionState
    data class Failed(val reason: String) : ConsoleConnectionState
}

/**
 * Live server console over **Socket.IO** (namespace `/ws/console`), mirroring the
 * iOS `ConsoleSocket` (parity spec §6).
 *
 * - Bearer is passed **two ways**: the `Authorization` handshake header AND the
 *   CONNECT auth payload `{ token }`.
 * - Manager reconnects infinitely with 2s→15s backoff.
 * - Emits `subscribe { serverId }` on connect and `command { command }`.
 * - Listens for `console`, `stats`, `power`, `error`, `subscribed`.
 * - On `unauthorized` it refreshes the token **once** and reconnects; `forbidden`
 *   ends in [ConsoleConnectionState.Forbidden].
 * - Output buffer is capped at [MAX_LINES] (FIFO).
 */
class ConsoleSocket(
    private val configProvider: () -> ApiConfig,
    private val tokens: TokenProvider,
    private val refresher: TokenRefresher,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow<ConsoleConnectionState>(ConsoleConnectionState.Idle)
    val connectionState: StateFlow<ConsoleConnectionState> = _connectionState.asStateFlow()

    private val _lines = MutableStateFlow<List<ConsoleLine>>(emptyList())
    val lines: StateFlow<List<ConsoleLine>> = _lines.asStateFlow()

    private val _liveState = MutableStateFlow<ServerState?>(null)
    val liveState: StateFlow<ServerState?> = _liveState.asStateFlow()

    private val _stats = MutableStateFlow<StatsFrame?>(null)
    val stats: StateFlow<StatsFrame?> = _stats.asStateFlow()

    private var socket: Socket? = null
    private var serverId: String? = null

    @Volatile
    private var didRefreshForAuth = false

    fun connect(serverId: String) {
        this.serverId = serverId
        scope.launch { openSocket(serverId) }
    }

    private fun openSocket(serverId: String) {
        val token = tokens.accessToken()
        if (token == null) {
            _connectionState.value = ConsoleConnectionState.Failed("Not signed in")
            return
        }
        _connectionState.value = ConsoleConnectionState.Connecting

        val config = configProvider()
        val opts = IO.Options().apply {
            reconnection = true
            reconnectionDelay = 2_000
            reconnectionDelayMax = 15_000
            reconnectionAttempts = Int.MAX_VALUE
            transports = arrayOf("websocket", "polling")
            forceNew = true
            auth = mapOf("token" to token)
            extraHeaders = mapOf("Authorization" to listOf("Bearer $token"))
        }

        val uri = URI.create(config.socketOrigin + config.consoleNamespace)
        val s = IO.socket(uri, opts)
        socket = s

        s.on(Socket.EVENT_CONNECT) {
            didRefreshForAuth = false
            emitSubscribe(serverId)
            _connectionState.value = ConsoleConnectionState.Connected
        }
        s.on(Socket.EVENT_DISCONNECT) {
            if (_connectionState.value != ConsoleConnectionState.Forbidden) {
                _connectionState.value = ConsoleConnectionState.Reconnecting
            }
        }
        s.on(Socket.EVENT_CONNECT_ERROR) { args -> handleTransportError(args) }
        s.on("subscribed") { _connectionState.value = ConsoleConnectionState.Connected }
        s.on("console") { args -> onConsole(args) }
        s.on("stats") { args -> onStats(args) }
        s.on("power") { args -> onPower(args) }
        s.on("error") { args -> onServerError(args) }

        s.connect()
    }

    private fun emitSubscribe(serverId: String) {
        runCatching { socket?.emit("subscribe", JSONObject().put("serverId", serverId)) }
    }

    fun sendCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return
        append(listOf(ConsoleLine("> $trimmed", "input")))
        runCatching { socket?.emit("command", JSONObject().put("command", trimmed)) }
    }

    private fun onConsole(args: Array<out Any?>) {
        val obj = args.firstOrNull() as? JSONObject ?: return
        val raw = obj.optString("line")
        val stream = obj.optString("stream", "stdout").ifBlank { "stdout" }
        val newLines = raw.split("\n").map { ConsoleLine(it, stream) }
        append(newLines)
    }

    private fun onStats(args: Array<out Any?>) {
        val obj = args.firstOrNull() as? JSONObject ?: return
        val frame = runCatching {
            RefxJson.decodeFromString(StatsFrame.serializer(), obj.toString())
        }.getOrNull() ?: return
        _stats.value = frame
        frame.state?.let { _liveState.value = it }
    }

    private fun onPower(args: Array<out Any?>) {
        val obj = args.firstOrNull() as? JSONObject ?: return
        val raw = obj.optString("state")
        if (raw.isNotBlank()) {
            _liveState.value = runCatching {
                RefxJson.decodeFromString(ServerState.serializer(), "\"$raw\"")
            }.getOrDefault(ServerState.UNKNOWN)
        }
    }

    private fun onServerError(args: Array<out Any?>) {
        val message = (args.firstOrNull() as? JSONObject)?.optString("message").orEmpty().lowercase()
        when {
            message.contains("unauthorized") -> refreshAndReconnect()
            message.contains("forbidden") -> {
                _connectionState.value = ConsoleConnectionState.Forbidden
                append(listOf(ConsoleLine("You don't have console access to this server.", "stderr")))
            }
            else -> append(listOf(ConsoleLine(message.ifBlank { "Console error" }, "stderr")))
        }
    }

    private fun handleTransportError(args: Array<out Any?>) {
        val message = args.firstOrNull()?.toString().orEmpty().lowercase()
        if (message.contains("unauthorized") || message.contains("401")) {
            refreshAndReconnect()
        } else if (_connectionState.value != ConsoleConnectionState.Connected) {
            // The manager keeps retrying; reflect the reconnecting state.
            _connectionState.value = ConsoleConnectionState.Reconnecting
        }
    }

    /** Refresh the token once and reconnect; give up (sign-out-ish) if it fails again. */
    private fun refreshAndReconnect() {
        val id = serverId ?: return
        if (didRefreshForAuth) {
            _connectionState.value = ConsoleConnectionState.Failed("Session expired")
            return
        }
        didRefreshForAuth = true
        scope.launch {
            val refreshToken = tokens.refreshToken()
            val refreshed = refreshToken?.let { refresher.refresh(it) }
            if (refreshed == null) {
                _connectionState.value = ConsoleConnectionState.Failed("Session expired")
                return@launch
            }
            tokens.save(refreshed)
            teardownSocket()
            openSocket(id)
        }
    }

    private fun append(newLines: List<ConsoleLine>) {
        _lines.update { old ->
            val combined = old + newLines
            if (combined.size > MAX_LINES) combined.takeLast(MAX_LINES) else combined
        }
    }

    private fun teardownSocket() {
        socket?.let { s ->
            s.off()
            s.disconnect()
            s.close()
        }
        socket = null
    }

    fun disconnect() {
        teardownSocket()
        _connectionState.value = ConsoleConnectionState.Idle
    }

    /** Release everything; call from the owner's onCleared. */
    fun dispose() {
        teardownSocket()
        scope.cancel()
    }

    fun clearLines() {
        _lines.value = emptyList()
    }

    companion object {
        const val MAX_LINES = 2000
    }
}
