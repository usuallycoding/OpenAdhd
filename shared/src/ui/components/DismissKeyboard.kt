package com.usuallycoding.openadhd.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
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

fun FocusManager.putKeyboardAway(keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?) {
    clearFocus()
    keyboard?.hide()
}

/** Whether the on-screen keyboard is showing: its inset is above zero, which it never is on the desktop. */
@Composable
fun rememberKeyboardShowing(): State<Boolean> = rememberUpdatedState(WindowInsets.ime.getBottom(LocalDensity.current) > 0)

/**
 * Makes a downward scroll on a sheet body put away only the on-screen keyboard: while the keyboard is showing, the
 * first user scroll of a gesture hides it, and the rest of that gesture's scroll and fling is kept from the sheet
 * around it, so the sheet's own swipe-to-dismiss does not take it. The next gesture, with the keyboard gone, reaches
 * the sheet as usual. With no keyboard showing, which is always so on the desktop, it changes nothing. Put it above
 * the scrolling body, so its post-scroll runs before the sheet's.
 */
@Composable
fun Modifier.putKeyboardAwayOnScroll(): Modifier {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val keyboardVisible by rememberKeyboardShowing()
    val guard = remember(focus, keyboard) { KeyboardScrollGuard({ keyboardVisible }, { focus.putKeyboardAway(keyboard) }) }
    return this.nestedScroll(guard)
}

/** The scroll logic of [putKeyboardAwayOnScroll], apart from Compose so it can be tested on its own. */
class KeyboardScrollGuard(
    private val keyboardVisible: () -> Boolean,
    private val putKeyboardAway: () -> Unit,
) : NestedScrollConnection {
    /** Set by the first user scroll of a gesture that found the keyboard showing; cleared when the gesture ends. */
    private var swallowing = false

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput && !swallowing && keyboardVisible()) {
            putKeyboardAway()
            swallowing = true
        }
        return Offset.Zero
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        if (swallowing && source == NestedScrollSource.UserInput) available else Offset.Zero

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val swallowed = swallowing
        swallowing = false
        return if (swallowed) available else Velocity.Zero
    }
}

/**
 * Decides whether a bottom sheet may move to Hidden. While the keyboard is showing it may not: the keyboard is put
 * away instead and the sheet stays open, so a swipe on the sheet's title or footer, or a tap on the dimmed area,
 * only closes the keyboard. A close the sheet asks for itself (Save, Cancel) sets [closing] first and always goes through.
 */
class SheetDismissGate(
    private val keyboardShowing: () -> Boolean,
    private val putKeyboardAway: () -> Unit,
) {
    var closing = false

    /** Whether the sheet may move to the target; [movingToHidden] is false for any other target. */
    fun allows(movingToHidden: Boolean): Boolean {
        if (!movingToHidden || closing || !keyboardShowing()) return true
        putKeyboardAway()
        return false
    }
}
