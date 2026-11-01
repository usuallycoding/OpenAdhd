package com.usuallycoding.openadhd.domain

/**
 * Every tunable number of the game. The defaults are the original rules; a user can change any of them in
 * Settings. Payouts and prices are listed for tiers 1 to 4 in order.
 */
data class GameRules(
    /** How far below zero a purchase may take the balance. */
    val debtLimit: Int = 500,
    /** No task has more subtasks than this, whatever its tier. */
    val subtaskCeiling: Int = 10,
    /** The coin pool of a task of tier 1 to 4. */
    val taskPayouts: List<Int> = listOf(5, 10, 20, 40),
    /** The Shop price of a reward of tier 1 to 4. */
    val rewardPrices: List<Int> = listOf(20, 50, 120, 300),
    /** One completion in this many is a critical hit. */
    val criticalOneIn: Int = 8,
    /** A critical hit pays this many times the normal reward in total. */
    val criticalMultiplier: Int = 2,
    /** One roll in this many gives a reward from the next tier up. */
    val upgradeOneIn: Int = 6,
) {
    init {
        require(taskPayouts.size == Tier.entries.size) { "A payout is needed for every task tier" }
        require(rewardPrices.size == RewardTier.entries.size - 1) { "A price is needed for reward tiers 1 to 4" }
    }

    fun payout(tier: Tier): Int = taskPayouts[tier.level - 1]

    /** The fixed price of reward tiers 1 to 4, or null for tier 5 whose price is entered per reward. */
    fun price(tier: RewardTier): Int? = rewardPrices.getOrNull(tier.level - 1)

    companion object {
        val Default = GameRules()
    }
}

/** The user's settings: a name, and the [rules] of the game. */
data class Settings(
    val name: String = "",
    val rules: GameRules = GameRules.Default,
    /** The hour of the day, 1 to 23, at which reminders stop for the day. */
    val reminderStopHour: Int = 22,
    val themeMode: ThemeMode = ThemeMode.System,
    /** Whether Android 12+ takes the app's colors from the wallpaper; a platform without dynamic color ignores it. */
    val dynamicColor: Boolean = true,
) {
    /** The title of the top bar. */
    val title: String get() = if (name.isBlank()) "OpenAdhd" else "$name's OpenAdhd"

    /** The settings that differ from their defaults, as storage key to text: what is stored and what a backup holds. */
    val overrides: Map<String, String>
        get() = SettingKey.entries
            .filter { text(it) != Default.text(it) }
            .associate { it.storageKey to text(it) }

    /** The value of [key] as the text the Settings form shows. */
    fun text(key: SettingKey): String = when (key) {
        SettingKey.Name -> name
        SettingKey.DebtLimit -> rules.debtLimit.toString()
        SettingKey.SubtaskCeiling -> rules.subtaskCeiling.toString()
        SettingKey.PayoutTier1 -> rules.taskPayouts[0].toString()
        SettingKey.PayoutTier2 -> rules.taskPayouts[1].toString()
        SettingKey.PayoutTier3 -> rules.taskPayouts[2].toString()
        SettingKey.PayoutTier4 -> rules.taskPayouts[3].toString()
        SettingKey.PriceTier1 -> rules.rewardPrices[0].toString()
        SettingKey.PriceTier2 -> rules.rewardPrices[1].toString()
        SettingKey.PriceTier3 -> rules.rewardPrices[2].toString()
        SettingKey.PriceTier4 -> rules.rewardPrices[3].toString()
        SettingKey.CriticalOneIn -> rules.criticalOneIn.toString()
        SettingKey.CriticalMultiplier -> rules.criticalMultiplier.toString()
        SettingKey.UpgradeOneIn -> rules.upgradeOneIn.toString()
        SettingKey.ReminderStopHour -> reminderStopHour.toString()
        SettingKey.Theme -> themeMode.name
        SettingKey.DynamicColor -> if (dynamicColor) DynamicColorDynamic else DynamicColorApp
    }

    /** These settings with [key] set to [value], which must already be valid (a number for every key but the name). */
    fun with(key: SettingKey, value: String): Settings {
        if (key == SettingKey.Name) return copy(name = value)
        if (key == SettingKey.Theme) return copy(themeMode = ThemeMode.valueOf(value))
        if (key == SettingKey.DynamicColor) return copy(dynamicColor = value == DynamicColorDynamic)
        val number = value.toInt()
        if (key == SettingKey.ReminderStopHour) return copy(reminderStopHour = number)
        fun <T> List<T>.set(index: Int, item: T) = toMutableList().also { it[index] = item }
        return copy(
            rules = when (key) {
                SettingKey.DebtLimit -> rules.copy(debtLimit = number)
                SettingKey.SubtaskCeiling -> rules.copy(subtaskCeiling = number)
                SettingKey.PayoutTier1 -> rules.copy(taskPayouts = rules.taskPayouts.set(0, number))
                SettingKey.PayoutTier2 -> rules.copy(taskPayouts = rules.taskPayouts.set(1, number))
                SettingKey.PayoutTier3 -> rules.copy(taskPayouts = rules.taskPayouts.set(2, number))
                SettingKey.PayoutTier4 -> rules.copy(taskPayouts = rules.taskPayouts.set(3, number))
                SettingKey.PriceTier1 -> rules.copy(rewardPrices = rules.rewardPrices.set(0, number))
                SettingKey.PriceTier2 -> rules.copy(rewardPrices = rules.rewardPrices.set(1, number))
                SettingKey.PriceTier3 -> rules.copy(rewardPrices = rules.rewardPrices.set(2, number))
                SettingKey.PriceTier4 -> rules.copy(rewardPrices = rules.rewardPrices.set(3, number))
                SettingKey.CriticalOneIn -> rules.copy(criticalOneIn = number)
                SettingKey.CriticalMultiplier -> rules.copy(criticalMultiplier = number)
                SettingKey.UpgradeOneIn -> rules.copy(upgradeOneIn = number)
                SettingKey.Name, SettingKey.ReminderStopHour, SettingKey.Theme, SettingKey.DynamicColor -> rules
            },
        )
    }

    companion object {
        val Default = Settings()

        /**
         * The settings with the saved [overrides] (storage key to text) applied on top of the defaults. A stored
         * value that is no longer valid for its key is ignored, so a bad row can never break the app.
         */
        fun fromOverrides(overrides: Map<String, String>): Settings =
            SettingKey.entries.fold(Default) { settings, key ->
                val saved = overrides[key.storageKey] ?: return@fold settings
                if (key.problemWith(saved) == null) settings.with(key, saved.trim()) else settings
            }
    }
}

/** The stored values of the Dynamic color setting: colors from the wallpaper, or the app's own. */
const val DynamicColorDynamic = "dynamic"
const val DynamicColorApp = "app"

enum class SettingGroup(val label: String) {
    Appearance("Appearance"), Profile("Profile"), Coins("Coins"), Subtasks("Subtasks"), Shop("Shop"), Luck("Luck"), Reminders("Reminders")
}

/**
 * One editable setting. [storageKey] is the key of its row in the database. A setting is a number with a [range],
 * a choice among [choices] (stored as text), or, when it has neither, free text (the name).
 */
enum class SettingKey(
    val storageKey: String,
    val label: String,
    val group: SettingGroup,
    val range: IntRange?,
    val choices: List<String>? = null,
) {
    Theme("theme", "Theme", SettingGroup.Appearance, null, ThemeMode.entries.map { it.name }),
    DynamicColor("dynamic_color", "Dynamic color", SettingGroup.Appearance, null, listOf(DynamicColorDynamic, DynamicColorApp)),
    Name("name", "Your name", SettingGroup.Profile, null),
    DebtLimit("debt_limit", "Debt limit", SettingGroup.Coins, 0..100_000),
    PayoutTier1("task_payout_1", "Tier 1 task pays", SettingGroup.Coins, 1..1000),
    PayoutTier2("task_payout_2", "Tier 2 task pays", SettingGroup.Coins, 1..1000),
    PayoutTier3("task_payout_3", "Tier 3 task pays", SettingGroup.Coins, 1..1000),
    PayoutTier4("task_payout_4", "Tier 4 task pays", SettingGroup.Coins, 1..1000),
    SubtaskCeiling("subtask_ceiling", "Most subtasks per task", SettingGroup.Subtasks, 1..50),
    PriceTier1("reward_price_1", "Tier 1 reward price", SettingGroup.Shop, 1..100_000),
    PriceTier2("reward_price_2", "Tier 2 reward price", SettingGroup.Shop, 1..100_000),
    PriceTier3("reward_price_3", "Tier 3 reward price", SettingGroup.Shop, 1..100_000),
    PriceTier4("reward_price_4", "Tier 4 reward price", SettingGroup.Shop, 1..100_000),
    CriticalOneIn("critical_one_in", "Critical hit chance, 1 in", SettingGroup.Luck, 2..1000),
    CriticalMultiplier("critical_multiplier", "Critical hit multiplier", SettingGroup.Luck, 2..10),
    UpgradeOneIn("upgrade_one_in", "Roll upgrade chance, 1 in", SettingGroup.Luck, 2..1000),
    ReminderStopHour("reminder_stop_hour", "Stop reminders at", SettingGroup.Reminders, 1..23);

    val isChoice: Boolean get() = choices != null

    val isText: Boolean get() = range == null && choices == null

    /** [text], a stored value of this setting, as the user reads it: a choice shows its label, an empty name "Not set". */
    fun display(text: String): String = when (this) {
        Theme -> ThemeMode.entries.firstOrNull { it.name == text }?.label ?: text
        DynamicColor -> when (text) {
            DynamicColorDynamic -> "Dynamic (wallpaper colors)"
            DynamicColorApp -> "App colors"
            else -> text
        }
        else -> text.ifEmpty { "Not set" }
    }

    /** What is wrong with [text] on its own (not a number, not a choice, out of range, name too long), or null. */
    fun problemWith(text: String): String? {
        if (choices != null) return if (text.trim() in choices) null else "Choose one of the options"
        val range = range ?: return if (text.trim().length > MaxNameLength) "The name can be at most $MaxNameLength characters" else null
        val number = text.trim().toIntOrNull() ?: return "Enter a whole number"
        return if (number in range) null else "$label must be between ${range.first} and ${range.last}"
    }

    companion object {
        const val MaxNameLength = 40
    }
}
