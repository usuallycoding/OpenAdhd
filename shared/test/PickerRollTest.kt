package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.PickerRoll
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.today.TodayViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PickerRollTest {
    private fun template(name: String) = TaskTemplate(name, name, Tier.One, Energy.Low)

    private val a = template("A")
    private val b = template("B")
    private val c = template("C")

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `an empty list gives nothing and draws nothing`() {
        val random = ScriptedRandom()
        assertNull(PickerRoll.pick(random, emptyList(), null))
        assertTrue(random.bounds.isEmpty())
    }

    @Test
    fun `a single template is returned again and again`() {
        val random = ScriptedRandom()
        assertEquals(a, PickerRoll.pick(random, listOf(a), null))
        assertEquals(a, PickerRoll.pick(random, listOf(a), "A"))
    }

    @Test
    fun `with several shown the previous one is left out`() {
        val random = ScriptedRandom(0, 1)
        // B and C remain, in order, so draws 0 and 1 give B then C.
        assertEquals(b, PickerRoll.pick(random, listOf(a, b, c), "A"))
        assertEquals(c, PickerRoll.pick(random, listOf(a, b, c), "A"))
        assertEquals(listOf(2, 2), random.bounds)
    }

    @Test
    fun `two templates alternate`() {
        val random = ScriptedRandom(0, 0)
        assertEquals(b, PickerRoll.pick(random, listOf(a, b), "A"))
        assertEquals(a, PickerRoll.pick(random, listOf(a, b), "B"))
    }

    @Test
    fun `every template is reachable and equally likely on the first roll`() {
        val random = ScriptedRandom(0, 1, 2)
        val picks = (0 until 3).map { PickerRoll.pick(random, listOf(a, b, c), null) }
        assertEquals(listOf(a, b, c), picks)
        assertEquals(listOf(3, 3, 3), random.bounds)
    }

    @Test
    fun `a previous id that is no longer listed changes nothing`() {
        val random = ScriptedRandom(1)
        assertEquals(b, PickerRoll.pick(random, listOf(a, b), "gone"))
        assertEquals(listOf(2), random.bounds)
    }

    @Test
    fun `the real random source never repeats the previous template`() {
        val shown = listOf(a, b, c)
        var previous: String? = null
        repeat(200) {
            val rolled = PickerRoll.pick(Random(it), shown, previous)!!
            assertTrue(rolled.id != previous)
            previous = rolled.id
        }
    }

    @Test
    fun `the view model rolls with its own random source and changes nothing`() = runTest {
        val store = InMemoryStore()
        val first = store.create("Laundry", Tier.One, Energy.Low)
        val second = store.create("Dishes", Tier.One, Energy.Low)
        val vm = TodayViewModel(store, dateCheckInterval = null, random = ScriptedRandom(0))
        val rolled = vm.rollTemplate(listOf(first, second), null)
        assertEquals(first, rolled)
        assertTrue(store.entriesFor(LocalDate(2026, 10, 25)).first().isEmpty())
        assertTrue(store.daysWithEntries.first().isEmpty())
        assertTrue(store.entries.first().isEmpty())
    }

    @Test
    fun `a reveal made before anything collects is delivered once and changes nothing`() = runTest {
        val store = InMemoryStore()
        val vm = TodayViewModel(store, dateCheckInterval = null)
        vm.reveal("a")
        assertEquals("a", vm.revealed.first())
        assertTrue(store.daysWithEntries.first().isEmpty())

        // Nothing is left to deliver a second time; a newer reveal replaces an undelivered one.
        vm.reveal("b")
        vm.reveal("c")
        assertEquals("c", vm.revealed.first())
    }
}
