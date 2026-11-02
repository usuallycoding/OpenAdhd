package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.ui.components.SheetDismissGate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SheetDismissGateTest {
    private var keyboardShowing = true
    private var putAway = 0
    private val gate = SheetDismissGate({ keyboardShowing }, { putAway++ })

    @Test
    fun `moving to hidden with the keyboard showing is refused and puts the keyboard away`() {
        assertFalse(gate.allows(movingToHidden = true))
        assertEquals(1, putAway)
    }

    @Test
    fun `the next move to hidden with the keyboard gone is allowed`() {
        gate.allows(movingToHidden = true)
        keyboardShowing = false

        assertTrue(gate.allows(movingToHidden = true))
        assertEquals(1, putAway)
    }

    @Test
    fun `a close the sheet asked for itself goes through with the keyboard showing`() {
        gate.closing = true

        assertTrue(gate.allows(movingToHidden = true))
        assertEquals(0, putAway)
    }

    @Test
    fun `any other target is allowed and leaves the keyboard alone`() {
        assertTrue(gate.allows(movingToHidden = false))
        assertEquals(0, putAway)
    }

    @Test
    fun `without a keyboard nothing is refused`() {
        keyboardShowing = false

        assertTrue(gate.allows(movingToHidden = true))
        assertEquals(0, putAway)
    }
}
