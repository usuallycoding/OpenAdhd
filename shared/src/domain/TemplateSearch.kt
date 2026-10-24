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
