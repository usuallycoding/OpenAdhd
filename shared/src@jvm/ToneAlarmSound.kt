package com.usuallycoding.openadhd.data

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** The synthesized pattern of the desktop alarm: three short beeps and a pause, as 16-bit mono PCM. */
object AlarmTone {
    const val SampleRate = 44100f
    private const val Pitch = 880.0
    private const val BeepSeconds = 0.15
    private const val GapSeconds = 0.08
    private const val Beeps = 3
    private const val PatternSeconds = 1.5
    private const val FadeSeconds = 0.005
    private const val Volume = 0.6

    val format = AudioFormat(SampleRate, 16, 1, true, false)

    /** One pattern, which is played in a loop; it ends in silence so the loop has a pause between rounds. */
    fun pcm(): ByteArray {
        val frames = (SampleRate * PatternSeconds).toInt()
        val bytes = ByteArray(frames * 2)
        val beepFrames = (SampleRate * BeepSeconds).toInt()
        val stepFrames = (SampleRate * (BeepSeconds + GapSeconds)).toInt()
        val fadeFrames = (SampleRate * FadeSeconds).toInt()
        for (beep in 0 until Beeps) {
            for (i in 0 until beepFrames) {
                // A short fade in and out keeps each beep from clicking.
                val fade = min(1.0, min(i, beepFrames - 1 - i).toDouble() / fadeFrames)
                val value = (sin(2 * PI * Pitch * i / SampleRate) * fade * Volume * Short.MAX_VALUE).toInt()
                val at = (beep * stepFrames + i) * 2
                bytes[at] = value.toByte()
                bytes[at + 1] = (value shr 8).toByte()
            }
        }
        return bytes
    }
}

/**
 * The alarm sound: [AlarmTone] looped through the system's audio output until [stop]. Opening the output can be slow
 * and can fail (no device, a busy or refusing system), so it happens on its own thread and a failure leaves the ring
 * silent, with the card and notification unaffected. [open] starts playing the pattern in a loop and returns what
 * stops it.
 */
class ToneAlarmSound(
    private val open: (format: AudioFormat, pcm: ByteArray) -> AutoCloseable = ::openLoopingClip,
) : AlarmSound {
    private val logger = System.getLogger("OpenAdhd")
    private var wanted = false
    private var round = 0
    private var playing: AutoCloseable? = null

    @Synchronized
    override fun start() {
        if (wanted) return
        wanted = true
        val mine = ++round
        thread(isDaemon = true, name = "alarm-sound") {
            val started = try {
                open(AlarmTone.format, AlarmTone.pcm())
            } catch (e: Exception) {
                logger.log(System.Logger.Level.WARNING, "The alarm sound could not play; the alarm stays silent", e)
                return@thread
            }
            synchronized(this) {
                if (wanted && round == mine) playing = started else runCatching { started.close() }
            }
        }
    }

    @Synchronized
    override fun stop() {
        wanted = false
        val current = playing
        playing = null
        if (current != null) runCatching { current.close() }
    }
}

private fun openLoopingClip(format: AudioFormat, pcm: ByteArray): AutoCloseable {
    val clip = AudioSystem.getClip()
    clip.open(format, pcm, 0, pcm.size)
    clip.loop(Clip.LOOP_CONTINUOUSLY)
    return AutoCloseable {
        clip.stop()
        clip.close()
    }
}
