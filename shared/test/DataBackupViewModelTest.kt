package com.usuallycoding.openadhd

import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.data.OpenResult
import com.usuallycoding.openadhd.data.SaveResult
import com.usuallycoding.openadhd.domain.BackupJson
import com.usuallycoding.openadhd.domain.BackupResult
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.SettingKey
import com.usuallycoding.openadhd.domain.StoreState
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.settings.BackupSummary
import com.usuallycoding.openadhd.ui.settings.DataDialog
import com.usuallycoding.openadhd.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DataBackupViewModelTest {
    private val clock = FakeClock()
    private val store = InMemoryStore(clock)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(files: FakeDataFiles?) = SettingsViewModel(store, files, clock, TimeZone.UTC)

    private suspend fun fill(target: InMemoryStore = store) {
        val template = target.create("Clean", Tier.Three, Energy.Low, subtasks = listOf("A", "B"))
        target.create("Walk", Tier.One, Energy.Low)
        target.completeSubtask(target.assign(template, clock.todayIn(TimeZone.UTC)).subtasks[0].id)
        target.createReward("Tea", RewardTier.One, Energy.Low)
        target.saveSetting(SettingKey.Name, "Sam")
    }

    /** A backup text made from another store, to import. */
    private suspend fun otherBackup(): Pair<String, StoreState> {
        val other = InMemoryStore(clock)
        other.create("Imported", Tier.Two, Energy.Medium)
        other.createReward("Cake", RewardTier.Two, Energy.Low)
        val data = other.exportData()
        return BackupJson.encode(data, clock.now()) to data
    }

    @Test
    fun `without a file dialog export and import do nothing`() = runTest {
        val vm = viewModel(null)
        assertFalse(vm.canTransfer)
        vm.exportBackup()
        vm.chooseImport()
        assertNull(vm.dialog.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `an export is saved under a dated name with everything in it`() = runTest {
        fill()
        val files = FakeDataFiles()
        val vm = viewModel(files)
        assertTrue(vm.canTransfer)

        vm.exportBackup()

        val (name, text) = files.saved.single()
        assertEquals("openadhd-2026-10-23.json", name)
        assertEquals(store.exportData(), assertIs<BackupResult.Valid>(BackupJson.decode(text)).state)
        assertEquals(DataDialog.Notice("Backup saved", "Your data was saved to the file you chose."), vm.dialog.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a cancelled export saves nothing and says nothing`() = runTest {
        val files = FakeDataFiles(nextSave = SaveResult.Cancelled)
        val vm = viewModel(files)
        vm.exportBackup()
        assertTrue(files.saved.isEmpty())
        assertNull(vm.dialog.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a failed export says it was not saved`() = runTest {
        val vm = viewModel(FakeDataFiles(nextSave = SaveResult.Failed))
        vm.exportBackup()
        assertEquals("Backup not saved", assertIs<DataDialog.Notice>(vm.dialog.value).title)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a valid file is summarised and nothing changes until it is confirmed`() = runTest {
        fill()
        val before = store.exportData()
        val (text, data) = otherBackup()
        val vm = viewModel(FakeDataFiles(text))

        vm.chooseImport()

        val dialog = assertIs<DataDialog.ConfirmImport>(vm.dialog.value)
        assertEquals(BackupSummary(taskTemplates = 1, rewardTemplates = 1, todayEntries = 0, redemptions = 0, balance = 0), dialog.summary)
        assertEquals(data, dialog.data)
        assertEquals(before, store.exportData())
        vm.viewModelScope.cancel()
    }

    @Test
    fun `confirming replaces everything and says so`() = runTest {
        fill()
        val (text, data) = otherBackup()
        val vm = viewModel(FakeDataFiles(text))
        vm.chooseImport()

        vm.confirmImport()

        assertEquals(data, store.exportData())
        assertEquals("Backup imported", assertIs<DataDialog.Notice>(vm.dialog.value).title)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `declining the import changes nothing`() = runTest {
        fill()
        val before = store.exportData()
        val vm = viewModel(FakeDataFiles(otherBackup().first))
        vm.chooseImport()

        vm.dismissDialog()
        vm.confirmImport()

        assertNull(vm.dialog.value)
        assertEquals(before, store.exportData())
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a file that is not a backup is explained and changes nothing`() = runTest {
        fill()
        val before = store.exportData()
        val vm = viewModel(FakeDataFiles("{\"hello\": 1}"))

        vm.chooseImport()

        val notice = assertIs<DataDialog.Notice>(vm.dialog.value)
        assertEquals("Cannot import this file", notice.title)
        assertEquals("This is not an OpenAdhd backup. Nothing was changed.", notice.text)
        vm.confirmImport()
        assertEquals(before, store.exportData())
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a damaged or unreadable file is explained`() = runTest {
        val damaged = viewModel(FakeDataFiles("{\"app\": "))
        damaged.chooseImport()
        assertTrue(assertIs<DataDialog.Notice>(damaged.dialog.value).text.startsWith("The file could not be read."))

        val unreadable = viewModel(FakeDataFiles(nextOpen = OpenResult.Failed))
        unreadable.chooseImport()
        assertTrue(assertIs<DataDialog.Notice>(unreadable.dialog.value).text.startsWith("The file could not be read."))
        damaged.viewModelScope.cancel()
        unreadable.viewModelScope.cancel()
    }

    @Test
    fun `a newer backup is explained`() = runTest {
        val newer = BackupJson.encode(StoreState(), clock.now()).replace("\"formatVersion\": 1", "\"formatVersion\": 5")
        val vm = viewModel(FakeDataFiles(newer))
        vm.chooseImport()
        assertTrue(assertIs<DataDialog.Notice>(vm.dialog.value).text.contains("newer version of OpenAdhd"))
        vm.viewModelScope.cancel()
    }

    @Test
    fun `closing the open dialog shows nothing`() = runTest {
        val files = FakeDataFiles(nextOpen = OpenResult.Cancelled)
        val vm = viewModel(files)
        vm.chooseImport()
        assertEquals(1, files.opened)
        assertNull(vm.dialog.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `delete asks first and cancelling removes nothing`() = runTest {
        fill()
        val before = store.exportData()
        val vm = viewModel(FakeDataFiles())

        vm.requestDelete()
        assertEquals(DataDialog.ConfirmDelete, vm.dialog.value)
        vm.dismissDialog()
        vm.confirmDelete()

        assertEquals(before, store.exportData())
        vm.viewModelScope.cancel()
    }

    @Test
    fun `confirming delete empties the app including the settings`() = runTest {
        fill()
        val vm = viewModel(FakeDataFiles())
        vm.requestDelete()

        vm.confirmDelete()

        assertEquals(StoreState(), store.exportData())
        assertEquals("All data deleted", assertIs<DataDialog.Notice>(vm.dialog.value).title)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `exporting before deleting saves the file and keeps the confirmation open`() = runTest {
        fill()
        val files = FakeDataFiles()
        val vm = viewModel(files)
        vm.requestDelete()

        vm.exportBeforeDelete()

        assertEquals(1, files.saved.size)
        assertEquals(DataDialog.ConfirmDelete, vm.dialog.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a failed backup before deleting deletes nothing`() = runTest {
        fill()
        val before = store.exportData()
        val vm = viewModel(FakeDataFiles(nextSave = SaveResult.Failed))
        vm.requestDelete()

        vm.exportBeforeDelete()

        assertEquals("Backup not saved", assertIs<DataDialog.Notice>(vm.dialog.value).title)
        assertEquals(before, store.exportData())
        vm.viewModelScope.cancel()
    }
}
