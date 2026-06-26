package gg.refx.android.feature.servers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.design.GlassCard
import gg.refx.android.core.design.ManageRow
import gg.refx.android.core.design.RefxControlShape
import gg.refx.android.core.design.ResourceGauge
import gg.refx.android.core.design.SectionHeader
import gg.refx.android.core.design.StatePill
import gg.refx.android.core.realtime.ConsoleConnectionState
import gg.refx.android.core.realtime.ConsoleLine
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.data.model.LiveStats
import gg.refx.android.data.model.PowerSignal
import gg.refx.android.data.model.Server
import gg.refx.android.data.model.ServerState
import gg.refx.android.data.model.StateColors

@Composable
fun ServerDetailScreen(
    serverId: String,
    onBack: () -> Unit,
    onOpenSection: (ServerSection, String) -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: ServerDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ServerDetailViewModel(
                    serverId = serverId,
                    repo = container.serversRepository,
                    console = container.createConsoleSocket(),
                )
            }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val lines by vm.console.lines.collectAsStateWithLifecycle()
    val connection by vm.console.connectionState.collectAsStateWithLifecycle()
    val appendCount by vm.console.appendCount.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = state.server.value?.name ?: "Server", onBack = onBack)

        AsyncState(state = state.server, onRetry = vm::load) { server ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ServerHeader(server = server, effectiveState = state.effectiveState)
                if (state.effectiveState == ServerState.PENDING_PAYMENT) {
                    PayNowBanner(shortId = server.shortId)
                }
                GaugesCard(server = server, stats = state.stats)
                PowerControls(
                    busy = state.powerBusy,
                    onSignal = vm::requestPower,
                )
                state.errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = DesignTokens.AppDestructive,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().clickable { vm.dismissError() },
                    )
                }
                ConsoleCard(
                    connection = connection,
                    lines = lines,
                    scrollKey = appendCount,
                    onSend = vm::sendCommand,
                )
                SectionsCard(server = server, onOpen = onOpenSection)
            }
        }
    }

    // Destructive power confirm dialog.
    state.pendingConfirm?.let { signal ->
        AlertDialog(
            onDismissRequest = vm::cancelConfirm,
            title = { Text("${signal.raw.replaceFirstChar { it.uppercase() }} server?") },
            text = { Text("Are you sure you want to ${signal.raw} this server?") },
            confirmButton = { TextButton(onClick = vm::confirmPower) { Text(signal.raw.replaceFirstChar { it.uppercase() }) } },
            dismissButton = { TextButton(onClick = vm::cancelConfirm) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DetailTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DesignTokens.AppForeground)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = DesignTokens.AppForegroundStrong,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ServerHeader(server: Server, effectiveState: ServerState) {
    val (label, color) = StateColors.server(effectiveState)
    val clipboard = LocalClipboardManager.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = DesignTokens.AppForegroundStrong,
                    modifier = Modifier.weight(1f),
                )
                StatePill(label = label, color = color)
            }
            Text(text = server.gameName, color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
            server.connectionString?.let { conn ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { clipboard.setText(AnnotatedString(conn)) },
                ) {
                    Text(
                        text = conn,
                        color = DesignTokens.AppAccentText,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Copy address",
                        tint = DesignTokens.AppMuted,
                        modifier = Modifier.padding(start = 6.dp).height(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun GaugesCard(server: Server, stats: LiveStats?) {
    val cpuFraction = ((stats?.cpuPct ?: 0.0) / 100.0).toFloat()
    val memTotal = stats?.memTotalMb ?: server.memoryMb?.toDouble()
    val memFraction = if (memTotal != null && memTotal > 0) ((stats?.memUsedMb ?: 0.0) / memTotal).toFloat() else 0f
    val diskTotal = server.diskMb?.toDouble()
    val diskFraction = if (diskTotal != null && diskTotal > 0) ((stats?.diskUsedMb ?: 0.0) / diskTotal).toFloat() else 0f

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ResourceGauge(fraction = cpuFraction, label = "CPU", value = "${(cpuFraction * 100).toInt()}%")
            ResourceGauge(fraction = memFraction, label = "RAM", value = "${(memFraction * 100).toInt()}%")
            ResourceGauge(fraction = diskFraction, label = "Disk", value = "${(diskFraction * 100).toInt()}%")
        }
    }
}

@Composable
private fun PowerControls(busy: Boolean, onSignal: (PowerSignal) -> Unit) {
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(title = "Power")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PowerButton("Start", DesignTokens.AppSuccess, !busy, Modifier.weight(1f)) {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onSignal(PowerSignal.START)
                }
                PowerButton("Restart", DesignTokens.AppWarning, !busy, Modifier.weight(1f)) {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onSignal(PowerSignal.RESTART)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PowerButton("Stop", DesignTokens.AppMuted, !busy, Modifier.weight(1f)) {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onSignal(PowerSignal.STOP)
                }
                PowerButton("Kill", DesignTokens.AppDestructive, !busy, Modifier.weight(1f)) {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onSignal(PowerSignal.KILL)
                }
            }
        }
    }
}

@Composable
private fun PowerButton(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(color.copy(alpha = if (enabled) 0.16f else 0.06f), RefxControlShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = if (enabled) color else DesignTokens.AppMuted, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun PayNowBanner(shortId: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val webOrigin = LocalAppContainer.current.config.webOrigin
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { gg.refx.android.core.ui.WebLink.open(context, "$webOrigin/servers/$shortId/billing") },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Payment required",
                    style = MaterialTheme.typography.titleMedium,
                    color = DesignTokens.AppWarning,
                )
                Text(
                    text = "This server is awaiting payment. Tap to pay on the web.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignTokens.AppMuted,
                )
            }
            Text("Pay now", color = DesignTokens.AppAccentText, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ConsoleCard(
    connection: ConsoleConnectionState,
    lines: List<ConsoleLine>,
    scrollKey: Long,
    onSend: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    // Key on the append counter, not lines.size (which saturates at MAX_LINES).
    LaunchedEffect(scrollKey) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(title = "Console", trailing = { ConnectionDot(connection) })

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(DesignTokens.AppPopover, RefxControlShape)
                    .padding(8.dp),
            ) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(lines) { line ->
                        Text(
                            text = line.text,
                            color = when {
                                line.isError -> DesignTokens.AppDestructive
                                line.stream == "input" -> DesignTokens.AppAccentText
                                else -> DesignTokens.AppForeground
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Type a command") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (input.isNotBlank()) { onSend(input); input = "" }
                    }),
                )
                IconButton(onClick = { if (input.isNotBlank()) { onSend(input); input = "" } }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = DesignTokens.AppPrimary)
                }
            }
        }
    }
}

@Composable
private fun ConnectionDot(connection: ConsoleConnectionState) {
    val (label, color) = when (connection) {
        ConsoleConnectionState.Connected -> "Connected" to DesignTokens.AppSuccess
        ConsoleConnectionState.Connecting -> "Connecting" to DesignTokens.AppWarning
        ConsoleConnectionState.Reconnecting -> "Reconnecting" to DesignTokens.AppWarning
        // Spec §2 console state→token map: every non-connected/connecting state → appMuted.
        ConsoleConnectionState.Forbidden -> "No access" to DesignTokens.AppMuted
        is ConsoleConnectionState.Failed -> "Disconnected" to DesignTokens.AppMuted
        ConsoleConnectionState.Idle -> "Idle" to DesignTokens.AppMuted
    }
    StatePill(label = label, color = color)
}

@Composable
private fun SectionsCard(server: Server, onOpen: (ServerSection, String) -> Unit) {
    val purchasingEnabled = LocalAppContainer.current.purchasingEnabled
    val sections = remember(server.id, purchasingEnabled) {
        ServerSection.applicableFor(server, purchasingEnabled).filter { it != ServerSection.CONSOLE }
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            SectionHeader(title = "Manage")
            sections.forEach { section ->
                ManageRow(
                    title = section.label,
                    leadingIcon = section.icon,
                    subtitle = if (section.isNative) null else "Opens on the web",
                    modifier = Modifier.clickable { onOpen(section, server.shortId) },
                )
            }
        }
    }
}
