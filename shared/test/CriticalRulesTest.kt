package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.CompletionResult
import com.usuallycoding.openadhd.domain.CriticalHits
import com.usuallycoding.openadhd.domain.CriticalRules
import com.usuallycoding.openadhd.domain.GameRules
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CriticalRulesTest {
    @Test
    fun `the default chance is one in eight and the multiplier is two`() {
        assertEquals(8, GameRules.Default.criticalOneIn)
        assertEquals(2, GameRules.Default.criticalMultiplier)
    }

    @Test
    fun `the bonus equals the normal reward`() {
        assertEquals(20, CriticalRules.bonus(20))
        assertEquals(7, CriticalRules.bonus(7))
        assertEquals(1, CriticalRules.bonus(1))
        assertEquals(40, 20 + CriticalRules.bonus(20))
    }

    @Test
    fun `never is never critical`() {
        repeat(50) { assertFalse(CriticalHits.Never.isCritical(8)) }
    }

    @Test
    fun `only a draw of zero is a critical hit`() {
        val hits = CriticalHits.standard(ScriptedRandom(0, 1, 7, 0, 4))
        assertEquals(listOf(true, false, false, true, false), List(5) { hits.isCritical(8) })
    }

    @Test
    fun `the chance asked for is the one drawn from`() {
        val random = ScriptedRandom(0, 0)
        val hits = CriticalHits.standard(random)
        assertTrue(hits.isCritical(2))
        assertTrue(hits.isCritical(1000))
        assertEquals(listOf(2, 1000), random.bounds)
    }

    @Test
    fun `a changed multiplier changes the bonus`() {
        val rules = GameRules(criticalMultiplier = 3)
        assertEquals(40, CriticalRules.bonus(20, rules))
        assertEquals(60, 20 + CriticalRules.bonus(20, rules))
    }

    @Test
    fun `about one completion in eight is critical over many draws`() {
        val hits = CriticalHits.standard(Random(7))
        val critical = (1..8000).count { hits.isCritical(GameRules.Default.criticalOneIn) }
        assertTrue(critical in 800..1200, "expected about 1000 critical hits, got $critical")
    }

    @Test
    fun `a completion result knows whether it was critical`() {
        assertFalse(CompletionResult(reward = 7).isCritical)
        assertTrue(CompletionResult(reward = 7, bonus = 7).isCritical)
    }

    @Test
    fun `the scripted source answers in order`() {
        val crits = ScriptedCrits(true, false)
        assertTrue(crits.isCritical(8))
        assertEquals(1, crits.remaining)
        assertFalse(crits.isCritical(8))
        assertEquals(0, crits.remaining)
    }
}
