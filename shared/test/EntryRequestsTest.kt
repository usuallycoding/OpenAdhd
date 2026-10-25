package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.EntryRequests
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EntryRequestsTest {
    @Test
    fun `nothing is waiting at first`() {
        assertNull(EntryRequests().pending.value)
    }

    @Test
    fun `a request made before anyone is looking is still there afterwards`() {
        val requests = EntryRequests()
        requests.open("a")
        assertEquals("a", requests.pending.value)
    }

    @Test
    fun `consuming clears the request it was given for`() {
        val requests = EntryRequests()
        requests.open("a")
        requests.consume("a")
        assertNull(requests.pending.value)
    }

    @Test
    fun `consuming an older request leaves a newer one waiting`() {
        val requests = EntryRequests()
        requests.open("a")
        requests.open("b")
        requests.consume("a")
        assertEquals("b", requests.pending.value)
    }

    @Test
    fun `a newer request replaces one that was not taken`() {
        val requests = EntryRequests()
        requests.open("a")
        requests.open("b")
        assertEquals("b", requests.pending.value)
    }

    @Test
    fun `the container carries its own holder`() {
        val a = com.usuallycoding.openadhd.data.AppContainer(com.usuallycoding.openadhd.data.InMemoryStore())
        val b = com.usuallycoding.openadhd.data.AppContainer(com.usuallycoding.openadhd.data.InMemoryStore())
        a.entryRequests.open("x")
        assertNull(b.entryRequests.pending.value)
        assertEquals("x", a.entryRequests.pending.value)
    }
}
