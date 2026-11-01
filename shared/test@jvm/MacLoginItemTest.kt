package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.data.BackgroundArgument
import com.usuallycoding.openadhd.data.MacLoginItem
import com.usuallycoding.openadhd.data.createLoginItem
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MacLoginItemTest {
    private val launcher = "/Applications/OpenAdhd.app/Contents/MacOS/OpenAdhd"

    private fun agents(): File = Files.createTempDirectory("openadhd-agents").toFile()

    private fun plist(directory: File) = File(directory, "com.usuallycoding.openadhd.plist")

    @Test
    fun `it is off until turned on`() {
        assertFalse(MacLoginItem(launcher, agents()).isEnabled())
    }

    @Test
    fun `turning it on writes the agent and turning it off removes it`() {
        val directory = agents()
        val item = MacLoginItem(launcher, directory)

        item.setEnabled(true)
        assertTrue(plist(directory).isFile)
        assertTrue(item.isEnabled())
        val text = plist(directory).readText()
        assertTrue("<string>$launcher</string>" in text)
        assertTrue("<string>$BackgroundArgument</string>" in text)
        assertTrue("<key>RunAtLoad</key>" in text)

        item.setEnabled(false)
        assertFalse(plist(directory).exists())
        assertFalse(item.isEnabled())
    }

    @Test
    fun `turning it off when it is already off is harmless`() {
        val item = MacLoginItem(launcher, agents())
        item.setEnabled(false)
        assertFalse(item.isEnabled())
    }

    @Test
    fun `deleting the file elsewhere reads as off`() {
        val directory = agents()
        val item = MacLoginItem(launcher, directory)
        item.setEnabled(true)
        plist(directory).delete()
        assertFalse(item.isEnabled())
    }

    @Test
    fun `an agent that names another launcher reads as off until turned on again`() {
        val directory = agents()
        MacLoginItem("/old/place/OpenAdhd.app/Contents/MacOS/OpenAdhd", directory).setEnabled(true)
        val item = MacLoginItem(launcher, directory)
        assertFalse(item.isEnabled())

        item.setEnabled(true)
        assertTrue(item.isEnabled())
        assertFalse("/old/place" in plist(directory).readText())
    }

    @Test
    fun `a launcher path with special characters is escaped and still reads as on`() {
        val odd = "/Applications/Open & <Adhd>.app/Contents/MacOS/OpenAdhd"
        val item = MacLoginItem(odd, agents())
        item.setEnabled(true)
        assertTrue(item.isEnabled())
    }

    @Test
    fun `the item exists only for the packaged mac app`() {
        val directory = agents()
        assertNotNull(createLoginItem("Mac OS X", launcher, directory))
        assertNull(createLoginItem("Mac OS X", null, directory))
        assertNull(createLoginItem("Mac OS X", "  ", directory))
        assertNull(createLoginItem("Linux", launcher, directory))
        assertNull(createLoginItem("Windows 11", launcher, directory))
        assertEquals(false, plist(directory).exists())
    }
}
