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
        val vm = SettingsViewModel(store, dynamicColorSupported = true)
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
    fun `the theme shows its label as value and default`() = runTest {
        val vm = SettingsViewModel(store)
        assertEquals("Follow system theme", vm.row(SettingKey.Theme).valueLabel)
        assertEquals("Follow system theme", vm.row(SettingKey.Theme).defaultLabel)

        var result: String? = "unset"
        vm.save(SettingKey.Theme, "Dark") { result = it }
        assertNull(result)
        assertEquals("Dark", vm.row(SettingKey.Theme).valueLabel)
        assertEquals("Follow system theme", vm.row(SettingKey.Theme).defaultLabel)
        assertFalse(vm.row(SettingKey.Theme).isDefault)

        vm.restore(SettingKey.Theme)
        assertEquals("Follow system theme", vm.row(SettingKey.Theme).valueLabel)
        assertTrue(vm.row(SettingKey.Theme).isDefault)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `the appearance group comes first and holds only the theme where there is no dynamic color`() = runTest {
        val vm = SettingsViewModel(store, dynamicColorSupported = false)
        assertEquals(SettingGroup.Appearance, vm.sections.value.first().group)
        assertEquals(listOf(SettingKey.Theme), vm.sections.value.first().rows.map { it.key })
        assertTrue(vm.sections.value.flatMap { it.rows }.none { it.key == SettingKey.DynamicColor })
        vm.viewModelScope.cancel()
    }

    @Test
    fun `the appearance group lists dynamic color after the theme where the platform has it`() = runTest {
        val vm = SettingsViewModel(store, dynamicColorSupported = true)
        assertEquals(listOf(SettingKey.Theme, SettingKey.DynamicColor), vm.sections.value.first().rows.map { it.key })
        assertEquals("Dynamic (wallpaper colors)", vm.row(SettingKey.DynamicColor).valueLabel)
        assertTrue(vm.row(SettingKey.DynamicColor).isDefault)

        vm.save(SettingKey.DynamicColor, "app") { }
        assertEquals("App colors", vm.row(SettingKey.DynamicColor).valueLabel)
        assertFalse(vm.row(SettingKey.DynamicColor).isDefault)
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
