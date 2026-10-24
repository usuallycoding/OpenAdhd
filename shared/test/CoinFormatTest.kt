package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.formatCoins
import kotlin.test.Test
import kotlin.test.assertEquals

class CoinFormatTest {
    @Test
    fun `amounts below one thousand are shown as is`() {
        assertEquals("0", formatCoins(0))
        assertEquals("7", formatCoins(7))
        assertEquals("999", formatCoins(999))
    }

    @Test
    fun `thousands have one decimal that is truncated and dropped when zero`() {
        assertEquals("1k", formatCoins(1000))
        assertEquals("1k", formatCoins(1099))
        assertEquals("1.1k", formatCoins(1123))
        assertEquals("1.9k", formatCoins(1999))
        assertEquals("12.5k", formatCoins(12_500))
        assertEquals("99.9k", formatCoins(99_999))
    }

    @Test
    fun `from one hundred thousand the decimal goes`() {
        assertEquals("100k", formatCoins(100_000))
        assertEquals("100k", formatCoins(100_400))
        assertEquals("999k", formatCoins(999_999))
    }

    @Test
    fun `millions follow the same rule`() {
        assertEquals("1M", formatCoins(1_000_000))
        assertEquals("1.2M", formatCoins(1_250_000))
        assertEquals("2M", formatCoins(2_000_000))
        assertEquals("99.9M", formatCoins(99_999_999))
        assertEquals("100M", formatCoins(100_000_000))
        assertEquals("123M", formatCoins(123_456_789))
    }

    @Test
    fun `negative amounts keep their sign`() {
        assertEquals("-7", formatCoins(-7))
        assertEquals("-1.5k", formatCoins(-1500))
        assertEquals("-100k", formatCoins(-100_400))
        assertEquals("-1.2M", formatCoins(-1_250_000))
    }

    @Test
    fun `the extreme values do not overflow`() {
        assertEquals("2147M", formatCoins(Int.MAX_VALUE))
        assertEquals("-2147M", formatCoins(Int.MIN_VALUE))
    }
}
