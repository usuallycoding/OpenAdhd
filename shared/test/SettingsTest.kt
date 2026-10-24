package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.GameRules
import com.usuallycoding.openadhd.domain.RewardTemplate
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.SettingCheck
import com.usuallycoding.openadhd.domain.SettingKey
import com.usuallycoding.openadhd.domain.Settings
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.domain.checkSetting
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsTest {
    private val defaults = Settings.Default

    private fun check(
        key: SettingKey,
        text: String,
        current: Settings = defaults,
        rewards: List<RewardTemplate> = emptyList(),
        templates: List<TaskTemplate> = emptyList(),
    ) = checkSetting(key, text, current, rewards, templates)

    private fun refusal(
        key: SettingKey,
        text: String,
        current: Settings = defaults,
        rewards: List<RewardTemplate> = emptyList(),
        templates: List<TaskTemplate> = emptyList(),
    ): String = assertIs<SettingCheck.Refused>(check(key, text, current, rewards, templates)).message

    private fun accepted(key: SettingKey, text: String, current: Settings = defaults): Settings =
        assertIs<SettingCheck.Accepted>(check(key, text, current)).settings

    @Test
    fun `the defaults are the original rules`() {
        val rules = defaults.rules
        assertEquals(500, rules.debtLimit)
        assertEquals(10, rules.subtaskCeiling)
        assertEquals(listOf(5, 10, 20, 40), rules.taskPayouts)
        assertEquals(listOf(20, 50, 120, 300), rules.rewardPrices)
        assertEquals(8, rules.criticalOneIn)
        assertEquals(2, rules.criticalMultiplier)
        assertEquals(6, rules.upgradeOneIn)
        assertEquals("", defaults.name)
        assertEquals("OpenAdhd", defaults.title)
    }

    @Test
    fun `every key shows its default as text and can be set back to it`() {
        for (key in SettingKey.entries) {
            val text = defaults.text(key)
            assertEquals(defaults, defaults.with(key, text.ifEmpty { "" }), key.name)
        }
    }

    @Test
    fun `the title carries the name`() {
        assertEquals("Sam's OpenAdhd", Settings(name = "Sam").title)
        assertEquals("OpenAdhd", Settings(name = "  ").title)
    }

    @Test
    fun `a saved value sets the right rule`() {
        assertEquals(200, accepted(SettingKey.DebtLimit, "200").rules.debtLimit)
        assertEquals(25, accepted(SettingKey.PayoutTier3, "25").rules.payout(Tier.Three))
        assertEquals(60, accepted(SettingKey.PriceTier2, "60").rules.price(RewardTier.Two))
        assertEquals(3, accepted(SettingKey.CriticalMultiplier, "3").rules.criticalMultiplier)
        assertEquals(4, accepted(SettingKey.UpgradeOneIn, " 4 ").rules.upgradeOneIn)
        assertEquals("Sam", accepted(SettingKey.Name, "Sam").name)
    }

    @Test
    fun `a value must be a whole number`() {
        assertEquals("Enter a whole number", refusal(SettingKey.DebtLimit, ""))
        assertEquals("Enter a whole number", refusal(SettingKey.DebtLimit, "12.5"))
        assertEquals("Enter a whole number", refusal(SettingKey.DebtLimit, "abc"))
    }

    @Test
    fun `values must be inside their range and the boundaries are accepted`() {
        assertEquals(0, accepted(SettingKey.DebtLimit, "0").rules.debtLimit)
        assertEquals(100_000, accepted(SettingKey.DebtLimit, "100000").rules.debtLimit)
        assertTrue(refusal(SettingKey.DebtLimit, "100001").contains("between 0 and 100000"))
        assertTrue(refusal(SettingKey.SubtaskCeiling, "0").contains("between 1 and 50"))
        assertEquals(50, accepted(SettingKey.SubtaskCeiling, "50").rules.subtaskCeiling)
        assertTrue(refusal(SettingKey.CriticalOneIn, "1").contains("between 2 and 1000"))
        assertEquals(2, accepted(SettingKey.CriticalOneIn, "2").rules.criticalOneIn)
        assertTrue(refusal(SettingKey.CriticalMultiplier, "11").contains("between 2 and 10"))
        assertTrue(refusal(SettingKey.UpgradeOneIn, "1001").contains("between 2 and 1000"))
        assertTrue(refusal(SettingKey.PayoutTier1, "1001").contains("between 1 and 1000"))
        assertTrue(refusal(SettingKey.PriceTier1, "0").contains("between 1 and 100000"))
    }

    @Test
    fun `the name is trimmed and at most forty characters`() {
        assertEquals("Sam", accepted(SettingKey.Name, "  Sam  ").name)
        assertEquals("a".repeat(40), accepted(SettingKey.Name, "a".repeat(40)).name)
        assertEquals("The name can be at most 40 characters", refusal(SettingKey.Name, "a".repeat(41)))
        assertEquals("", accepted(SettingKey.Name, "   ").name)
    }

    @Test
    fun `payouts must keep increasing from tier one to tier four`() {
        assertEquals("Tier 3 must pay more than tier 2", refusal(SettingKey.PayoutTier3, "10"))
        assertEquals("Tier 3 must pay less than tier 4", refusal(SettingKey.PayoutTier3, "40"))
        assertEquals("Tier 1 must pay less than tier 2", refusal(SettingKey.PayoutTier1, "10"))
        assertEquals(41, accepted(SettingKey.PayoutTier4, "41").rules.payout(Tier.Four))
        assertEquals(9, accepted(SettingKey.PayoutTier1, "9").rules.payout(Tier.One))
    }

    @Test
    fun `prices must keep increasing and a tier can be moved by raising its neighbour first`() {
        assertEquals("Tier 2 must cost less than tier 3", refusal(SettingKey.PriceTier2, "120"))
        assertEquals("Tier 4 must cost more than tier 3", refusal(SettingKey.PriceTier4, "120"))
        val payoutsTwoFirst = accepted(SettingKey.PayoutTier2, "15")
        assertEquals("Tier 1 must pay less than tier 2", refusal(SettingKey.PayoutTier1, "15", payoutsTwoFirst))
        assertEquals(12, accepted(SettingKey.PayoutTier1, "12", payoutsTwoFirst).rules.payout(Tier.One))
        assertEquals("Tier 1 must pay less than tier 2", refusal(SettingKey.PayoutTier1, "12"))
    }

    @Test
    fun `tier four may not cost as much as a tier five reward`() {
        val weekend = RewardTemplate("w", "Weekend away", RewardTier.Five, Energy.High, customPrice = 500)
        val spa = RewardTemplate("s", "Spa", RewardTier.Five, Energy.High, customPrice = 800)
        assertEquals(
            "Tier 4 must cost less than \"Weekend away\", which costs 500",
            refusal(SettingKey.PriceTier4, "500", rewards = listOf(spa, weekend)),
        )
        assertEquals(499, (check(SettingKey.PriceTier4, "499", rewards = listOf(spa, weekend)) as SettingCheck.Accepted).settings.rules.rewardPrices[3])
    }

    @Test
    fun `a payout may not drop below the subtask count of a template of that tier`() {
        val kitchen = TaskTemplate("k", "Clean kitchen", Tier.Two, Energy.Low, subtasks = List(8) { "Step $it" })
        val other = TaskTemplate("o", "Laundry", Tier.Three, Energy.Low, subtasks = List(12) { "Step $it" })
        assertEquals(
            "\"Clean kitchen\" has 8 subtasks and needs at least 8 coins",
            refusal(SettingKey.PayoutTier2, "6", templates = listOf(kitchen, other)),
        )
        assertEquals(8, (check(SettingKey.PayoutTier2, "8", templates = listOf(kitchen)) as SettingCheck.Accepted).settings.rules.payout(Tier.Two))
    }

    @Test
    fun `lowering the ceiling is allowed even when templates have more subtasks`() {
        val kitchen = TaskTemplate("k", "Clean kitchen", Tier.Two, Energy.Low, subtasks = List(8) { "Step $it" })
        val result = check(SettingKey.SubtaskCeiling, "5", templates = listOf(kitchen))
        assertEquals(5, assertIs<SettingCheck.Accepted>(result).settings.rules.subtaskCeiling)
    }

    @Test
    fun `saved overrides are applied and bad rows are ignored`() {
        val settings = Settings.fromOverrides(
            mapOf(
                "debt_limit" to "250",
                "critical_one_in" to "1",
                "subtask_ceiling" to "abc",
                "name" to "Sam",
                "unknown_key" to "7",
            ),
        )
        assertEquals(250, settings.rules.debtLimit)
        assertEquals(8, settings.rules.criticalOneIn)
        assertEquals(10, settings.rules.subtaskCeiling)
        assertEquals("Sam", settings.name)
        assertEquals(GameRules.Default.upgradeOneIn, settings.rules.upgradeOneIn)
    }

    @Test
    fun `a value equal to the default reads as the default`() {
        assertFalse(accepted(SettingKey.DebtLimit, "500").text(SettingKey.DebtLimit) != defaults.text(SettingKey.DebtLimit))
        assertNull(SettingKey.DebtLimit.problemWith("500"))
    }
}
