package com.velqor.core.common

import com.velqor.core.common.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {
    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", Formatters.formatFileSize(0))
        assertEquals("500 B", Formatters.formatFileSize(500))
        assertEquals("1.0 KB", Formatters.formatFileSize(1024))
        assertEquals("1.5 MB", Formatters.formatFileSize((1024 * 1024 * 1.5).toLong()))
        assertEquals("2.0 GB", Formatters.formatFileSize(2L * 1024 * 1024 * 1024))
    }
}
