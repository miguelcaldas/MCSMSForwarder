package com.miguelcaldas.mcsmsforwarder

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.miguelcaldas.mcsmsforwarder.data.LogEntry
import com.miguelcaldas.mcsmsforwarder.data.LogType
import com.miguelcaldas.mcsmsforwarder.databinding.ActivityMainBinding
import com.miguelcaldas.mcsmsforwarder.ui.LogAdapter
import com.miguelcaldas.mcsmsforwarder.util.AppPreferences
import com.miguelcaldas.mcsmsforwarder.util.PermissionManager
import java.util.regex.Pattern

class MainActivity: AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var logAdapter: LogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppPreferences.init(applicationContext)
        PermissionManager.checkAndRequestPermissions(this)

        setupUI()
        loadSettings()
        setupListeners()
    }

    private fun setupUI() {
        binding.rvLog.layoutManager = LinearLayoutManager(this)
        logAdapter = LogAdapter(this, AppPreferences.logEntries)
        binding.rvLog.adapter = logAdapter
    }

    private fun loadSettings() {
        binding.etTargetNumber.setText(AppPreferences.targetNumber)
        binding.etSenderFilters.setText(AppPreferences.senderFilters.joinToString("\n"))
        binding.etRegexFilters.setText(AppPreferences.regexFilters.joinToString("\n"))
        updateLogView()
    }

    private fun saveSettings() {
        AppPreferences.targetNumber = binding.etTargetNumber.text.toString().trim()
        AppPreferences.senderFilters = binding.etSenderFilters.text.toString().lines()
        AppPreferences.regexFilters = binding.etRegexFilters.text.toString().lines()
        Toast.makeText(this, "Configuration Saved!", Toast.LENGTH_SHORT).show()
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            saveSettings()
        }

        binding.btnClearLog.setOnClickListener {
            AppPreferences.clearLog()
            updateLogView()
        }

        binding.btnTest.setOnClickListener {
            runTestFilter()
        }
    }

    private fun runTestFilter() {
        val testSender = binding.etTestSender.text.toString().trim()
        val testMessage = binding.etTestMessage.text.toString()

        if (testSender.isEmpty() || testMessage.isEmpty()) {
            Toast.makeText(this, "Please enter a test sender and message.", Toast.LENGTH_SHORT).show()
            return
        }

        val senderFilters = binding.etSenderFilters.text.toString().lines().filter { it.isNotBlank() }
        val regexFilters = binding.etRegexFilters.text.toString().lines().filter { it.isNotBlank() }

        var passed = false
        var reason: String

        if (senderFilters.isEmpty() || senderFilters.any { it.equals(testSender, ignoreCase = true) }) {
            if (regexFilters.isEmpty() || regexFilters.any { Pattern.compile(it).matcher(testMessage).find() }) {
                passed = true
                reason = "Test PASSED. Message would be forwarded."
            } else {
                reason = "Test FAILED: Blocked by content filter."
            }
        } else {
            reason = "Test FAILED: Blocked by sender filter."
        }

        val logType = if (passed) LogType.TEST_PASS else LogType.TEST_FAIL
        AppPreferences.addLogEntry(LogEntry(reason, logType))
        updateLogView()
    }

    private fun updateLogView() {
        logAdapter.updateLogs(AppPreferences.logEntries)
        binding.rvLog.scrollToPosition(0)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionManager.PERMISSION_REQUEST_CODE) {
            if (grantResults.any { it != android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions are required for the app to function.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
