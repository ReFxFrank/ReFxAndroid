package gg.refx.android.data.model

import kotlinx.serialization.Serializable

/**
 * Server file models (parity spec §3/§5, `FilesService.swift`). `modifiedAt` is a
 * raw string per the iOS model (server format varies).
 */
@Serializable
data class FileEntry(
    val name: String,
    val path: String,
    val isDir: Boolean = false,
    val size: Long = 0,
    val mode: String? = null,
    val modifiedAt: String? = null,
) {
    val isDirectory: Boolean get() = isDir
}

@Serializable
data class FileContent(
    val content: String = "",
    val encoding: String? = null,
)

/** A short-lived signed URL — opened in the browser (https-only), never streamed in-app. */
@Serializable
data class SignedUrl(
    val url: String,
)

@Serializable
data class WriteFileRequest(val path: String, val content: String)

@Serializable
data class MkdirRequest(val path: String)

@Serializable
data class RenameRequest(val from: String, val to: String)

@Serializable
data class DeletePathsRequest(val paths: List<String>)
