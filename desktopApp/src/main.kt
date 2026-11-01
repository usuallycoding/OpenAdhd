package com.usuallycoding.openadhd

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import com.usuallycoding.openadhd.data.AlarmCard
import com.usuallycoding.openadhd.data.BackgroundArgument
import com.usuallycoding.openadhd.data.DesktopAlarmRinger
import com.usuallycoding.openadhd.data.FirstCloseNotice
import com.usuallycoding.openadhd.data.ToneAlarmSound
import com.usuallycoding.openadhd.data.createDesktopContainer
import com.usuallycoding.openadhd.data.defaultDesktopDirectory
import com.usuallycoding.openadhd.ui.theme.AppTheme
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

/** Bounces the Dock icon until the app is noticed (or [on] is false), where the system has such a thing; nothing elsewhere. */
private fun setDockAttention(on: Boolean) {
    runCatching {
        if (!Taskbar.isTaskbarSupported()) return
        val taskbar = Taskbar.getTaskbar()
        if (taskbar.isSupported(Taskbar.Feature.USER_ATTENTION)) taskbar.requestUserAttention(on, true)
    }
}

fun main(args: Array<String>) = application {
    val trayState = rememberTrayState()
    // With a tray, closing the window only hides it and the app keeps delivering reminders until Quit in the tray menu.
    // Without one a hidden app could not be shown again, so closing quits as before.
    val hasTray = isTraySupported
    val ringer = remember { DesktopAlarmRinger(ToneAlarmSound()) }
    val container = remember {
        createDesktopContainer(
            tray = if (hasTray) { title, text -> trayState.sendNotification(Notification(title, text, Notification.Type.Info)) } else null,
            ringer = ringer,
        )
    }
    val firstClose = remember { FirstCloseNotice(defaultDesktopDirectory()) }
    var windowVisible by remember { mutableStateOf(!(hasTray && BackgroundArgument in args)) }
    var frontRequests by remember { mutableIntStateOf(0) }

    // Shows the main window and brings it in front, even when it was already open behind other windows. It does not touch a ring.
    fun showMainWindow() {
        windowVisible = true
        frontRequests++
    }

    LaunchedEffect(Unit) {
        setTaskbarIcon()
        // A click on the Dock icon while the window is hidden brings it back.
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_EVENT_REOPENED)) {
            Desktop.getDesktop().addAppEventListener(AppReopenedListener { showMainWindow() })
        }
    }

    if (hasTray) {
        Tray(
            icon = painterResource(IconResource),
            state = trayState,
            tooltip = "OpenAdhd",
            onAction = ::showMainWindow,
        ) {
            Item("Open", onClick = ::showMainWindow)
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
        LaunchedEffect(windowVisible, frontRequests) {
            if (windowVisible) {
                window.toFront()
                window.requestFocus()
            }
        }
        App(container)
    }

    // While alarms ring: the card above all other windows, even with the main window hidden, and the Dock icon bouncing.
    val ringing by ringer.rows.collectAsState()
    val settings by container.store.settings.collectAsState(container.initialSettings)
    DisposableEffect(ringing.isNotEmpty()) {
        if (ringing.isNotEmpty()) setDockAttention(true)
        onDispose { if (ringing.isNotEmpty()) setDockAttention(false) }
    }
    if (ringing.isNotEmpty()) {
        Window(
            onCloseRequest = ringer::dismissAll,
            title = "OpenAdhd alarm",
            icon = painterResource(IconResource),
            state = rememberWindowState(position = WindowPosition(Alignment.Center), size = DpSize(440.dp, 380.dp)),
            resizable = false,
            alwaysOnTop = true,
            // So an alarm that appears while the user is typing in another app takes none of their keystrokes.
            focusable = false,
        ) {
            AppTheme(settings.themeMode, settings.dynamicColor) {
                AlarmCard(
                    rows = ringing,
                    onOpenTask = { entryId ->
                        container.entryRequests.open(entryId)
                        showMainWindow()
                        ringer.dismiss(entryId)
                    },
                    onDismiss = ringer::dismiss,
                )
            }
        }
    }
}
