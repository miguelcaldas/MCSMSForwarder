package com.miguelcaldas.mcsmsforwarder.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val message: String,
    val type: LogType,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getFormattedTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
