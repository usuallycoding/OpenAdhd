package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.Redemption
import com.usuallycoding.openadhd.domain.RedemptionSource
import com.usuallycoding.openadhd.domain.RewardTemplate
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.RollRules
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class RollRulesTest {
    private fun reward(id: String, tier: RewardTier, customPrice: Int? = null) =
        RewardTemplate(id, id, tier, Energy.Low, customPrice = customPrice)

    private val pool = listOf(
        reward("a1", RewardTier.One), reward("a2", RewardTier.One),
        reward("b1", RewardTier.Two), reward("b2", RewardTier.Two), reward("b3", RewardTier.Two),
        reward("c1", RewardTier.Three),
        reward("d1", RewardTier.Four), reward("d2", RewardTier.Four),
        reward("e1", RewardTier.Five, 900),
    )

    @Test
    fun `a roll costs the tier price`() {
        assertEquals(listOf(20, 50, 120, 300), RollRules.RollableTiers.map { RollRules.price(it) })
        assertFailsWith<IllegalArgumentException> { RollRules.price(RewardTier.Five) }
    }

    @Test
    fun `tiers upgrade one step up and tier four has no step`() {
        assertEquals(RewardTier.Two, RollRules.nextTier(RewardTier.One))
        assertEquals(RewardTier.Three, RollRules.nextTier(RewardTier.Two))
        assertEquals(RewardTier.Four, RollRules.nextTier(RewardTier.Three))
        assertNull(RollRules.nextTier(RewardTier.Four))
    }

    @Test
    fun `the upgrade chance is one in six`() {
        assertEquals(6, RollRules.UpgradeOneIn)
    }

    @Test
    fun `usual outcome picks from the rolled tier`() {
        // 3 means no upgrade, 2 picks the third tier 2 reward
        val outcome = RollRules.choose(ScriptedRandom(3, 2), RewardTier.Two, pool)!!
        assertEquals("b3", outcome.reward.id)
        assertFalse(outcome.upgraded)
    }

    @Test
    fun `only a draw of zero upgrades`() {
        for (draw in 1..5) {
            assertFalse(RollRules.choose(ScriptedRandom(draw, 0), RewardTier.Two, pool)!!.upgraded, "draw $draw")
        }
        assertTrue(RollRules.choose(ScriptedRandom(0, 0), RewardTier.Two, pool)!!.upgraded)
    }

    @Test
    fun `an upgrade picks from the next tier`() {
        val outcome = RollRules.choose(ScriptedRandom(0, 0), RewardTier.One, pool)!!
        assertEquals("b1", outcome.reward.id)
        assertTrue(outcome.upgraded)
        assertEquals("b3", RollRules.choose(ScriptedRandom(0, 2), RewardTier.One, pool)!!.reward.id)
    }

    @Test
    fun `an upgrade falls back to the rolled tier when the next tier is empty`() {
        val withoutTierThree = pool.filter { it.tier != RewardTier.Three }
        val outcome = RollRules.choose(ScriptedRandom(0, 1), RewardTier.Two, withoutTierThree)!!
        assertEquals("b2", outcome.reward.id)
        assertFalse(outcome.upgraded)
    }

    @Test
    fun `tier four never upgrades and tier five is never given`() {
        val outcome = RollRules.choose(ScriptedRandom(0, 1), RewardTier.Four, pool)!!
        assertEquals("d2", outcome.reward.id)
        assertFalse(outcome.upgraded)
        repeat(200) {
            val rolled = RollRules.choose(Random(it), RewardTier.Four, pool)!!
            assertEquals(RewardTier.Four, rolled.reward.tier)
        }
    }

    @Test
    fun `an empty tier gives nothing`() {
        assertNull(RollRules.choose(ScriptedRandom(), RewardTier.Three, pool.filter { it.tier != RewardTier.Three }))
    }

    @Test
    fun `tier five cannot be rolled`() {
        assertFailsWith<IllegalArgumentException> { RollRules.choose(Random(1), RewardTier.Five, pool) }
    }

    @Test
    fun `a real random source never gives a lower tier and upgrades about one time in six`() {
        val random = Random(42)
        val trials = 6000
        var upgrades = 0
        repeat(trials) {
            val outcome = RollRules.choose(random, RewardTier.Two, pool)!!
            assertTrue(outcome.reward.tier.level >= RewardTier.Two.level)
            if (outcome.upgraded) {
                upgrades++
                assertEquals(RewardTier.Three, outcome.reward.tier)
            }
        }
        assertTrue(upgrades in 800..1200, "expected about 1000 upgrades, got $upgrades")
    }

    @Test
    fun `a redemption knows whether it was a lucky upgrade`() {
        fun redemption(tier: RewardTier, rolled: RewardTier?) = Redemption(
            "r", "t", "Reward", tier, Energy.Low, 50, Instant.fromEpochSeconds(0),
            if (rolled == null) RedemptionSource.Purchase else RedemptionSource.Roll, rolled,
        )
        assertTrue(redemption(RewardTier.Three, RewardTier.Two).isLuckyUpgrade)
        assertFalse(redemption(RewardTier.Two, RewardTier.Two).isLuckyUpgrade)
        assertFalse(redemption(RewardTier.Two, null).isLuckyUpgrade)
    }
}
