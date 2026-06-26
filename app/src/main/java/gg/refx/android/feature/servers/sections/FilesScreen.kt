package gg.refx.android.feature.servers.sections

import androidx.activity.compose.BackHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.core.network.toApiException
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.design.RefxPrimaryButton
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.core.ui.LoadState
import gg.refx.android.core.ui.WebLink
import gg.refx.android.data.model.FileEntry
import gg.refx.android.data.repo.FilesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FileEditState(val path: String, val content: String, val saving: Boolean = false)

data class FilesUiState(
    val path: String = "/",
    val entries: LoadState<List<FileEntry>> = LoadState.Loading,
    val editing: FileEditState? = null,
    val editorLoading: Boolean = false,
    val error: String? = null,
    val downloadUrl: String? = null,
)

class FilesViewModel(private val serverId: String, private val repo: FilesRepository) : ViewModel() {
    private val _state = MutableStateFlow(FilesUiState())
    val state: StateFlow<FilesUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init { load("/") }

    fun load(path: String) {
        // Single-flight: cancel any in-flight load and guard the write to the
        // still-current path, so out-of-order responses can't show the wrong folder.
        loadJob?.cancel()
        _state.update { it.copy(path = path, entries = LoadState.Loading, error = null) }
        loadJob = viewModelScope.launch {
            runCatching { repo.list(serverId, path) }
                .onSuccess { entries ->
                    val sorted = entries.sortedWith(compareByDescending<FileEntry> { it.isDir }.thenBy { it.name.lowercase() })
                    _state.update { if (it.path == path) it.copy(entries = LoadState.Loaded(sorted)) else it }
                }
                .onFailure { t -> _state.update { if (it.path == path) it.copy(entries = LoadState.Failed(t.toApiException().message)) else it } }
        }
    }

    fun open(entry: FileEntry) {
        if (entry.isDir) {
            load(entry.path)
        } else {
            _state.update { it.copy(editorLoading = true, error = null) }
            viewModelScope.launch {
                runCatching { repo.read(serverId, entry.path) }
                    .onSuccess { fc -> _state.update { it.copy(editorLoading = false, editing = FileEditState(entry.path, fc.content)) } }
                    .onFailure { t -> _state.update { it.copy(editorLoading = false, error = t.toApiException().message) } }
            }
        }
    }

    fun goUp() {
        val current = _state.value.path.trimEnd('/')
        if (current.isEmpty() || current == "") { load("/"); return }
        val parent = current.substringBeforeLast('/', "").ifEmpty { "/" }
        load(parent)
    }

    fun onEditorChange(content: String) = _state.update { it.copy(editing = it.editing?.copy(content = content)) }

    fun saveEditor() {
        val edit = _state.value.editing ?: return
        _state.update { it.copy(editing = edit.copy(saving = true), error = null) }
        viewModelScope.launch {
            runCatching { repo.write(serverId, edit.path, edit.content) }
                .onSuccess { _state.update { it.copy(editing = null) } }
                .onFailure { t -> _state.update { it.copy(editing = edit.copy(saving = false), error = t.toApiException().message) } }
        }
    }

    // Clear the shared error so an editor save failure can't bleed into the list.
    fun closeEditor() = _state.update { it.copy(editing = null, error = null) }

    fun setError(message: String) = _state.update { it.copy(error = message) }

    fun mkdir(name: String) {
        val path = joinPath(_state.value.path, name)
        viewModelScope.launch {
            runCatching { repo.mkdir(serverId, path) }
                .onSuccess { load(_state.value.path) }
                .onFailure { t -> _state.update { it.copy(error = t.toApiException().message) } }
        }
    }

    fun rename(entry: FileEntry, newName: String) {
        if (newName.isBlank()) return
        val dest = joinPath(_state.value.path, newName)
        viewModelScope.launch {
            runCatching { repo.rename(serverId, entry.path, dest) }
                .onSuccess { load(_state.value.path) }
                .onFailure { t -> _state.update { it.copy(error = t.toApiException().message) } }
        }
    }

    fun delete(entry: FileEntry) {
        viewModelScope.launch {
            runCatching { repo.delete(serverId, listOf(entry.path)) }
                .onSuccess { load(_state.value.path) }
                .onFailure { t -> _state.update { it.copy(error = t.toApiException().message) } }
        }
    }

    fun download(entry: FileEntry) {
        viewModelScope.launch {
            runCatching { repo.downloadUrl(serverId, entry.path) }
                .onSuccess { url -> _state.update { it.copy(downloadUrl = url) } }
                .onFailure { t -> _state.update { it.copy(error = t.toApiException().message) } }
        }
    }

    fun downloadOpened() = _state.update { it.copy(downloadUrl = null) }

    private fun joinPath(dir: String, name: String): String =
        (dir.trimEnd('/') + "/" + name.trim('/')).ifEmpty { "/" }
}

@Composable
fun FilesScreen(serverId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val vm: FilesViewModel = viewModel(
        factory = viewModelFactory { initializer { FilesViewModel(serverId, container.filesRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var showMkdir by remember { mutableStateOf(false) }
    var newDir by remember { mutableStateOf("") }
    var actionEntry by remember { mutableStateOf<FileEntry?>(null) }
    var confirmDelete by remember { mutableStateOf<FileEntry?>(null) }
    var renameEntry by remember { mutableStateOf<FileEntry?>(null) }
    var renameText by remember { mutableStateOf("") }
    val notRoot = state.path.trimEnd('/').isNotEmpty()

    LaunchedEffect(state.downloadUrl) {
        state.downloadUrl?.let { url ->
            if (!WebLink.open(context, url)) vm.setError("Couldn't open the download link.")
            vm.downloadOpened()
        }
    }

    // System back closes the inline editor instead of popping the whole section.
    BackHandler(enabled = state.editing != null) { vm.closeEditor() }

    // Editor takes over the screen when a file is open.
    state.editing?.let { edit ->
        FileEditor(
            path = edit.path,
            content = edit.content,
            saving = edit.saving,
            error = state.error,
            onChange = vm::onEditorChange,
            onSave = vm::saveEditor,
            onClose = vm::closeEditor,
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(
            title = "Files",
            onBack = onBack,
            trailing = {
                Row {
                    if (notRoot) TextButton(onClick = vm::goUp) { Text("Up") }
                    TextButton(onClick = { showMkdir = true }) { Text("New folder") }
                }
            },
        )
        Text(
            text = state.path,
            color = DesignTokens.AppMuted,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        state.error?.let { Text(it, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp)) }

        AsyncState(
            state = state.entries,
            isEmpty = { it.isEmpty() },
            emptyTitle = "Empty folder",
            emptyMessage = "No files here yet.",
            onRetry = { vm.load(state.path) },
        ) { entries ->
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                if (notRoot) {
                    item {
                        FileRow(name = "..", isDir = true, sub = "Up a level", onClick = vm::goUp, onLong = {})
                    }
                }
                items(entries, key = { it.path }) { entry ->
                    FileRow(
                        name = entry.name,
                        isDir = entry.isDir,
                        sub = if (entry.isDir) "Folder" else "${entry.size} B",
                        onClick = { vm.open(entry) },
                        onLong = { actionEntry = entry },
                        onDownload = if (!entry.isDir) ({ vm.download(entry) }) else null,
                    )
                }
            }
        }
    }

    // Long-press action chooser: Rename / Delete.
    actionEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { actionEntry = null },
            title = { Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    TextButton(onClick = { renameEntry = entry; renameText = entry.name; actionEntry = null }) { Text("Rename") }
                    TextButton(onClick = { confirmDelete = entry; actionEntry = null }) { Text("Delete", color = DesignTokens.AppDestructive) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { actionEntry = null }) { Text("Cancel") } },
        )
    }

    renameEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { renameEntry = null },
            title = { Text("Rename") },
            text = { OutlinedTextField(renameText, { renameText = it }, label = { Text("New name") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { vm.rename(entry, renameText); renameEntry = null }) { Text("Rename") } },
            dismissButton = { TextButton(onClick = { renameEntry = null }) { Text("Cancel") } },
        )
    }

    if (showMkdir) {
        AlertDialog(
            onDismissRequest = { showMkdir = false },
            title = { Text("New folder") },
            text = { OutlinedTextField(newDir, { newDir = it }, label = { Text("Folder name") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { if (newDir.isNotBlank()) { vm.mkdir(newDir); newDir = ""; showMkdir = false } }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { showMkdir = false }) { Text("Cancel") } },
        )
    }
    confirmDelete?.let { entry ->
        ConfirmDialog("Delete ${entry.name}?", "This can't be undone.", "Delete", { vm.delete(entry); confirmDelete = null }, { confirmDelete = null })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(
    name: String,
    isDir: Boolean,
    sub: String,
    onClick: () -> Unit,
    onLong: () -> Unit,
    onDownload: (() -> Unit)? = null,
) {
    Row(
        // Long-press opens the Rename/Delete chooser; tap opens/navigates.
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLong).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (isDir) Icons.Outlined.Folder else Icons.AutoMirrored.Outlined.InsertDriveFile,
            contentDescription = null,
            tint = if (isDir) DesignTokens.AppAccentText else DesignTokens.AppMuted,
        )
        Column(Modifier.weight(1f)) {
            Text(name, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
        }
        if (onDownload != null) {
            TextButton(onClick = onDownload) { Text("Download") }
        }
    }
}

@Composable
private fun FileEditor(
    path: String,
    content: String,
    saving: Boolean,
    error: String?,
    onChange: (String) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = path.substringAfterLast('/'), onBack = onClose, trailing = {
            TextButton(onClick = onSave, enabled = !saving) { Text("Save") }
        })
        error?.let { Text(it, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp)) }
        Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
            OutlinedTextField(
                value = content,
                onValueChange = onChange,
                modifier = Modifier.fillMaxSize(),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
        }
        RefxPrimaryButton("Save", onSave, loading = saving, fullWidth = true, modifier = Modifier.fillMaxWidth().padding(16.dp))
    }
}
