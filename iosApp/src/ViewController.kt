package com.usuallycoding.openadhd

import androidx.compose.ui.window.ComposeUIViewController
import com.usuallycoding.openadhd.data.createIosContainer

private val container by lazy { createIosContainer() }

fun ViewController() = ComposeUIViewController { App(container) }
