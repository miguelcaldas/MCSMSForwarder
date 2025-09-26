package com.miguelcaldas.mcsmsforwarder.worker

import android.content.ContentUris
import android.content.Context
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.miguelcaldas.mcsmsforwarder.data.LogEntry
import com.miguelcaldas.mcsmsforwarder.data.LogType
import com.miguelcaldas.mcsmsforwarder.util.AppPreferences
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern

class SmsWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val TAG = "SmsWorker"

    companion object {
        const val KEY_ACTION = "key_action"
        const val KEY_SENDER = "key_sender"
        const val KEY_BODY = "key_body"
    }

    override suspend fun doWork(): Result {
        AppPreferences.init(applicationContext)

        when (inputData.getString(KEY_ACTION)) {
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                val sender = inputData.getString(KEY_SENDER) ?: "Unknown"
                val body = inputData.getString(KEY_BODY) ?: ""
                processAndForwardMessage(sender, body)
            }
            Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION -> {
                handleMmsWithRetry(applicationContext)
            }
        }
        return Result.success()
    }

    private suspend fun handleMmsWithRetry(context: Context) {
        val maxRetries = 5
        val delayMillis = 500L

        repeat(maxRetries) { attempt ->
            Log.d(TAG, "Attempt ${attempt + 1} to query for MMS...")
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                Telephony.Mms.Inbox.CONTENT_URI, null, null, null,
                "${Telephony.Mms.DATE} DESC"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val messageId = it.getString(it.getColumnIndexOrThrow(Telephony.Mms._ID))
                    val sender = getMmsSender(context, messageId) ?: "Unknown"
                    val messageBody = getMmsText(context, messageId)

                    if (messageBody.isNotBlank()) {
                        processAndForwardMessage(sender, messageBody)
                    } else {
                        Log.d(TAG, "MMS from $sender has no text content, ignoring.")
                    }
                    return // Exit successfully
                }
            }
            delay(delayMillis)
        }
        Log.w(TAG, "Could not find MMS message after $maxRetries attempts.")
    }

    private fun processAndForwardMessage(sender: String, messageBody: String) {
        AppPreferences.addLogEntry(LogEntry("Received from: $sender\nBody: $messageBody", LogType.INFO))

        val senderFilters = AppPreferences.senderFilters
        if (senderFilters.isEmpty() || senderFilters.any { it.equals(sender, ignoreCase = true) }) {
            AppPreferences.addLogEntry(LogEntry("Sender '$sender' passed filter.", LogType.INFO))

            val regexFilters = AppPreferences.regexFilters
            if (regexFilters.isEmpty() || regexFilters.any { Pattern.compile(it).matcher(messageBody).find() }) {
                forwardMessage(sender, messageBody)
            } else {
                AppPreferences.addLogEntry(LogEntry("Blocked by content filter.", LogType.NOT_FORWARDED))
            }
        } else {
            AppPreferences.addLogEntry(LogEntry("Blocked by sender filter.", LogType.NOT_FORWARDED))
        }
    }

    private fun forwardMessage(originalSender: String, messageBody: String) {
        val targetNumber = AppPreferences.targetNumber
        if (targetNumber.isBlank()) {
            AppPreferences.addLogEntry(LogEntry("Forwarding failed: Target number not set.", LogType.NOT_FORWARDED))
            return
        }

        try {
            val smsManager = applicationContext.getSystemService(SmsManager::class.java)
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

    // --- MMS Parsing Helper Functions ---
    private fun getMmsText(context: Context, messageId: String): String {
        val selection = "${Telephony.Mms.Part.MSG_ID} = ?"
        val selectionArgs = arrayOf(messageId)
        val cursor = context.contentResolver.query(
            Telephony.Mms.CONTENT_URI.buildUpon().appendPath("part").build(),
            null, selection, selectionArgs, null
        )

        var body = ""
        cursor?.use {
            while (it.moveToNext()) {
                val contentType = it.getString(it.getColumnIndexOrThrow(Telephony.Mms.Part.CONTENT_TYPE))
                if (contentType == "text/plain") {
                    val data = it.getString(it.getColumnIndexOrThrow(Telephony.Mms.Part._DATA))
                    val partId = it.getString(it.getColumnIndexOrThrow(Telephony.Mms.Part._ID))
                    body = if (data != null) {
                        val partUri = ContentUris.withAppendedId(Telephony.Mms.Part.CONTENT_URI, partId.toLong())
                        readMmsTextFromData(context, partUri)
                    } else {
                        it.getString(it.getColumnIndexOrThrow(Telephony.Mms.Part.TEXT))
                    }
                    break
                }
            }
        }
        return body
    }

    private fun readMmsTextFromData(context: Context, partUri: android.net.Uri): String {
        val stringBuilder = StringBuilder()
        context.contentResolver.openInputStream(partUri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }

    private fun getMmsSender(context: Context, messageId: String): String? {
        val selection = "${Telephony.Mms.Addr.MSG_ID} = ? AND ${Telephony.Mms.Addr.TYPE} = ?"
        // **FIXED**: Use the literal value for the 'FROM' header (137), as PduHeaders.FROM is a hidden API.
        val selectionArgs = arrayOf(messageId, "137")
        val cursor = context.contentResolver.query(
            Telephony.Mms.CONTENT_URI.buildUpon().appendPath(messageId).appendPath("addr").build(),
            null, selection, selectionArgs, null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(Telephony.Mms.Addr.ADDRESS))
            }
        }
        return null
    }
}