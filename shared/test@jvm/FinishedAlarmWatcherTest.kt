package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.data.AlarmSound
import com.usuallycoding.openadhd.data.DesktopAlarmRinger
import com.usuallycoding.openadhd.data.DueAlarm
import com.usuallycoding.openadhd.data.FinishedAlarmWatcher
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FinishedAlarmWatcherTest {
    private val clock = FakeClock()
    private val store = InMemoryStore(clock)
    private val today = clock.todayIn(TimeZone.UTC)

    private class QuietSound : AlarmSound {
        override fun start() = Unit

        override fun stop() = Unit
    }

    private fun TestScope.ringer() = DesktopAlarmRinger(QuietSound(), scope = backgroundScope, clock = clock, zone = TimeZone.UTC)

    private suspend fun task(name: String) = store.assign(store.create(name, Tier.Two, Energy.Low), today, TimeZone.UTC)

    private fun DesktopAlarmRinger.ids() = rows.value.map { it.entryId }

    @Test
    fun `completing a task ends its row and only its row`() = runTest {
        val ringer = ringer()
        FinishedAlarmWatcher(store, ringer, backgroundScope).start()
        val a = task("Take medication")
        val b = task("Taxes")
        ringer.ring(DueAlarm(a.id, a.name))
        ringer.ring(DueAlarm(b.id, b.name))
        runCurrent()
        assertEquals(listOf(a.id, b.id), ringer.ids())

        store.completeTask(a.id)
        runCurrent()
        assertEquals(listOf(b.id), ringer.ids())
    }

    @Test
    fun `deleting a task ends its row`() = runTest {
        val ringer = ringer()
        FinishedAlarmWatcher(store, ringer, backgroundScope).start()
        val a = task("Take medication")
        ringer.ring(DueAlarm(a.id, a.name))
        runCurrent()

        store.remove(a.id)
        runCurrent()
        assertTrue(ringer.rows.value.isEmpty())
    }

    @Test
    fun `a row of a scheduled template without an entry stays`() = runTest {
        val ringer = ringer()
        FinishedAlarmWatcher(store, ringer, backgroundScope).start()
        val a = task("Take medication")
        val planned = "template:t1:$today"
        ringer.ring(DueAlarm(planned, "Morning pills"))
        ringer.ring(DueAlarm(a.id, a.name))
        runCurrent()

        store.completeTask(a.id)
        runCurrent()
        assertEquals(listOf(planned), ringer.ids())
    }

    @Test
    fun `a task that is still pending keeps ringing`() = runTest {
        val ringer = ringer()
        FinishedAlarmWatcher(store, ringer, backgroundScope).start()
        val a = task("Take medication")
        ringer.ring(DueAlarm(a.id, a.name))
        task("Something else")
        runCurrent()
        assertEquals(listOf(a.id), ringer.ids())
    }

    @Test
    fun `a ring for a task that is already done ends at once`() = runTest {
        val ringer = ringer()
        FinishedAlarmWatcher(store, ringer, backgroundScope).start()
        val a = task("Take medication")
        store.completeTask(a.id)
        runCurrent()

        ringer.ring(DueAlarm(a.id, a.name))
        runCurrent()
        assertTrue(ringer.rows.value.isEmpty())
    }
}
