package com.usuallycoding.openadhd.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/**
 * Adds the entries of scheduled templates to Today. It generates when it starts, whenever [today] changes and
 * whenever the templates change, which covers the date rolling over while the app runs and a template created or
 * edited so that it is due today. The store remembers what it generated, so running it again adds nothing.
 */
class ScheduledEntries(
    private val store: OpenAdhdStore,
    private val today: Flow<LocalDate>,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) {
    /** Runs until cancelled. */
    suspend fun run() {
        combine(today, store.templates) { date, _ -> date }.collect { store.generateDue(it, zone) }
    }
}
