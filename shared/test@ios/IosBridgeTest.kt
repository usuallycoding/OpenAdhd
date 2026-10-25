package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.data.IosAlarmBridge
import com.usuallycoding.openadhd.data.IosAlarmItem
import com.usuallycoding.openadhd.data.IosBridge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class IosBridgeTest {
    @Test
    fun `the bridge holds the alarms Swift registers and one shared set of entry requests`() {
        assertNull(IosBridge.alarms)
        val fake = object : IosAlarmBridge {
            var replaced: List<IosAlarmItem> = emptyList()
            override val isAvailable = true
            override fun isAuthorized() = true
            override fun requestAuthorization(onResult: (Boolean) -> Unit) = onResult(true)
            override fun replaceAll(items: List<IosAlarmItem>) { replaced = items }
        }
        IosBridge.alarms = fake
        assertSame(fake, IosBridge.alarms)
        IosBridge.alarms?.replaceAll(listOf(IosAlarmItem("e", "Task", 5)))
        assertEquals(listOf(IosAlarmItem("e", "Task", 5)), fake.replaced)
        IosBridge.alarms = null

        IosBridge.entryRequests.open("x")
        assertEquals("x", IosBridge.entryRequests.pending.value)
        IosBridge.entryRequests.consume("x")
    }
}
