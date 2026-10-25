package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.NotificationTarget
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
}
