package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.RewardInputError
import com.usuallycoding.openadhd.domain.RewardRules
import com.usuallycoding.openadhd.domain.RewardTemplate
import com.usuallycoding.openadhd.domain.RewardTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class RewardRulesTest {
    @Test
    fun `tiers one to four have fixed prices`() {
        assertEquals(20, RewardRules.price(RewardTier.One, null))
        assertEquals(50, RewardRules.price(RewardTier.Two, null))
        assertEquals(120, RewardRules.price(RewardTier.Three, null))
        assertEquals(300, RewardRules.price(RewardTier.Four, null))
    }

    @Test
    fun `a custom price is ignored below tier five`() {
        assertEquals(50, RewardRules.price(RewardTier.Two, 999))
    }

    @Test
    fun `tier five costs its custom price`() {
        assertEquals(500, RewardRules.price(RewardTier.Five, 500))
        assertNull(RewardRules.fixedPrice(RewardTier.Five))
    }

    @Test
    fun `tier five without a price cannot be priced`() {
        assertFailsWith<IllegalArgumentException> { RewardRules.price(RewardTier.Five, null) }
    }

    @Test
    fun `the tier five minimum is one above tier four`() {
        assertEquals(301, RewardRules.MinCustomPrice)
        assertEquals(RewardInputError.PriceTooLow, RewardRules.validate("Headphones", RewardTier.Five, 300))
        assertEquals(RewardInputError.PriceTooLow, RewardRules.validate("Headphones", RewardTier.Five, 1))
        assertNull(RewardRules.validate("Headphones", RewardTier.Five, 301))
    }

    @Test
    fun `tier five needs a price and every reward needs a name`() {
        assertEquals(RewardInputError.PriceRequired, RewardRules.validate("Headphones", RewardTier.Five, null))
        assertEquals(RewardInputError.NameRequired, RewardRules.validate("  ", RewardTier.One, null))
        assertEquals(RewardInputError.NameRequired, RewardRules.validate("", RewardTier.Five, 500))
        assertNull(RewardRules.validate("Movie night", RewardTier.Two, null))
    }

    @Test
    fun `a template exposes its price`() {
        assertEquals(120, RewardTemplate("1", "Dinner out", RewardTier.Three, Energy.High).price)
        assertEquals(800, RewardTemplate("2", "Headphones", RewardTier.Five, Energy.Low, customPrice = 800).price)
    }

    @Test
    fun `tier maps from its level`() {
        assertEquals(RewardTier.Five, RewardTier.fromLevel(5))
    }
}
