package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.domain.matching
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TemplateSearchTest {
    private fun template(name: String, description: String = "") =
        TaskTemplate(name, name, Tier.One, Energy.Low, description)

    private val laundry = template("Laundry")
    private val dishes = template("Dishes")
    private val taxes = template("Taxes", "Send the forms")
    private val reset = template("Reset the flat", "wash the dishes")
    private val all = listOf(laundry, dishes, taxes, reset)

    private fun names(query: String) = all.matching(query).map { it.name }

    @Test
    fun `a blank query lists every template in order`() {
        assertEquals(listOf("Laundry", "Dishes", "Taxes", "Reset the flat"), names(""))
        assertEquals(listOf("Laundry", "Dishes", "Taxes", "Reset the flat"), names("   "))
    }

    @Test
    fun `matches part of the name ignoring case`() {
        assertEquals(listOf("Dishes", "Reset the flat"), names("DIS"))
        assertEquals(listOf("Laundry"), names("laundry"))
        assertEquals(listOf("Laundry"), names("ndr"))
    }

    @Test
    fun `matches the description too`() {
        assertEquals(listOf("Taxes"), names("forms"))
        assertEquals(listOf("Dishes", "Reset the flat"), names("dishes"))
    }

    @Test
    fun `ignores spaces around the query but not inside it`() {
        assertEquals(listOf("Laundry"), names("  laundry  "))
        assertEquals(listOf("Reset the flat"), names("the flat"))
        assertTrue(names("the  flat").isEmpty())
    }

    @Test
    fun `keeps the given order and lists nothing when nothing matches`() {
        assertEquals(listOf("Laundry", "Taxes", "Reset the flat"), names("a"))
        assertTrue(names("xyz").isEmpty())
    }
}
