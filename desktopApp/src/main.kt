package com.usuallycoding.openadhd

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.usuallycoding.openadhd.data.createDesktopContainer

fun main() = application {
    val container = createDesktopContainer()
    Window(onCloseRequest = ::exitApplication, title = "OpenAdhd") {
        App(container)
    }
}
