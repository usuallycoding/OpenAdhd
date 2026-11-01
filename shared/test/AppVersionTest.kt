package com.usuallycoding.openadhd

import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.AppVersion
import com.usuallycoding.openadhd.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppVersionTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a name and a build give both`() {
        assertEquals("OpenAdhd 1.0.0 (1)", AppVersion.label("1.0.0", "1"))
    }

    @Test
    fun `a name without a build gives only the name`() {
        assertEquals("OpenAdhd 1.0.0", AppVersion.label("1.0.0"))
        assertEquals("OpenAdhd 1.0.0", AppVersion.label("1.0.0", null))
    }

    @Test
    fun `a blank build is left out and not shown as empty brackets`() {
        assertEquals("OpenAdhd 1.0.0", AppVersion.label("1.0.0", ""))
        assertEquals("OpenAdhd 1.0.0", AppVersion.label("1.0.0", "  "))
    }

    @Test
    fun `a missing or blank name reads as dev with or without a build`() {
        assertEquals("OpenAdhd dev", AppVersion.label(null))
        assertEquals("OpenAdhd dev", AppVersion.label(""))
        assertEquals("OpenAdhd dev", AppVersion.label("   ", "1"))
        assertEquals(AppVersion.Unpackaged, AppVersion.label(null, null))
    }

    @Test
    fun `surrounding spaces are trimmed`() {
        assertEquals("OpenAdhd 1.2.3 (45)", AppVersion.label(" 1.2.3 ", " 45 "))
    }

    @Test
    fun `settings exposes the label it was given and dev by default`() = runTest {
        val given = SettingsViewModel(InMemoryStore(), versionLabel = "OpenAdhd 1.0.0 (1)")
        assertEquals("OpenAdhd 1.0.0 (1)", given.versionLabel)
        val none = SettingsViewModel(InMemoryStore())
        assertEquals("OpenAdhd dev", none.versionLabel)
        given.viewModelScope.cancel()
        none.viewModelScope.cancel()
    }
}
