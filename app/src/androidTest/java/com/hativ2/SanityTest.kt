package com.hativ2

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SanityTest {
    @Test
    fun testOnePlusOne() {
        assertEquals(2, 1 + 1)
    }
}
