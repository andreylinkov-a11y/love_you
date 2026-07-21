package ru.loveyou.app

import androidx.compose.runtime.Composable

/**
 * Small compatibility wrapper for the v4 screen.
 * It avoids ambiguous implicit layout receivers in the Compose version used by this project.
 */
@Composable
fun AnimatedVisibility(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    if (visible) content()
}
