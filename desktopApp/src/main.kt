package com.usuallycoding.openadhd

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberTrayState
import com.usuallycoding.openadhd.data.BackgroundArgument
import com.usuallycoding.openadhd.data.FirstCloseNotice
import com.usuallycoding.openadhd.data.createDesktopContainer
import com.usuallycoding.openadhd.data.defaultDesktopDirectory
import java.awt.Desktop
import java.awt.Taskbar
import java.awt.desktop.AppReopenedListener
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

fun main(args: Array<String>) = application {
    val trayState = rememberTrayState()
    // With a tray, closing the window only hides it and the app keeps delivering reminders until Quit in the tray menu.
    // Without one a hidden app could not be shown again, so closing quits as before.
    val hasTray = isTraySupported
    val container = remember {
        createDesktopContainer(
            tray = if (hasTray) { title, text -> trayState.sendNotification(Notification(title, text, Notification.Type.Info)) } else null,
        )
    }
    val firstClose = remember { FirstCloseNotice(defaultDesktopDirectory()) }
    var windowVisible by remember { mutableStateOf(!(hasTray && BackgroundArgument in args)) }

    LaunchedEffect(Unit) {
        setTaskbarIcon()
        // A click on the Dock icon while the window is hidden brings it back.
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_EVENT_REOPENED)) {
            Desktop.getDesktop().addAppEventListener(AppReopenedListener { windowVisible = true })
        }
    }

    if (hasTray) {
        Tray(
            icon = painterResource(IconResource),
            state = trayState,
            tooltip = "OpenAdhd",
            onAction = { windowVisible = true },
        ) {
            Item("Open", onClick = { windowVisible = true })
            Item("Quit", onClick = ::exitApplication)
        }
    }

    Window(
        onCloseRequest = {
            if (!hasTray) {
                exitApplication()
            } else {
                windowVisible = false
                if (firstClose.shouldShow()) {
                    trayState.sendNotification(
                        Notification(
                            "OpenAdhd is still running",
                            "It stays in the tray so reminders keep coming. Choose Quit in the tray menu to exit.",
                            Notification.Type.Info,
                        ),
                    )
                }
            }
        },
        visible = windowVisible,
        title = "OpenAdhd",
        icon = painterResource(IconResource),
    ) {
        LaunchedEffect(windowVisible) {
            if (windowVisible) {
                window.toFront()
                window.requestFocus()
            }
        }
        App(container)
    }
}
