package ru.loveyou.app

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable

/**
 * Small local helper for count-based LazyColumn items.
 * Kept in the app package so call sites do not depend on an additional import.
 */
fun LazyListScope.items(
    count: Int,
    itemContent: @Composable LazyItemScope.(index: Int) -> Unit
) {
    repeat(count) { index ->
        item(key = index) {
            itemContent(index)
        }
    }
}
