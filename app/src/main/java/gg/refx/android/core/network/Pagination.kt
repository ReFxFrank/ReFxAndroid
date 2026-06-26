package gg.refx.android.core.network

import kotlinx.serialization.Serializable

/**
 * `Page<E> = { items: [E], meta: {...}, hasMore: Bool }` — the list-endpoint shape
 * (§3.2). `meta` is kept loose since servers vary the field set across endpoints.
 */
@Serializable
data class Page<E>(
    val items: List<E> = emptyList(),
    val meta: PageMeta? = null,
    val hasMore: Boolean = false,
)

@Serializable
data class PageMeta(
    val total: Int? = null,
    val page: Int? = null,
    val pageSize: Int? = null,
    val totalPages: Int? = null,
)

/** Convenience result for the `sendPaginated` infinite-scroll path (§3.2). */
data class PaginatedResult<E>(
    val items: List<E>,
    val hasMore: Boolean,
)

fun <E> Page<E>.toPaginatedResult(): PaginatedResult<E> = PaginatedResult(items, hasMore)
