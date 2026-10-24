package com.usuallycoding.openadhd

import androidx.room.Room
import com.usuallycoding.openadhd.data.AppContainer
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.data.createContainer
import com.usuallycoding.openadhd.data.room.OpenAdhdDatabase
import com.usuallycoding.openadhd.data.room.RoomStore
import com.usuallycoding.openadhd.data.room.buildOpenAdhdDatabase
import com.usuallycoding.openadhd.domain.SettingKey
import com.usuallycoding.openadhd.domain.Settings
import com.usuallycoding.openadhd.domain.ThemeMode
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
/** The container reads the saved settings before any screen exists, so the first frame already uses them. */
class InitialSettingsTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a new container starts with the default settings`() {
        assertEquals(Settings.Default, AppContainer(InMemoryStore()).initialSettings)
    }

    @Test
    fun `a container over saved settings has them from the start`() = runTest {
        val store = InMemoryStore()
        assertNull(store.saveSetting(SettingKey.Theme, "Dark"))
        assertNull(store.saveSetting(SettingKey.Name, "Sam"))

        val initial = AppContainer(store).initialSettings

        assertEquals(ThemeMode.Dark, initial.themeMode)
        assertEquals("Sam's OpenAdhd", initial.title)
    }

    @Test
    fun `the real container reads the saved theme from the database at start`() = runTest {
        val file = createTempDirectory().resolve("initial.db").toString()
        val firstDb = Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase()
        assertNull(RoomStore(firstDb).saveSetting(SettingKey.Theme, "Light"))
        firstDb.close()

        val secondDb = Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase()
        val container = createContainer(secondDb)
        assertEquals(ThemeMode.Light, container.initialSettings.themeMode)
        secondDb.close()
    }

    private fun androidx.compose.ui.test.ComposeUiTest.backgroundLuminance(): Float {
        val image = onRoot().captureToImage().toPixelMap()
        // the middle of the empty Today screen is the plain background
        return image[image.width / 2, image.height / 3].luminance()
    }

    @Test
    fun `the first frame is already dark when the saved theme is dark`() = runComposeUiTest {
        val store = InMemoryStore().also { runBlocking { it.saveSetting(SettingKey.Theme, "Dark") } }
        setContent { App(AppContainer(store, dateCheckInterval = null)) }
        assertTrue(backgroundLuminance() < 0.3f, "expected a dark background")
    }

    @Test
    fun `the first frame is already light when the saved theme is light`() = runComposeUiTest {
        val store = InMemoryStore().also { runBlocking { it.saveSetting(SettingKey.Theme, "Light") } }
        setContent { App(AppContainer(store, dateCheckInterval = null)) }
        assertTrue(backgroundLuminance() > 0.7f, "expected a light background")
    }
}
