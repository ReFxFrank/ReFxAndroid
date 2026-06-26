package gg.refx.android.core.network

import kotlinx.serialization.Serializable

/**
 * List-endpoint shape (parity spec §3/§5). The **wire** form is
 * `{ data: [E], meta: { page, pageSize, total, totalPages } }` — there is **no**
 * `hasMore` or `items` field on the wire. `items` is an alias for `data` and
 * `hasMore` is computed `page < totalPages` (mirrors the iOS `Page`/`PaginatedEnvelope`).
 */
@Serializable
data class Page<E>(
    val data: List<E> = emptyList(),
    val meta: PageMeta = PageMeta(),
) {
    val items: List<E> get() = data

    /** Computed client-side: more pages remain. */
    val hasMore: Boolean get() = meta.page < meta.totalPages
}

@Serializable
data class PageMeta(
    val page: Int = 1,
    val pageSize: Int = 0,
    val total: Int = 0,
    val totalPages: Int = 0,
)

/** Convenience result for the `sendPaginated` infinite-scroll path. */
data class PaginatedResult<E>(
    val items: List<E>,
    val hasMore: Boolean,
)

fun <E> Page<E>.toPaginatedResult(): PaginatedResult<E> = PaginatedResult(items, hasMore)
