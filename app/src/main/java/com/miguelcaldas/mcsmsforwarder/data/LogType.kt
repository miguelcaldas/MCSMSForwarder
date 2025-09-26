package com.miguelcaldas.mcsmsforwarder.data

enum class LogType {
    INFO,           // General information
    FORWARDED,      // SMS successfully passed filters and was forwarded
    NOT_FORWARDED,  // SMS did not pass filters
    TEST_PASS,      // Test message passed filters
    TEST_FAIL       // Test message did not pass filters
}
