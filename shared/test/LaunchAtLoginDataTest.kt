package com.usuallycoding.openadhd

import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.BackupJson
import com.usuallycoding.openadhd.domain.LoginItem
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

/** The login item belongs to the system, so a backup does not carry it and Delete all data does not touch it. */
@OptIn(ExperimentalCoroutinesApi::class)
class LaunchAtLoginDataTest {
    private class FakeLoginItem(var on: Boolean) : LoginItem {
        var changes = 0

        override fun isEnabled() = on

        override fun setEnabled(enabled: Boolean) {
            changes++
            on = enabled
        }
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a backup does not mention launching at login`() = runTest {
        val store = InMemoryStore()
        val text = BackupJson.encode(store.exportData(), Clock.System.now()).lowercase()
        assertFalse("login" in text)
        assertFalse("launch" in text)
    }

    @Test
    fun `delete all data leaves launch at login as it was`() = runTest {
        val item = FakeLoginItem(on = true)
        val vm = SettingsViewModel(InMemoryStore(), loginItem = item)
        assertEquals(true, vm.launchAtLogin.value)

        vm.requestDelete()
        vm.confirmDelete()

        assertTrue(item.on)
        assertEquals(0, item.changes)
        vm.refreshLaunchAtLogin()
        assertEquals(true, vm.launchAtLogin.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `there is no value without a login item`() = runTest {
        val vm = SettingsViewModel(InMemoryStore())
        assertNull(vm.launchAtLogin.value)
        vm.setLaunchAtLogin(true)
        assertNull(vm.launchAtLogin.value)
        vm.viewModelScope.cancel()
    }
}
