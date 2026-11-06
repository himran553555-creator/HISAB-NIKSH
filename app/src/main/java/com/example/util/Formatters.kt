package com.example.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Formatters {
    private val bdtFormat = DecimalFormat("#,##,##0.00")
    private val bdtIntFormat = DecimalFormat("#,##,##0")
    private val dhakaZone = TimeZone.getTimeZone("Asia/Dhaka")

    fun formatCurrency(amount: Double, includeDecimals: Boolean = false): String {
        val formatted = if (includeDecimals) bdtFormat.format(amount) else bdtIntFormat.format(amount)
        return "৳$formatted"
    }

    fun formatDate(timestampMs: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply {
            timeZone = dhakaZone
        }
        return sdf.format(Date(timestampMs))
    }

    fun formatDateTime(timestampMs: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).apply {
            timeZone = dhakaZone
        }
        return sdf.format(Date(timestampMs))
    }

    fun currentDateString(): String {
        return formatDate(System.currentTimeMillis())
    }
}
