package com.usuallycoding.openadhd.domain

/**
 * The whole persisted state: the unit a store reads and writes, and what a backup holds. Every list is in the
 * order the app shows it (Today entries in their order within each day, redemptions and ledger oldest first).
 */
data class StoreState(
    val templates: List<TaskTemplate> = emptyList(),
    val planned: List<PlannedTask> = emptyList(),
    val ledger: List<LedgerEntry> = emptyList(),
    val rewards: List<RewardTemplate> = emptyList(),
    val redemptions: List<Redemption> = emptyList(),
    /** The settings the user changed: storage key to text. Anything missing has its default. */
    val overrides: Map<String, String> = emptyMap(),
) {
    val settings: Settings get() = Settings.fromOverrides(overrides)
    val rules: GameRules get() = settings.rules
}
