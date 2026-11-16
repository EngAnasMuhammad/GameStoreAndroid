package com.anasdev.gamestoreandroid.util

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

/**
 * Consumes leftover upward (negative y) scroll and fling velocity so it cannot leak
 * into [androidx.compose.material3.ModalBottomSheet]'s internal draggable state.
 *
 * This prevents the sheet from violently jumping downward and bouncing back up when
 * the user swipes or flings up aggressively inside an already-expanded sheet.
 * Downward (positive y) scroll passes through unconsumed so swipe-to-dismiss works normally.
 *
 * Apply this modifier to the outermost content container wrapping the scrollable content
 * inside the [androidx.compose.material3.ModalBottomSheet].
 */
fun Modifier.consumeUpwardSheetOverscroll(): Modifier = composed {
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = if (available.y < 0f) Offset(0f, available.y) else Offset.Zero

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity = if (available.y < 0f) Velocity(0f, available.y) else Velocity.Zero
        }
    }
    this.nestedScroll(connection)
}
