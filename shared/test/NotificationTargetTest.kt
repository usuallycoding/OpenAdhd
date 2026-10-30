package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.NotificationTarget
import com.usuallycoding.openadhd.domain.TemplateReminders
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class NotificationTargetTest {
    @Test
    fun `two entries get different targets and codes`() {
        assertNotEquals(NotificationTarget.uri("a"), NotificationTarget.uri("b"))
        assertNotEquals(NotificationTarget.requestCode("a"), NotificationTarget.requestCode("b"))
    }

    @Test
    fun `the same entry always gets the same target`() {
        assertEquals(NotificationTarget.uri("a"), NotificationTarget.uri("a"))
        assertEquals(NotificationTarget.requestCode("a"), NotificationTarget.requestCode("a"))
    }

    @Test
    fun `the alarm target is not the reminder target`() {
        assertNotEquals(NotificationTarget.uri("a"), NotificationTarget.alarmUri("a"))
        assertEquals("openadhd://entry/abc/alarm", NotificationTarget.alarmUri("abc"))
    }

    @Test
    fun `the address names the entry`() {
        assertEquals("openadhd://entry/abc-123", NotificationTarget.uri("abc-123"))
    }

    @Test
    fun `a planned template reminder gets its own target for each day`() {
        val monday = TemplateReminders.id("t", LocalDate(2026, 10, 26))
        val tuesday = TemplateReminders.id("t", LocalDate(2026, 10, 27))
        assertNotEquals(NotificationTarget.uri(monday), NotificationTarget.uri(tuesday))
        assertNotEquals(NotificationTarget.requestCode(monday), NotificationTarget.requestCode(tuesday))
        assertEquals("openadhd://entry/template:t:2026-10-26", NotificationTarget.uri(monday))
    }
}
