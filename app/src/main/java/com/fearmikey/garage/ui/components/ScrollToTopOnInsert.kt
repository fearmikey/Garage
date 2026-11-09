package com.fearmikey.garage.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Keyed lazy lists keep the current first item anchored when new items are inserted above it.
 * Tab ViewModels cache their last list, so on returning to a tab the stale list renders first and
 * the fresh list then inserts the new entry *above* the viewport, hiding it.
 *
 * Call this with the key of the list's first data item: when that key changes while the user is
 * at (or within a few rows of) the top, the list snaps back to the top so new entries are visible.
 */
@Composable
fun ScrollToTopOnInsert(listState: LazyListState, topKey: Any?) {
    LaunchedEffect(listState, topKey) {
        if (topKey != null && listState.firstVisibleItemIndex <= NEAR_TOP_ITEMS) {
            listState.scrollToItem(0)
        }
    }
}

private const val NEAR_TOP_ITEMS = 3
