package com.usuallycoding.openadhd.domain

/** The outcome of checking a value for a setting: the settings it would give, or why it is refused. */
sealed interface SettingCheck {
    data class Accepted(val settings: Settings) : SettingCheck
    data class Refused(val message: String) : SettingCheck
}

/**
 * Checks [text] as the new value of [key] against the [current] settings and the data that depends on them.
 * Refuses a value that is out of range, that would put the tiers out of order, or that would leave existing
 * rewards or templates invalid, and names the item that stops it.
 */
fun checkSetting(
    key: SettingKey,
    text: String,
    current: Settings,
    rewards: List<RewardTemplate>,
    templates: List<TaskTemplate>,
): SettingCheck {
    key.problemWith(text)?.let { return SettingCheck.Refused(it) }
    val value = text.trim()
    val next = current.with(key, value)
    orderProblem(key, next.rules)?.let { return SettingCheck.Refused(it) }
    dataProblem(key, next.rules, rewards, templates)?.let { return SettingCheck.Refused(it) }
    return SettingCheck.Accepted(next)
}

private val PayoutKeys = listOf(SettingKey.PayoutTier1, SettingKey.PayoutTier2, SettingKey.PayoutTier3, SettingKey.PayoutTier4)
private val PriceKeys = listOf(SettingKey.PriceTier1, SettingKey.PriceTier2, SettingKey.PriceTier3, SettingKey.PriceTier4)

/** Each tier must pay more, and cost more, than the tier below it. */
private fun orderProblem(key: SettingKey, rules: GameRules): String? {
    val payoutIndex = PayoutKeys.indexOf(key)
    if (payoutIndex >= 0) return tierOrderProblem(payoutIndex, rules.taskPayouts, "pay")
    val priceIndex = PriceKeys.indexOf(key)
    if (priceIndex >= 0) return tierOrderProblem(priceIndex, rules.rewardPrices, "cost")
    return null
}

private fun tierOrderProblem(index: Int, values: List<Int>, verb: String): String? {
    if (index > 0 && values[index] <= values[index - 1]) {
        return "Tier ${index + 1} must $verb more than tier $index"
    }
    if (index < values.lastIndex && values[index] >= values[index + 1]) {
        return "Tier ${index + 1} must $verb less than tier ${index + 2}"
    }
    return null
}

/** A change must not leave a tier 5 reward cheaper than tier 4, or a template with more subtasks than its tier pays. */
private fun dataProblem(key: SettingKey, rules: GameRules, rewards: List<RewardTemplate>, templates: List<TaskTemplate>): String? {
    if (key == SettingKey.PriceTier4) {
        val blocking = rewards
            .filter { it.tier == RewardTier.Five && (it.customPrice ?: Int.MAX_VALUE) <= rules.rewardPrices[3] }
            .minByOrNull { it.customPrice ?: Int.MAX_VALUE }
        if (blocking != null) return "Tier 4 must cost less than \"${blocking.name}\", which costs ${blocking.customPrice}"
    }
    val payoutIndex = PayoutKeys.indexOf(key)
    if (payoutIndex >= 0) {
        val pool = rules.taskPayouts[payoutIndex]
        val blocking = templates
            .filter { it.tier.level == payoutIndex + 1 && it.subtasks.size > pool }
            .maxByOrNull { it.subtasks.size }
        if (blocking != null) {
            return "\"${blocking.name}\" has ${blocking.subtasks.size} subtasks and needs at least ${blocking.subtasks.size} coins"
        }
    }
    return null
}
