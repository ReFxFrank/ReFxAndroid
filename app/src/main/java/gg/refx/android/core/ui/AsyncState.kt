package gg.refx.android.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.design.RefxSecondaryButton
import gg.refx.android.core.design.SkeletonBlock

/**
 * Wraps nearly every screen — renders the right thing for each [LoadState]:
 * a skeleton while loading, an error card with retry on failure, an empty
 * placeholder when [isEmpty] is true, otherwise the loaded [content].
 *
 * Equivalent to the iOS `AsyncStateView`.
 */
@Composable
fun <T> AsyncState(
    state: LoadState<T>,
    modifier: Modifier = Modifier,
    isEmpty: (T) -> Boolean = { false },
    emptyTitle: String = "Nothing here yet",
    emptyMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    skeleton: @Composable () -> Unit = { DefaultSkeleton() },
    content: @Composable (T) -> Unit,
) {
    when (state) {
        LoadState.Idle, LoadState.Loading -> skeleton()
        is LoadState.Failed -> ErrorState(
            modifier = modifier,
            message = state.message,
            onRetry = onRetry,
        )
        is LoadState.Loaded -> {
            if (isEmpty(state.data)) {
                EmptyState(modifier = modifier, title = emptyTitle, message = emptyMessage)
            } else {
                content(state.data)
            }
        }
    }
}

@Composable
private fun DefaultSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(5) {
            SkeletonBlock(height = 64.dp, cornerRadius = 16.dp)
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().wrapContentHeight().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Something went wrong",
            color = DesignTokens.AppForegroundStrong,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            color = DesignTokens.AppMuted,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            RefxSecondaryButton(text = "Retry", onClick = onRetry)
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().wrapContentHeight().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Inbox,
            contentDescription = null,
            tint = DesignTokens.AppMuted,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = title,
            color = DesignTokens.AppForegroundStrong,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            Text(
                text = message,
                color = DesignTokens.AppMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
