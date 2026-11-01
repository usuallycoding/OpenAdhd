package com.usuallycoding.openadhd.data

import com.usuallycoding.openadhd.domain.LoginItem
import java.io.File

private const val AgentLabel = "com.usuallycoding.openadhd"

/** The argument that makes the app start with only its tray icon showing. */
const val BackgroundArgument = "--background"

/**
 * Starts the packaged app at login on macOS with a launch agent: a property list in [agentsDirectory] that the system
 * runs at login. Enabled means the file exists and names this app's [launcher], so deleting it elsewhere, or moving
 * the app, reads as off. The file is written and removed directly, with nothing loaded into launchd, so no
 * permission is asked and it takes effect at the next login.
 */
class MacLoginItem(
    private val launcher: String,
    private val agentsDirectory: File,
) : LoginItem {
    private val agentFile: File get() = File(agentsDirectory, "$AgentLabel.plist")

    override fun isEnabled(): Boolean =
        runCatching { agentFile.isFile && agentFile.readText().contains("<string>${escape(launcher)}</string>") }
            .getOrDefault(false)

    override fun setEnabled(enabled: Boolean) {
        if (enabled) {
            agentsDirectory.mkdirs()
            agentFile.writeText(plist())
        } else {
            agentFile.delete()
        }
    }

    private fun plist(): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
        <plist version="1.0">
        <dict>
            <key>Label</key>
            <string>$AgentLabel</string>
            <key>ProgramArguments</key>
            <array>
                <string>${escape(launcher)}</string>
                <string>$BackgroundArgument</string>
            </array>
            <key>RunAtLoad</key>
            <true/>
            <key>LimitLoadToSessionType</key>
            <string>Aqua</string>
        </dict>
        </plist>
    """.trimIndent() + "\n"

    private fun escape(text: String) = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}

/**
 * The login item of the packaged macOS app, or null anywhere else: off macOS, or when the app was not started from the
 * packaged launcher (jpackage names it in `jpackage.app-path`), as when it runs from the build tool.
 */
fun createLoginItem(
    osName: String = System.getProperty("os.name").orEmpty(),
    launcher: String? = System.getProperty("jpackage.app-path"),
    agentsDirectory: File = File(System.getProperty("user.home"), "Library/LaunchAgents"),
): LoginItem? =
    if (osName.startsWith("Mac") && !launcher.isNullOrBlank()) MacLoginItem(launcher, agentsDirectory) else null
