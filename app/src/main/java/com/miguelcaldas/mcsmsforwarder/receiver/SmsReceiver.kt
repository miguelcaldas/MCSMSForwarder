package com.miguelcaldas.mcsmsforwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.miguelcaldas.mcsmsforwarder.worker.SmsWorker

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val workManager = WorkManager.getInstance(context)

        when (intent.action) {
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                // For SMS, parse messages and enqueue a worker for each.
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                messages?.forEach { smsMessage ->
                    val sender = smsMessage.originatingAddress ?: "Unknown"
                    val messageBody = smsMessage.messageBody ?: ""

                    val inputData = Data.Builder()
                        .putString(SmsWorker.KEY_ACTION, intent.action)
                        .putString(SmsWorker.KEY_SENDER, sender)
                        .putString(SmsWorker.KEY_BODY, messageBody)
                        .build()

                    val workRequest = OneTimeWorkRequestBuilder<SmsWorker>()
                        .setInputData(inputData)
                        .build()

                    workManager.enqueue(workRequest)
                }
            }

            Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION -> {
                // For MMS/RCS, just trigger the worker to handle the query.
                if (intent.type == "application/vnd.wap.mms-message") {
                    val inputData = Data.Builder()
                        .putString(SmsWorker.KEY_ACTION, intent.action)
                        .build()

                    val workRequest = OneTimeWorkRequestBuilder<SmsWorker>()
                        .setInputData(inputData)
                        .build()

                    workManager.enqueue(workRequest)
                }
            }
        }
    }
}
/*
package com.miguelcaldas.mcsmsforwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.widget.Toast
import com.miguelcaldas.mcsmsforwarder.data.LogEntry
import com.miguelcaldas.mcsmsforwarder.data.LogType
import com.miguelcaldas.mcsmsforwarder.util.AppPreferences
import java.util.regex.Pattern

class SmsReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "Broadcast Received", Toast.LENGTH_SHORT).show()
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        AppPreferences.init(context)
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages?.forEach { sms ->
            val sender = sms.originatingAddress ?: "Unknown"
            val messageBody = sms.messageBody ?: ""

            Toast.makeText(context, "SMS Received from $sender", Toast.LENGTH_SHORT).show()
            AppPreferences.addLogEntry(LogEntry("Received from: $sender\nBody: $messageBody", LogType.INFO))

            // Step 1: Filter by sender
            val senderFilters = AppPreferences.senderFilters
            if (senderFilters.isEmpty() || senderFilters.any { it.equals(sender, ignoreCase = true) }) {
                Toast.makeText(context, "Passed sender filter", Toast.LENGTH_SHORT).show()
                AppPreferences.addLogEntry(LogEntry("Sender '$sender' passed filter.", LogType.INFO))

                // Step 2: Filter by regex
                val regexFilters = AppPreferences.regexFilters
                if (regexFilters.isEmpty() || regexFilters.any { Pattern.compile(it).matcher(messageBody).find() }) {
                    Toast.makeText(context, "Passed content filter", Toast.LENGTH_SHORT).show()
                    forwardSms(context, sender, messageBody)
                } else {
                    AppPreferences.addLogEntry(LogEntry("Blocked by content filter.", LogType.NOT_FORWARDED))
                }
            } else {
                AppPreferences.addLogEntry(LogEntry("Blocked by sender filter.", LogType.NOT_FORWARDED))
            }
        }
    }

    private fun forwardSms(context: Context, originalSender: String, messageBody: String) {
        val targetNumber = AppPreferences.targetNumber
        if (targetNumber.isBlank()) {
            AppPreferences.addLogEntry(LogEntry("Forwarding failed: Target number not set.", LogType.NOT_FORWARDED))
            return
        }

        try {
            val smsManager: SmsManager = context.getSystemService(SmsManager::class.java)
            val fullMessage = "Fwd from $originalSender: $messageBody"
            smsManager.sendTextMessage(targetNumber, null, fullMessage, null, null)
            AppPreferences.addLogEntry(
                LogEntry("Forwarded to $targetNumber:\n$fullMessage", LogType.FORWARDED)
            )
        } catch (e: Exception) {
            AppPreferences.addLogEntry(
                LogEntry("Forwarding failed: ${e.message}", LogType.NOT_FORWARDED)
            )
        }
    }
}
*/