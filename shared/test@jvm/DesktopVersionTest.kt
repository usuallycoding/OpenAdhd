package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.data.createDesktopContainer
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

/** The desktop app's version line comes from the version jpackage gives the packaged app. */
class DesktopVersionTest {
    private fun folder() = Files.createTempDirectory("openadhd-version").toFile()

    @Test
    fun `a packaged app shows its version without a build number`() {
        val container = createDesktopContainer(folder(), files = null, loginItem = null, version = "1.0.0")
        assertEquals("OpenAdhd 1.0.0", container.versionLabel)
    }

    @Test
    fun `an app run from the build tool has no version and reads dev`() {
        val container = createDesktopContainer(folder(), files = null, loginItem = null, version = null)
        assertEquals("OpenAdhd dev", container.versionLabel)
    }

    @Test
    fun `the version is read from the property jpackage sets when none is given`() {
        val before = System.getProperty("jpackage.app-version")
        try {
            System.setProperty("jpackage.app-version", "2.3.4")
            assertEquals("OpenAdhd 2.3.4", createDesktopContainer(folder(), files = null, loginItem = null).versionLabel)
            System.clearProperty("jpackage.app-version")
            assertEquals("OpenAdhd dev", createDesktopContainer(folder(), files = null, loginItem = null).versionLabel)
        } finally {
            if (before != null) System.setProperty("jpackage.app-version", before) else System.clearProperty("jpackage.app-version")
        }
    }
}
