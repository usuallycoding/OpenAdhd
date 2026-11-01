package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.data.versionLabelOf
import kotlin.test.Test
import kotlin.test.assertEquals

class IosVersionTest {
    @Test
    fun `the label has the short version and the bundle version`() {
        val info = mapOf<Any?, Any?>("CFBundleShortVersionString" to "1.0.0", "CFBundleVersion" to "1")
        assertEquals("OpenAdhd 1.0.0 (1)", versionLabelOf(info))
    }

    @Test
    fun `a missing bundle version leaves out the brackets`() {
        assertEquals("OpenAdhd 1.0.0", versionLabelOf(mapOf<Any?, Any?>("CFBundleShortVersionString" to "1.0.0")))
    }

    @Test
    fun `no info or no version reads dev`() {
        assertEquals("OpenAdhd dev", versionLabelOf(null))
        assertEquals("OpenAdhd dev", versionLabelOf(emptyMap<Any?, Any?>()))
        assertEquals("OpenAdhd dev", versionLabelOf(mapOf<Any?, Any?>("CFBundleShortVersionString" to 5)))
    }
}
