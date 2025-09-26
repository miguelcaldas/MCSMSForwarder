package com.miguelcaldas.mcsmsforwarder.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.miguelcaldas.mcsmsforwarder.R
import com.miguelcaldas.mcsmsforwarder.data.LogEntry
import com.miguelcaldas.mcsmsforwarder.data.LogType

class LogAdapter(private val context: Context, private var logEntries: List<LogEntry>) :
    RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val logMessage: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val entry = logEntries[position]
        holder.logMessage.text = "[${entry.getFormattedTimestamp()}] ${entry.message}"

        val color = when (entry.type) {
            LogType.FORWARDED -> R.color.log_forwarded
            LogType.NOT_FORWARDED -> R.color.log_not_forwarded
            LogType.TEST_PASS -> R.color.log_test_pass
            LogType.TEST_FAIL -> R.color.log_test_fail
            LogType.INFO -> R.color.log_info
        }
        holder.logMessage.setTextColor(ContextCompat.getColor(context, color))
    }

    override fun getItemCount() = logEntries.size

    fun updateLogs(newLogs: List<LogEntry>) {
        this.logEntries = newLogs
        notifyDataSetChanged()
    }
}
