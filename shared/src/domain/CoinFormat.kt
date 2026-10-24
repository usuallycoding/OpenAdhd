package com.usuallycoding.openadhd.domain

/**
 * A coin amount as the app displays it; see the coin-economy spec. Below 1000 it is shown as is; from there in
 * thousands ("k") and from 1,000,000 in millions ("M"), with one decimal until 100 of that unit and none after.
 * Amounts are truncated, never rounded up, and a zero decimal is dropped.
 */
fun formatCoins(amount: Int): String {
    val sign = if (amount < 0) "-" else ""
    val value = kotlin.math.abs(amount.toLong())
    return sign + when {
        value < 1_000 -> "$value"
        value < 100_000 -> tenths(value / 100, "k")
        value < 1_000_000 -> "${value / 1_000}k"
        value < 100_000_000 -> tenths(value / 100_000, "M")
        else -> "${value / 1_000_000}M"
    }
}

/** [tenths] counted in tenths of a unit: 11 is "1.1" and 10 is "1". */
private fun tenths(tenths: Long, unit: String): String =
    if (tenths % 10 == 0L) "${tenths / 10}$unit" else "${tenths / 10}.${tenths % 10}$unit"
