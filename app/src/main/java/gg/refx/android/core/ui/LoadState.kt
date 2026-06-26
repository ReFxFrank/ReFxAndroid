package gg.refx.android.core.ui

/**
 * Mirrors the iOS `LoadState<T>`: the lifecycle of an async-loaded value.
 *
 * `Idle` — not started; `Loading` — in flight; `Loaded` — has a value;
 * `Failed` — carries a user-facing error message.
 */
sealed interface LoadState<out T> {
    data object Idle : LoadState<Nothing>
    data object Loading : LoadState<Nothing>
    data class Loaded<T>(val data: T) : LoadState<T>
    data class Failed(val message: String) : LoadState<Nothing>

    /** The loaded value, or null in any non-loaded state. Mirrors iOS `.value`. */
    val value: T?
        get() = (this as? Loaded)?.data

    val isLoading: Boolean get() = this is Loading
}

/** Map the loaded value while preserving the surrounding state. */
inline fun <T, R> LoadState<T>.map(transform: (T) -> R): LoadState<R> = when (this) {
    is LoadState.Loaded -> LoadState.Loaded(transform(data))
    is LoadState.Failed -> this
    LoadState.Idle -> LoadState.Idle
    LoadState.Loading -> LoadState.Loading
}
