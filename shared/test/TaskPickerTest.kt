package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.TaskPicker
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.Tier
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskPickerTest {
    private fun template(id: String, energy: Energy) = TaskTemplate(id, id, Tier.Two, energy)

    private val low = template("low", Energy.Low)
    private val medium = template("medium", Energy.Medium)
    private val high = template("high", Energy.High)
    private val all = listOf(high, medium, low)

    @Test
    fun `the weight table is 4 2 1 and zero when more energy is needed`() {
        val expected = mapOf(
            Energy.Low to listOf(4, 0, 0),
            Energy.Medium to listOf(2, 4, 0),
            Energy.High to listOf(1, 2, 4),
        )
        for ((chosen, weights) in expected) {
            assertEquals(
                weights,
                listOf(Energy.Low, Energy.Medium, Energy.High).map { TaskPicker.weight(chosen, it) },
                "chosen $chosen (template Low, Medium, High)",
            )
        }
    }

    @Test
    fun `low energy only offers low tasks and medium never offers high`() {
        assertEquals(listOf(low), TaskPicker.eligible(Energy.Low, all, emptySet()))
        assertEquals(listOf(medium, low), TaskPicker.eligible(Energy.Medium, all, emptySet()))
        assertEquals(all, TaskPicker.eligible(Energy.High, all, emptySet()))
    }

    @Test
    fun `excluded templates are never eligible`() {
        assertEquals(listOf(low), TaskPicker.eligible(Energy.Medium, all, setOf("medium")))
        assertNull(TaskPicker.pick(Random(1), Energy.Low, all, setOf("low")))
    }

    @Test
    fun `scripted draws map onto the 4 2 1 weights in template order`() {
        // High day, order high (4), medium (2), low (1): total 7
        val picks = (0..6).map { draw -> TaskPicker.pick(ScriptedRandom(draw), Energy.High, all)!!.id }
        assertEquals(listOf("high", "high", "high", "high", "medium", "medium", "low"), picks)
    }

    @Test
    fun `a medium day with a medium and a low task splits 2 to 1`() {
        val templates = listOf(medium, low)
        val picks = (0..5).map { draw -> TaskPicker.pick(ScriptedRandom(draw), Energy.Medium, templates)!!.id }
        assertEquals(listOf("medium", "medium", "medium", "medium", "low", "low"), picks)
    }

    @Test
    fun `templates with the same energy have the same chance`() {
        val a = template("a", Energy.Medium)
        val b = template("b", Energy.Medium)
        val picks = (0..7).map { draw -> TaskPicker.pick(ScriptedRandom(draw), Energy.Medium, listOf(a, b))!!.id }
        assertEquals(4, picks.count { it == "a" })
        assertEquals(4, picks.count { it == "b" })
    }

    @Test
    fun `nothing eligible gives null without drawing`() {
        assertNull(TaskPicker.pick(ScriptedRandom(), Energy.Low, listOf(medium, high)))
        assertNull(TaskPicker.pick(ScriptedRandom(), Energy.High, emptyList()))
    }

    @Test
    fun `a real random source follows the 4 2 1 weights`() {
        val random = Random(3)
        val counts = mutableMapOf("high" to 0, "medium" to 0, "low" to 0)
        repeat(7000) {
            val picked = TaskPicker.pick(random, Energy.High, all)!!.id
            counts[picked] = counts.getValue(picked) + 1
        }
        assertTrue(counts.getValue("high") in 3700..4300, "high ${counts["high"]}")
        assertTrue(counts.getValue("medium") in 1700..2300, "medium ${counts["medium"]}")
        assertTrue(counts.getValue("low") in 800..1200, "low ${counts["low"]}")
    }

    @Test
    fun `a real random source never suggests a task needing more energy`() {
        val random = Random(11)
        repeat(500) {
            assertTrue(TaskPicker.pick(random, Energy.Medium, all)!!.energy != Energy.High)
            assertEquals(Energy.Low, TaskPicker.pick(random, Energy.Low, all)!!.energy)
        }
    }
}
