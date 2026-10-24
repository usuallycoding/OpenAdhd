package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.BackupJson
import com.usuallycoding.openadhd.domain.BackupResult
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.LedgerEntry
import com.usuallycoding.openadhd.domain.LedgerReason
import com.usuallycoding.openadhd.domain.PlannedSubtask
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.Redemption
import com.usuallycoding.openadhd.domain.Reminder
import com.usuallycoding.openadhd.domain.RedemptionSource
import com.usuallycoding.openadhd.domain.RewardTemplate
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.StoreState
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class BackupJsonTest {
    private val at = Instant.parse("2026-10-24T10:00:00Z")
    private val day = LocalDate(2026, 10, 24)

    private val full = StoreState(
        templates = listOf(
            TaskTemplate("t1", "Clean", Tier.Three, Energy.Low, "Whole flat", listOf("Dishes", "Floor")),
            TaskTemplate("t2", "Taxes", Tier.Four, Energy.High),
        ),
        planned = listOf(
            PlannedTask(
                "p1", day, "Clean", Tier.Three, Energy.Low, "t1", null, "Whole flat", 20,
                listOf(PlannedSubtask("s1", "Dishes", 10, at), PlannedSubtask("s2", "Floor", 10)),
                Reminder(Instant.parse("2026-10-24T15:00:00Z"), 30),
            ),
            PlannedTask("p2", day, "Taxes", Tier.Four, Energy.High, null, at, reward = 40),
        ),
        ledger = listOf(
            LedgerEntry("l1", 10, at, LedgerReason.SubtaskCompleted, "s1"),
            LedgerEntry("l2", 10, at, LedgerReason.CriticalHit, "s1"),
            LedgerEntry("l3", -20, at, LedgerReason.RewardRedeemed, "r1"),
        ),
        rewards = listOf(
            RewardTemplate("w1", "Tea", RewardTier.One, Energy.Low, "Green"),
            RewardTemplate("w2", "Weekend", RewardTier.Five, Energy.High, "Away", 800),
        ),
        redemptions = listOf(
            Redemption("r1", "w1", "Tea", RewardTier.One, Energy.Low, 20, at, RedemptionSource.Purchase, null, "Green"),
            Redemption("r2", "w2", "Weekend", RewardTier.Two, Energy.High, 50, at, RedemptionSource.Roll, RewardTier.One, "Away"),
        ),
        overrides = mapOf("debt_limit" to "300", "name" to "Sam"),
    )

    private fun valid(text: String) = assertIs<BackupResult.Valid>(BackupJson.decode(text))

    private fun refusal(text: String) = assertIs<BackupResult.Invalid>(BackupJson.decode(text)).message

    /** The encoded full state with [from] replaced by [to], to make a file with one wrong value. */
    private fun tampered(from: String, to: String): String {
        val text = BackupJson.encode(full, at)
        assertTrue(text.contains(from), "the file should contain $from")
        return text.replace(from, to)
    }

    @Test
    fun `a full state survives an export and an import`() {
        val decoded = valid(BackupJson.encode(full, at))
        assertEquals(full, decoded.state)
        assertEquals(at, decoded.exportedAt)
    }

    @Test
    fun `an empty state round trips`() {
        assertEquals(StoreState(), valid(BackupJson.encode(StoreState(), at)).state)
    }

    @Test
    fun `the file names itself and carries the format version`() {
        val text = BackupJson.encode(full, at)
        assertTrue(text.contains("\"app\": \"openadhd-backup\""))
        assertTrue(text.contains("\"formatVersion\": 1"))
    }

    @Test
    fun `only changed settings are written`() {
        val text = BackupJson.encode(StoreState(overrides = mapOf("debt_limit" to "300")), at)
        assertTrue(text.contains("debt_limit"))
        assertFalse(text.contains("subtask_ceiling"))
        assertEquals(mapOf("debt_limit" to "300"), valid(text).state.overrides)
    }

    @Test
    fun `lists keep their order`() {
        val reversed = full.copy(templates = full.templates.reversed(), rewards = full.rewards.reversed())
        val decoded = valid(BackupJson.encode(reversed, at)).state
        assertEquals(listOf("t2", "t1"), decoded.templates.map { it.id })
        assertEquals(listOf("w2", "w1"), decoded.rewards.map { it.id })
    }

    @Test
    fun `unknown extra fields and unknown or invalid settings are ignored`() {
        val text = BackupJson.encode(StoreState(), at).replace(
            "\"settings\": {}",
            "\"settings\": {\"unknown_key\": \"7\", \"subtask_ceiling\": \"abc\", \"debt_limit\": \"250\"}, \"future\": [1, 2]",
        )
        assertEquals(mapOf("debt_limit" to "250"), valid(text).state.overrides)
    }

    @Test
    fun `text that is not json is a damaged file`() {
        assertEquals("The file could not be read. It may be damaged.", refusal("this is not json"))
        assertEquals("The file could not be read. It may be damaged.", refusal(""))
    }

    @Test
    fun `a cut off file is a damaged file`() {
        val text = BackupJson.encode(full, at)
        assertEquals("The file could not be read. It may be damaged.", refusal(text.take(text.length / 2)))
    }

    @Test
    fun `json that is not a backup is refused`() {
        assertEquals("This is not an OpenAdhd backup.", refusal("{\"hello\": \"world\"}"))
        assertEquals("This is not an OpenAdhd backup.", refusal("[1, 2, 3]"))
        assertEquals("This is not an OpenAdhd backup.", refusal("{\"app\": \"other\", \"formatVersion\": 1}"))
        assertEquals("This is not an OpenAdhd backup.", refusal("{\"app\": \"openadhd-backup\"}"))
    }

    @Test
    fun `a newer format is refused with its own message`() {
        val message = refusal(tampered("\"formatVersion\": 1", "\"formatVersion\": 2"))
        assertEquals("This backup was made by a newer version of OpenAdhd. Update the app to import it.", message)
    }

    @Test
    fun `a newer format is refused even when its shape is different`() {
        val message = refusal("{\"app\": \"openadhd-backup\", \"formatVersion\": 9, \"templates\": \"changed\"}")
        assertTrue(message.contains("newer version"))
    }

    @Test
    fun `a missing required field is a damaged file`() {
        assertEquals("The file could not be read. It may be damaged.", refusal(tampered("\"name\": \"Clean\",", "")))
    }

    @Test
    fun `an unknown energy level is refused and says which kind of value`() {
        assertEquals("A task template has an unknown energy level (Extreme).", refusal(tampered("\"energy\": \"Low\",\n            \"description\": \"Whole flat\"", "\"energy\": \"Extreme\",\n            \"description\": \"Whole flat\"")))
    }

    @Test
    fun `a tier outside the range is refused`() {
        val text = BackupJson.encode(full.copy(templates = listOf(TaskTemplate("t", "X", Tier.One, Energy.Low))), at)
        assertEquals("A task template has a tier outside 1 to 4 (7).", refusal(text.replace("\"tier\": 1", "\"tier\": 7")))
        val rewards = BackupJson.encode(StoreState(rewards = listOf(RewardTemplate("w", "X", RewardTier.One, Energy.Low))), at)
        assertEquals("A reward has a tier outside 1 to 5 (6).", refusal(rewards.replace("\"tier\": 1", "\"tier\": 6")))
    }

    @Test
    fun `a tier five reward needs a price and the others must not have one`() {
        val five = BackupJson.encode(StoreState(rewards = listOf(RewardTemplate("w", "X", RewardTier.Five, Energy.Low, customPrice = 500))), at)
        assertEquals("A tier 5 reward has no price.", refusal(five.replace("\"customPrice\": 500", "\"customPrice\": null")))
        val one = BackupJson.encode(StoreState(rewards = listOf(RewardTemplate("w", "X", RewardTier.One, Energy.Low))), at)
        assertEquals("A reward below tier 5 has a custom price.", refusal(one.replace("\"customPrice\": null", "\"customPrice\": 30")))
    }

    @Test
    fun `duplicate ids are refused`() {
        val twice = full.copy(templates = listOf(full.templates[0], full.templates[0]))
        assertEquals("Two of the task templates have the same id.", refusal(BackupJson.encode(twice, at)))
        val sub = full.copy(
            planned = listOf(
                full.planned[0].copy(subtasks = listOf(PlannedSubtask("same", "A", 10), PlannedSubtask("same", "B", 10))),
            ),
        )
        assertEquals("Two of the subtasks have the same id.", refusal(BackupJson.encode(sub, at)))
        val ledger = full.copy(ledger = listOf(full.ledger[0], full.ledger[0]))
        assertEquals("Two of the coin ledger entries have the same id.", refusal(BackupJson.encode(ledger, at)))
    }

    @Test
    fun `a date that is not a date is refused`() {
        val message = refusal(tampered("\"date\": \"2026-10-24\"", "\"date\": \"yesterday\""))
        assertEquals("A Today entry has a date that is not a date (yesterday).", message)
    }

    @Test
    fun `an unknown ledger reason is refused`() {
        val message = refusal(tampered("\"reason\": \"CriticalHit\"", "\"reason\": \"Gift\""))
        assertEquals("A coin ledger entry has an unknown reason (Gift).", message)
    }

    @Test
    fun `a blank name is refused`() {
        val text = BackupJson.encode(StoreState(templates = listOf(TaskTemplate("t", "Named", Tier.One, Energy.Low))), at)
        assertEquals("A task template has no name.", refusal(text.replace("\"name\": \"Named\"", "\"name\": \"  \"")))
    }

    @Test
    fun `a valid file has no refusal message`() {
        assertNull((BackupJson.decode(BackupJson.encode(full, at)) as? BackupResult.Invalid)?.message)
    }

    @Test
    fun `reminders round trip with and without a repeat`() {
        val noRepeat = full.copy(planned = listOf(full.planned[0].copy(reminder = Reminder(at, null)), full.planned[1]))
        val decoded = valid(BackupJson.encode(noRepeat, at)).state
        assertEquals(Reminder(at, null), decoded.planned[0].reminder)
        assertNull(decoded.planned[1].reminder)
        assertEquals(Reminder(Instant.parse("2026-10-24T15:00:00Z"), 30), valid(BackupJson.encode(full, at)).state.planned[0].reminder)
    }

    @Test
    fun `a file written before reminders existed still imports`() {
        val root = Json.parseToJsonElement(BackupJson.encode(full, at)).jsonObject
        val planned = JsonArray(root.getValue("planned").jsonArray.map { JsonObject(it.jsonObject - "reminderAt" - "reminderRepeatMinutes") })
        val text = JsonObject(root + ("planned" to planned)).toString()
        assertFalse(text.contains("reminderAt"))
        val decoded = valid(text).state
        assertNull(decoded.planned[0].reminder)
        assertEquals(full.planned[0].copy(reminder = null), decoded.planned[0])
    }

    @Test
    fun `a reminder that repeats at an invalid interval is refused`() {
        val message = refusal(tampered("\"reminderRepeatMinutes\": 30", "\"reminderRepeatMinutes\": 0"))
        assertEquals("A reminder repeats at an invalid interval (0).", message)
    }

    @Test
    fun `the theme round trips as a setting and an unknown one is ignored`() {
        val dark = StoreState(overrides = mapOf("theme" to "Dark"))
        assertEquals(mapOf("theme" to "Dark"), valid(BackupJson.encode(dark, at)).state.overrides)

        val text = BackupJson.encode(StoreState(), at).replace("\"settings\": {}", "\"settings\": {\"theme\": \"Neon\"}")
        assertEquals(emptyMap(), valid(text).state.overrides)
    }
}
