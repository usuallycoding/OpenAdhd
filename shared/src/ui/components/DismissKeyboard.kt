package com.usuallycoding.openadhd.ui.components

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * Puts the keyboard away when the user taps this area or scrolls something inside it: focus is cleared, which hides the
 * on-screen keyboard on every platform. Put it on the area under a search field, not on the field itself, or a tap on the
 * field would take focus and lose it again.
 */
@Composable
fun Modifier.dismissKeyboardOnTapOrScroll(): Modifier {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val connection = remember(focus, keyboard) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) focus.putKeyboardAway(keyboard)
                return Offset.Zero
            }
        }
    }
    return this
        .nestedScroll(connection)
        .pointerInput(focus, keyboard) { detectTapGestures(onTap = { focus.putKeyboardAway(keyboard) }) }
}

internal fun FocusManager.putKeyboardAway(keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?) {
    clearFocus()
    keyboard?.hide()
}
