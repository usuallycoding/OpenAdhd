package com.usuallycoding.openadhd.domain

/**
 * The templates whose name or description contains [query], ignoring case and spaces around the query, in their
 * given order. A blank query matches every template.
 */
fun List<TaskTemplate>.matching(query: String): List<TaskTemplate> {
    val text = query.trim()
    if (text.isEmpty()) return this
    return filter { it.name.contains(text, ignoreCase = true) || it.description.contains(text, ignoreCase = true) }
}

/**
 * The templates matching [query] as above that also have [energy], in their given order. A null [energy] applies
 * no energy filter.
 */
fun List<TaskTemplate>.matching(query: String, energy: Energy?): List<TaskTemplate> =
    matching(query).let { found -> if (energy == null) found else found.filter { it.energy == energy } }

/**
 * The rewards whose name or description contains [query], by the same rule as the task templates' [matching]: ignoring
 * case and spaces around the query, in their given order, and every reward for a blank query. It has its own name
 * because two functions `matching(String)` on lists of different element types would clash after type erasure.
 */
fun List<RewardTemplate>.matchingRewards(query: String): List<RewardTemplate> {
    val text = query.trim()
    if (text.isEmpty()) return this
    return filter { it.name.contains(text, ignoreCase = true) || it.description.contains(text, ignoreCase = true) }
}
