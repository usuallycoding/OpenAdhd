package com.usuallycoding.openadhd

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import com.usuallycoding.openadhd.data.AppContainer
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.data.SaveResult
import com.usuallycoding.openadhd.domain.BackupJson
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.SettingKey
import com.usuallycoding.openadhd.domain.StoreState
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

/** The Data section of Settings: export, import and delete all data, with a fake file dialog. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class DataBackupUiTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun filled(): InMemoryStore = InMemoryStore().also {
        runBlocking {
            it.create("Clean", Tier.Two, Energy.Low)
            it.createReward("Tea", RewardTier.One, Energy.Low)
            it.saveSetting(SettingKey.Name, "Sam")
        }
    }

    private fun backupOf(build: suspend InMemoryStore.() -> Unit): String {
        val other = InMemoryStore()
        runBlocking { other.build() }
        return BackupJson.encode(runBlocking { other.exportData() }, Clock.System.now())
    }

    private fun ComposeUiTest.openData(store: InMemoryStore, files: FakeDataFiles?) {
        setContent { App(AppContainer(store, dateCheckInterval = null, files = files)) }
        onNodeWithText("Settings").performClick()
        scrollTo("Delete all data")
    }

    private fun ComposeUiTest.scrollTo(text: String) {
        onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }

    @Test
    fun `the data section shows export import and delete`() = runComposeUiTest {
        openData(filled(), FakeDataFiles())
        onNodeWithText("Data").assertExists()
        onNodeWithText("Export a backup").assertExists()
        onNodeWithText("Import a backup").assertExists()
        onNodeWithText("Delete all data").assertExists()
    }

    @Test
    fun `without a file dialog only delete is shown`() = runComposeUiTest {
        openData(filled(), null)
        onNodeWithText("Delete all data").assertExists()
        onNodeWithText("Export a backup").assertDoesNotExist()
        onNodeWithText("Import a backup").assertDoesNotExist()
    }

    @Test
    fun `exporting saves a file and says so`() = runComposeUiTest {
        val files = FakeDataFiles()
        openData(filled(), files)
        onNodeWithText("Export a backup").performClick()

        onNodeWithText("Backup saved").assertExists()
        assertEquals(1, files.saved.size)
        assertTrue(files.saved.single().first.startsWith("openadhd-") && files.saved.single().first.endsWith(".json"))
        onNodeWithText("OK").performClick()
        onNodeWithText("Backup saved").assertDoesNotExist()
    }

    @Test
    fun `a cancelled export shows nothing`() = runComposeUiTest {
        openData(filled(), FakeDataFiles(nextSave = SaveResult.Cancelled))
        onNodeWithText("Export a backup").performClick()
        onNodeWithText("Backup saved").assertDoesNotExist()
        onNodeWithText("Backup not saved").assertDoesNotExist()
    }

    @Test
    fun `importing shows what the file holds and replaces everything when confirmed`() = runComposeUiTest {
        val store = filled()
        val text = backupOf {
            create("Imported", Tier.One, Energy.Low)
            create("Second", Tier.One, Energy.Low)
        }
        openData(store, FakeDataFiles(text))
        onNodeWithText("Import a backup").performClick()

        onNodeWithText("Replace all data with this backup?").assertExists()
        onNodeWithText("2 task templates").assertExists()
        onNodeWithText("0 reward templates").assertExists()
        onNodeWithText("Balance: 0 coins").assertExists()

        onNodeWithText("Replace everything").performClick()
        onNodeWithText("Backup imported").assertExists()
        val templates = runBlocking { store.templates.first() }
        assertEquals(listOf("Second", "Imported"), templates.map { it.name })
        assertEquals("", runBlocking { store.settings.first() }.name)
        onNodeWithText("OpenAdhd").assertExists()
    }

    @Test
    fun `declining the import leaves the data alone`() = runComposeUiTest {
        val store = filled()
        openData(store, FakeDataFiles(backupOf { create("Imported", Tier.One, Energy.Low) }))
        onNodeWithText("Import a backup").performClick()
        onNodeWithText("Cancel").performClick()

        assertEquals(listOf("Clean"), runBlocking { store.templates.first() }.map { it.name })
        onNodeWithText("Replace all data with this backup?").assertDoesNotExist()
    }

    @Test
    fun `a file that is not a backup is explained and nothing changes`() = runComposeUiTest {
        val store = filled()
        openData(store, FakeDataFiles("{\"hello\": 1}"))
        onNodeWithText("Import a backup").performClick()

        onNodeWithText("Cannot import this file").assertExists()
        onNodeWithText("This is not an OpenAdhd backup. Nothing was changed.").assertExists()
        assertEquals(listOf("Clean"), runBlocking { store.templates.first() }.map { it.name })
    }

    @Test
    fun `delete asks first and cancel removes nothing`() = runComposeUiTest {
        val store = filled()
        openData(store, FakeDataFiles())
        onNodeWithText("Delete all data").performClick()

        onNodeWithText("Delete all data?").assertExists()
        onNodeWithText("Export a backup first").assertExists()
        onNodeWithText("Delete everything").assertExists()
        onNodeWithText("Cancel").performClick()

        onNodeWithText("Delete all data?").assertDoesNotExist()
        assertEquals(listOf("Clean"), runBlocking { store.templates.first() }.map { it.name })
    }

    @Test
    fun `exporting first keeps the confirmation open`() = runComposeUiTest {
        val files = FakeDataFiles()
        openData(filled(), files)
        onNodeWithText("Delete all data").performClick()
        onNodeWithText("Export a backup first").performClick()

        assertEquals(1, files.saved.size)
        onNodeWithText("Delete all data?").assertExists()
    }

    @Test
    fun `delete everything empties the app and resets the title`() = runComposeUiTest {
        val store = filled()
        openData(store, FakeDataFiles())
        onNodeWithText("Sam's OpenAdhd").assertExists()
        onNodeWithText("Delete all data").performClick()
        onNodeWithText("Delete everything").performClick()

        onNodeWithText("All data deleted").assertExists()
        assertEquals(StoreState(), runBlocking { store.exportData() })
        onNodeWithText("Sam's OpenAdhd").assertDoesNotExist()
    }
}
