package com.usuallycoding.openadhd.domain

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun newId(): String = Uuid.random().toString()

internal fun requireValidName(name: String): String {
    val trimmed = name.trim()
    require(trimmed.isNotEmpty()) { "Name is required" }
    return trimmed
}
