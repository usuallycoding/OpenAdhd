package com.usuallycoding.openadhd.domain

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

enum class Tier(val level: Int) {
    One(1), Two(2), Three(3), Four(4);

    companion object {
        fun fromLevel(level: Int): Tier = entries.first { it.level == level }
    }
}

enum class Energy { Low, Medium, High }

data class TaskTemplate(
    val id: String,
    val name: String,
    val tier: Tier,
    val energy: Energy,
)

/** A task assigned to a day. Name, tier and energy are copied from the template at assignment time. */
data class PlannedTask(
    val id: String,
    val date: LocalDate,
    val name: String,
    val tier: Tier,
    val energy: Energy,
    val templateId: String?,
    val completedAt: Instant? = null,
) {
    val isCompleted: Boolean get() = completedAt != null
}

enum class LedgerReason { TaskCompleted }

data class LedgerEntry(
    val id: String,
    val amount: Int,
    val timestamp: Instant,
    val reason: LedgerReason,
    val refId: String? = null,
)
