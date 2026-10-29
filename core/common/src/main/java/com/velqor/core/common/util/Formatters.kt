package com.velqor.core.common.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object Formatters {
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return if (digitGroups == 0) {
            "$bytes B"
        } else {
            String.format(Locale.getDefault(), "%.1f %s", value, units[digitGroups])
        }
    }

    /**
     * iOS style date format: e.g. 28/10/2026 (omits time for list items)
     */
    fun formatItemDate(timestampMs: Long): String {
        if (timestampMs <= 0) return "--"
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }

    fun formatDate(timestampMs: Long): String {
        if (timestampMs <= 0) return "--"
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }

    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatFileSize(bytesPerSecond)}/s"
    }

    /**
     * Preserves file extension by truncating in the middle
     */
    fun middleEllipsis(text: String, maxChars: Int = 30): String {
        if (text.length <= maxChars) return text
        val extIndex = text.lastIndexOf('.')
        if (extIndex > 0 && text.length - extIndex <= 8) {
            val ext = text.substring(extIndex)
            val base = text.substring(0, extIndex)
            val available = maxChars - ext.length - 1
            if (available > 6) {
                val half = available / 2
                return base.take(half) + "…" + base.takeLast(half) + ext
            }
        }
        val half = (maxChars - 1) / 2
        return text.take(half) + "…" + text.takeLast(half)
    }
}
