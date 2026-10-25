package com.usuallycoding.openadhd.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlin.time.Instant

/** The outcome of reading a backup: the data it holds, or the reason it was refused. */
sealed interface BackupResult {
    data class Valid(val state: StoreState, val exportedAt: Instant?) : BackupResult
    data class Invalid(val message: String) : BackupResult
}

/**
 * The backup file: one JSON document holding a whole [StoreState]. It names itself and carries a format
 * version, so a later version of the app can still read it, and it is checked completely before any data is
 * replaced. Unknown extra fields are ignored so that a later format can add some.
 */
object BackupJson {
    const val AppMarker = "openadhd-backup"
    const val CurrentVersion = 1

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(state: StoreState, exportedAt: Instant): String =
        json.encodeToString(BackupFile.serializer(), BackupFile.from(state, exportedAt))

    fun decode(text: String): BackupResult {
        val root = try {
            json.parseToJsonElement(text) as? JsonObject
        } catch (_: Exception) {
            return Invalid("The file could not be read. It may be damaged.")
        } ?: return Invalid(NotABackup)
        if ((root["app"] as? JsonPrimitive)?.content != AppMarker) return Invalid(NotABackup)
        val version = try {
            (root["formatVersion"] as? JsonPrimitive)?.int
        } catch (_: Exception) {
            null
        } ?: return Invalid(NotABackup)
        if (version > CurrentVersion) {
            return Invalid("This backup was made by a newer version of OpenAdhd. Update the app to import it.")
        }
        if (version < 1) return Invalid(NotABackup)
        val file = try {
            json.decodeFromJsonElement(BackupFile.serializer(), root)
        } catch (_: Exception) {
            return Invalid("The file could not be read. It may be damaged.")
        }
        return try {
            BackupResult.Valid(file.toState(), file.exportedAt?.let { runCatching { Instant.parse(it) }.getOrNull() })
        } catch (problem: BackupProblem) {
            Invalid(problem.message.orEmpty())
        }
    }

    private fun Invalid(message: String) = BackupResult.Invalid(message)

    private const val NotABackup = "This is not an OpenAdhd backup."
}

private class BackupProblem(message: String) : Exception(message)

private fun refuse(message: String): Nothing = throw BackupProblem(message)

@Serializable
private class BackupFile(
    val app: String = "",
    val formatVersion: Int = 0,
    val exportedAt: String? = null,
    val templates: List<TemplateDto> = emptyList(),
    val planned: List<PlannedDto> = emptyList(),
    val ledger: List<LedgerDto> = emptyList(),
    val rewards: List<RewardDto> = emptyList(),
    val redemptions: List<RedemptionDto> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
) {
    fun toState(): StoreState {
        requireUnique("task templates", templates.map { it.id })
        requireUnique("Today entries", planned.map { it.id })
        requireUnique("subtasks", planned.flatMap { task -> task.subtasks.map { it.id } })
        requireUnique("coin ledger entries", ledger.map { it.id })
        requireUnique("rewards", rewards.map { it.id })
        requireUnique("redemptions", redemptions.map { it.id })
        return StoreState(
            templates = templates.map { it.toDomain() },
            planned = planned.map { it.toDomain() },
            ledger = ledger.map { it.toDomain() },
            rewards = rewards.map { it.toDomain() },
            redemptions = redemptions.map { it.toDomain() },
            overrides = Settings.fromOverrides(settings).overrides,
        )
    }

    companion object {
        fun from(state: StoreState, exportedAt: Instant) = BackupFile(
            app = BackupJson.AppMarker,
            formatVersion = BackupJson.CurrentVersion,
            exportedAt = exportedAt.toString(),
            templates = state.templates.map(TemplateDto::from),
            planned = state.planned.map(PlannedDto::from),
            ledger = state.ledger.map(LedgerDto::from),
            rewards = state.rewards.map(RewardDto::from),
            redemptions = state.redemptions.map(RedemptionDto::from),
            settings = state.settings.overrides,
        )
    }
}

private fun requireUnique(kinds: String, ids: List<String>) {
    if (ids.toSet().size != ids.size) refuse("Two of the $kinds have the same id.")
}

private fun energyOf(name: String, kind: String): Energy =
    Energy.entries.firstOrNull { it.name == name } ?: refuse("A $kind has an unknown energy level ($name).")

private fun taskTierOf(level: Int, kind: String): Tier =
    Tier.entries.firstOrNull { it.level == level } ?: refuse("A $kind has a tier outside 1 to 4 ($level).")

private fun rewardTierOf(level: Int, kind: String): RewardTier =
    RewardTier.entries.firstOrNull { it.level == level } ?: refuse("A $kind has a tier outside 1 to 5 ($level).")

private fun instantOf(millis: Long): Instant = Instant.fromEpochMilliseconds(millis)

@Serializable
private class TemplateDto(
    val id: String,
    val name: String,
    val tier: Int,
    val energy: String,
    val description: String = "",
    val subtasks: List<String> = emptyList(),
    val schedule: String? = null,
    val scheduleStart: String? = null,
    val scheduleEnd: String? = null,
    val scheduleDays: List<String> = emptyList(),
    val reminderMinuteOfDay: Int? = null,
    val reminderRepeatMinutes: Int? = null,
    val lastGenerated: String? = null,
) {
    fun toDomain(): TaskTemplate {
        if (name.isBlank()) refuse("A task template has no name.")
        val schedule = scheduleOf()
        val reminder = reminderMinuteOfDay?.let { TemplateReminder(it, reminderRepeatMinutes) }
        if (ScheduleRules.check(schedule, reminder) != null) refuse("A task template has an invalid schedule or reminder.")
        val generated = lastGenerated?.let { dateOf(it, "a task template's last generated day") }
        return TaskTemplate(
            id, name, taskTierOf(tier, "task template"), energyOf(energy, "task template"), description, subtasks,
            schedule, reminder, generated,
        )
    }

    private fun scheduleOf(): TaskSchedule = when (schedule) {
        null, "None" -> TaskSchedule.None
        "Date" -> TaskSchedule.OnDate(dateOf(scheduleStart, "a task template's schedule"))
        "Weekly" -> TaskSchedule.Weekly(
            scheduleDays.map { name -> ScheduleRules.Weekdays.firstOrNull { it.name == name } ?: refuse("A task template has an unknown weekday ($name).") }.toSet(),
        )
        "Period" -> TaskSchedule.Period(dateOf(scheduleStart, "a task template's schedule"), dateOf(scheduleEnd, "a task template's schedule"))
        "Daily" -> TaskSchedule.Daily
        else -> refuse("A task template has an unknown schedule ($schedule).")
    }

    private fun dateOf(text: String?, what: String): LocalDate =
        text?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: refuse("The date of $what is not a date (${text.orEmpty()}).")

    companion object {
        fun from(t: TaskTemplate): TemplateDto {
            val stored = ScheduleRules.encode(t.schedule)
            return TemplateDto(
                t.id, t.name, t.tier.level, t.energy.name, t.description, t.subtasks,
                schedule = stored.kind, scheduleStart = stored.start, scheduleEnd = stored.end,
                scheduleDays = (t.schedule as? TaskSchedule.Weekly)?.let { w -> ScheduleRules.Weekdays.filter { it in w.days }.map { it.name } }.orEmpty(),
                reminderMinuteOfDay = t.reminder?.minuteOfDay, reminderRepeatMinutes = t.reminder?.repeatMinutes,
                lastGenerated = t.lastGenerated?.toString(),
            )
        }
    }
}

@Serializable
private class PlannedSubtaskDto(val id: String, val title: String, val reward: Int, val completedAt: Long? = null)

@Serializable
private class PlannedDto(
    val id: String,
    val date: String,
    val name: String,
    val tier: Int,
    val energy: String,
    val templateId: String? = null,
    val completedAt: Long? = null,
    val description: String = "",
    val reward: Int,
    val subtasks: List<PlannedSubtaskDto> = emptyList(),
    val reminderAt: Long? = null,
    val reminderRepeatMinutes: Int? = null,
) {
    fun toDomain(): PlannedTask {
        val day = try {
            LocalDate.parse(date)
        } catch (_: Exception) {
            refuse("A Today entry has a date that is not a date ($date).")
        }
        if (reward < 0 || subtasks.any { it.reward < 0 }) refuse("A Today entry has a negative amount of coins.")
        if (reminderRepeatMinutes != null && reminderRepeatMinutes <= 0) refuse("A reminder repeats at an invalid interval ($reminderRepeatMinutes).")
        return PlannedTask(
            id = id,
            date = day,
            name = name,
            tier = taskTierOf(tier, "Today entry"),
            energy = energyOf(energy, "Today entry"),
            templateId = templateId,
            completedAt = completedAt?.let(::instantOf),
            description = description,
            reward = reward,
            subtasks = subtasks.map { PlannedSubtask(it.id, it.title, it.reward, it.completedAt?.let(::instantOf)) },
            reminder = reminderAt?.let { Reminder(instantOf(it), reminderRepeatMinutes) },
        )
    }

    companion object {
        fun from(t: PlannedTask) = PlannedDto(
            t.id, t.date.toString(), t.name, t.tier.level, t.energy.name, t.templateId,
            t.completedAt?.toEpochMilliseconds(), t.description, t.reward,
            t.subtasks.map { PlannedSubtaskDto(it.id, it.title, it.reward, it.completedAt?.toEpochMilliseconds()) },
            t.reminder?.startAt?.toEpochMilliseconds(), t.reminder?.repeatMinutes,
        )
    }
}

@Serializable
private class LedgerDto(val id: String, val amount: Int, val timestamp: Long, val reason: String, val refId: String? = null) {
    fun toDomain(): LedgerEntry {
        val why = LedgerReason.entries.firstOrNull { it.name == reason } ?: refuse("A coin ledger entry has an unknown reason ($reason).")
        return LedgerEntry(id, amount, instantOf(timestamp), why, refId)
    }

    companion object {
        fun from(e: LedgerEntry) = LedgerDto(e.id, e.amount, e.timestamp.toEpochMilliseconds(), e.reason.name, e.refId)
    }
}

@Serializable
private class RewardDto(
    val id: String,
    val name: String,
    val tier: Int,
    val energy: String,
    val description: String = "",
    val customPrice: Int? = null,
) {
    fun toDomain(): RewardTemplate {
        if (name.isBlank()) refuse("A reward has no name.")
        val rewardTier = rewardTierOf(tier, "reward")
        if (rewardTier == RewardTier.Five && customPrice == null) refuse("A tier 5 reward has no price.")
        if (rewardTier != RewardTier.Five && customPrice != null) refuse("A reward below tier 5 has a custom price.")
        return RewardTemplate(id, name, rewardTier, energyOf(energy, "reward"), description, customPrice)
    }

    companion object {
        fun from(r: RewardTemplate) = RewardDto(r.id, r.name, r.tier.level, r.energy.name, r.description, r.customPrice)
    }
}

@Serializable
private class RedemptionDto(
    val id: String,
    val templateId: String? = null,
    val name: String,
    val tier: Int,
    val energy: String,
    val price: Int,
    val redeemedAt: Long,
    val source: String = RedemptionSource.Purchase.name,
    val rolledTier: Int? = null,
    val description: String = "",
) {
    fun toDomain(): Redemption {
        val how = RedemptionSource.entries.firstOrNull { it.name == source } ?: refuse("A redemption has an unknown source ($source).")
        return Redemption(
            id, templateId, name, rewardTierOf(tier, "redemption"), energyOf(energy, "redemption"), price,
            instantOf(redeemedAt), how, rolledTier?.let { rewardTierOf(it, "redemption") }, description,
        )
    }

    companion object {
        fun from(r: Redemption) = RedemptionDto(
            r.id, r.templateId, r.name, r.tier.level, r.energy.name, r.price, r.redeemedAt.toEpochMilliseconds(),
            r.source.name, r.rolledTier?.level, r.description,
        )
    }
}
