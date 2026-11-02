package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.ReminderKind
import com.usuallycoding.openadhd.domain.RememberedPlan
import com.usuallycoding.openadhd.domain.UpcomingReminder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class RememberedPlanTest {
    private fun plan(entryId: String) =
        listOf(UpcomingReminder(entryId, "Dishes", listOf(Instant.parse("2026-11-03T15:00:00Z")), ReminderKind.Notification))

    @Test
    fun `there is no plan before the first one is handed over`() {
        assertNull(RememberedPlan().last)
    }

    @Test
    fun `the plan is handed back as it was given and reading it changes nothing`() {
        val remembered = RememberedPlan()
        val given = plan("a")
        remembered.remember(given)

        assertEquals(given, remembered.last)
        assertEquals(given, remembered.last)
    }

    @Test
    fun `an empty plan is a plan`() {
        val remembered = RememberedPlan()
        remembered.remember(emptyList())

        assertNotNull(remembered.last)
        assertEquals(emptyList(), remembered.last)
    }

    @Test
    fun `a newer plan replaces the older one`() {
        val remembered = RememberedPlan()
        remembered.remember(plan("a"))
        remembered.remember(plan("b"))

        assertEquals(plan("b"), remembered.last)
    }
}
