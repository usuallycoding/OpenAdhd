package com.usuallycoding.openadhd.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface TemplateRepository {
    val templates: Flow<List<TaskTemplate>>

    /** @throws IllegalArgumentException if [name] is blank. */
    suspend fun create(name: String, tier: Tier, energy: Energy): TaskTemplate

    /** @throws IllegalArgumentException if the new name is blank. */
    suspend fun update(template: TaskTemplate)

    suspend fun delete(id: String)
}

interface PlannerRepository {
    fun entriesFor(date: LocalDate): Flow<List<PlannedTask>>

    suspend fun assign(template: TaskTemplate, date: LocalDate): PlannedTask

    /** Removes a pending entry; completed entries are left alone. */
    suspend fun remove(id: String)

    /**
     * Completes a pending entry and appends its ledger entry as one operation.
     * Returns false, changing nothing, if the entry is unknown or already completed.
     */
    suspend fun complete(id: String): Boolean
}

interface LedgerRepository {
    val entries: Flow<List<LedgerEntry>>
}

/** All repositories behind one object, so completing a task can update entry and ledger together. */
interface OpenAdhdStore : TemplateRepository, PlannerRepository, LedgerRepository
