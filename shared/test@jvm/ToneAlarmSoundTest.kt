package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.data.AlarmTone
import com.usuallycoding.openadhd.data.ToneAlarmSound
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToneAlarmSoundTest {
    private class Line : AutoCloseable {
        val closed = CountDownLatch(1)

        override fun close() = closed.countDown()
    }

    /** Waits for something another thread does, instead of guessing how long it takes. */
    private fun CountDownLatch.arrives() = assertTrue(await(5, TimeUnit.SECONDS))

    @Test
    fun `the pattern is 16-bit mono with beeps then silence`() {
        val pcm = AlarmTone.pcm()
        val format = AlarmTone.format
        assertEquals(44100f, format.sampleRate)
        assertEquals(16, format.sampleSizeInBits)
        assertEquals(1, format.channels)
        assertEquals(false, format.isBigEndian)
        assertEquals((44100 * 1.5).toInt() * 2, pcm.size)
        assertTrue(pcm.any { it.toInt() != 0 })
        val firstBeepEnds = (44100 * 0.15).toInt() * 2
        assertTrue(pcm.take(firstBeepEnds).any { it.toInt() != 0 })
        assertTrue(pcm.takeLast(44100).all { it.toInt() == 0 })
    }

    @Test
    fun `start opens the pattern once and stop closes it`() {
        val opened = CountDownLatch(1)
        val opens = AtomicInteger()
        val line = Line()
        val sound = ToneAlarmSound { format, pcm ->
            assertEquals(AlarmTone.format.sampleRate, format.sampleRate)
            assertEquals(AlarmTone.pcm().size, pcm.size)
            opens.incrementAndGet()
            opened.countDown()
            line
        }
        sound.start()
        sound.start()
        opened.arrives()
        assertFalse(line.closed.count == 0L)

        sound.stop()
        line.closed.arrives()
        assertEquals(1, opens.get())
    }

    @Test
    fun `a stop that comes before the output has opened closes it as soon as it does`() {
        val release = CountDownLatch(1)
        val entered = CountDownLatch(1)
        val line = Line()
        val sound = ToneAlarmSound { _, _ ->
            entered.countDown()
            release.arrives()
            line
        }
        sound.start()
        entered.arrives()
        sound.stop()
        release.countDown()
        line.closed.arrives()
    }

    @Test
    fun `a sound that cannot open stays silent without an error`() {
        val tried = CountDownLatch(1)
        val sound = ToneAlarmSound { _, _ ->
            tried.countDown()
            throw IllegalStateException("no audio device")
        }
        sound.start()
        tried.arrives()
        sound.stop()
        sound.stop()
    }

    @Test
    fun `stop without start and a second round both work`() {
        val first = Line()
        val second = Line()
        val lines = ArrayDeque(listOf(first, second))
        val opened = AtomicInteger()
        val sound = ToneAlarmSound { _, _ ->
            opened.incrementAndGet()
            lines.removeFirst()
        }
        sound.stop()
        sound.start()
        while (opened.get() < 1) Thread.sleep(5)
        sound.stop()
        first.closed.arrives()

        sound.start()
        while (opened.get() < 2) Thread.sleep(5)
        sound.stop()
        second.closed.arrives()
    }
}
