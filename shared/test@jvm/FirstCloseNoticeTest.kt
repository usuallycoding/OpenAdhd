package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.data.FirstCloseNotice
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FirstCloseNoticeTest {
    private fun folder(): File = Files.createTempDirectory("openadhd-notice").toFile()

    @Test
    fun `the first close shows the notice and later closes do not`() {
        val directory = folder()
        val notice = FirstCloseNotice(directory)
        assertTrue(notice.shouldShow())
        assertFalse(notice.shouldShow())
        assertFalse(notice.shouldShow())
    }

    @Test
    fun `restarting the app does not show it again`() {
        val directory = folder()
        assertTrue(FirstCloseNotice(directory).shouldShow())
        assertFalse(FirstCloseNotice(directory).shouldShow())
    }

    @Test
    fun `a fresh folder shows it again`() {
        assertTrue(FirstCloseNotice(folder()).shouldShow())
        assertTrue(FirstCloseNotice(folder()).shouldShow())
    }

    @Test
    fun `a folder that does not exist yet is created`() {
        val directory = File(folder(), "nested")
        assertTrue(FirstCloseNotice(directory).shouldShow())
        assertTrue(directory.isDirectory)
    }
}
