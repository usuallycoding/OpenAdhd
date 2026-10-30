package com.usuallycoding.openadhd

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.usuallycoding.openadhd.data.createDesktopContainer
import java.awt.Taskbar
import javax.imageio.ImageIO

private const val IconResource = "app-icon.png"

/** The Dock (or taskbar) icon, which the window's own icon does not set on macOS. A system without one keeps its default. */
private fun setTaskbarIcon() {
    runCatching {
        if (!Taskbar.isTaskbarSupported()) return
        val taskbar = Taskbar.getTaskbar()
        if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) return
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(IconResource) ?: return
        taskbar.iconImage = stream.use { ImageIO.read(it) }
    }
}

fun main() = application {
    val container = createDesktopContainer()
    setTaskbarIcon()
    Window(onCloseRequest = ::exitApplication, title = "OpenAdhd", icon = painterResource(IconResource)) {
        App(container)
    }
}
