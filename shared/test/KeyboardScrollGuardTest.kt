package com.usuallycoding.openadhd

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.usuallycoding.openadhd.ui.components.KeyboardScrollGuard
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyboardScrollGuardTest {
    private var keyboardVisible = true
    private var putAway = 0
    private val guard = KeyboardScrollGuard({ keyboardVisible }, { putAway++; })

    private val down = Offset(0f, 40f)
    private val user = NestedScrollSource.UserInput

    @Test
    fun `a scroll with the keyboard open puts it away and keeps the rest of the gesture from the sheet`() = runTest {
        assertEquals(Offset.Zero, guard.onPreScroll(down, user))
        assertEquals(1, putAway)
        // the keyboard goes within the gesture; the rest of the gesture is still ours
        keyboardVisible = false
        assertEquals(Offset.Zero, guard.onPreScroll(down, user))
        assertEquals(down, guard.onPostScroll(Offset.Zero, down, user))
        assertEquals(1, putAway)
        val fling = Velocity(0f, 900f)
        assertEquals(fling, guard.onPostFling(Velocity.Zero, fling))
    }

    @Test
    fun `the next gesture with the keyboard gone reaches the sheet`() = runTest {
        guard.onPreScroll(down, user)
        guard.onPostScroll(Offset.Zero, down, user)
        guard.onPostFling(Velocity.Zero, Velocity(0f, 900f))
        keyboardVisible = false

        assertEquals(Offset.Zero, guard.onPreScroll(down, user))
        assertEquals(Offset.Zero, guard.onPostScroll(Offset.Zero, down, user))
        assertEquals(Velocity.Zero, guard.onPostFling(Velocity.Zero, Velocity(0f, 900f)))
        assertEquals(1, putAway)
    }

    @Test
    fun `a gesture that ends without velocity also ends the guard`() = runTest {
        guard.onPreScroll(down, user)
        guard.onPostFling(Velocity.Zero, Velocity.Zero)
        keyboardVisible = false

        assertEquals(Offset.Zero, guard.onPostScroll(Offset.Zero, down, user))
    }

    @Test
    fun `without a keyboard nothing is put away and nothing is kept from the sheet`() = runTest {
        keyboardVisible = false

        assertEquals(Offset.Zero, guard.onPreScroll(down, user))
        assertEquals(Offset.Zero, guard.onPostScroll(Offset.Zero, down, user))
        assertEquals(Velocity.Zero, guard.onPostFling(Velocity.Zero, Velocity(0f, 900f)))
        assertEquals(0, putAway)
    }

    @Test
    fun `scrolling that is not the user's is ignored`() = runTest {
        assertEquals(Offset.Zero, guard.onPreScroll(down, NestedScrollSource.SideEffect))
        assertEquals(Offset.Zero, guard.onPostScroll(Offset.Zero, down, NestedScrollSource.SideEffect))
        assertEquals(0, putAway)
    }
}
