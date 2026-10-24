package com.usuallycoding.openadhd

import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.SettingGroup
import com.usuallycoding.openadhd.domain.SettingKey
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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val store = InMemoryStore()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun SettingsViewModel.row(key: SettingKey) = sections.value.flatMap { it.rows }.first { it.key == key }

    @Test
    fun `every setting is listed once in its group with its default`() = runTest {
        val vm = SettingsViewModel(store)
        assertEquals(SettingGroup.entries, vm.sections.value.map { it.group })
        assertEquals(SettingKey.entries.toSet(), vm.sections.value.flatMap { it.rows }.map { it.key }.toSet())
        assertEquals(SettingKey.entries.size, vm.sections.value.sumOf { it.rows.size })
        assertTrue(vm.sections.value.flatMap { it.rows }.all { it.isDefault })
        assertEquals("Not set", vm.row(SettingKey.Name).valueLabel)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `saving updates the value and the restore state and restoring undoes it`() = runTest {
        val vm = SettingsViewModel(store)
        var result: String? = "unset"
        vm.save(SettingKey.DebtLimit, "200") { result = it }
        assertNull(result)
        assertEquals("200", vm.row(SettingKey.DebtLimit).value)
        assertEquals("500", vm.row(SettingKey.DebtLimit).default)
        assertFalse(vm.row(SettingKey.DebtLimit).isDefault)

        vm.restore(SettingKey.DebtLimit)
        assertTrue(vm.row(SettingKey.DebtLimit).isDefault)
        assertEquals("500", vm.row(SettingKey.DebtLimit).value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a refused value reports why and changes nothing`() = runTest {
        val vm = SettingsViewModel(store)
        var result: String? = null
        vm.save(SettingKey.PayoutTier3, "10") { result = it }
        assertEquals("Tier 3 must pay more than tier 2", result)
        assertTrue(vm.row(SettingKey.PayoutTier3).isDefault)
        vm.viewModelScope.cancel()
    }
}
