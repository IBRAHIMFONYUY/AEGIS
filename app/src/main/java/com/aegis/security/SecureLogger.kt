package com.aegis.security

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class SecureLogger(private val context: Context) {

    companion object {
        private const val TAG = "AEGIS"
        private const val MAX_LOG_SIZE = 5 * 1024 * 1024
        private const val LOG_DIR = "logs"
    }

    private val logDir: File get() = File(context.filesDir, LOG_DIR).also { it.mkdirs() }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    enum class LogLevel(val value: Int) {
        DEBUG(0), INFO(1), WARN(2), ERROR(3), SECURITY(4)
    }

    fun d(message: String) = log(LogLevel.DEBUG, message)
    fun i(message: String) = log(LogLevel.INFO, message)
    fun w(message: String) = log(LogLevel.WARN, message)
    fun e(message: String) = log(LogLevel.ERROR, message)
    fun security(message: String) = log(LogLevel.SECURITY, message)

    private fun log(level: LogLevel, message: String) {
        val logMessage = "${dateFormat.format(Date())} [${level.name}] $message"
        Log.d(TAG, logMessage)

        if (level == LogLevel.SECURITY) {
            writeToFile(logMessage, "security.log")
        }
        if (level.value >= LogLevel.WARN.value) {
            writeToFile(logMessage, "aegis.log")
        }
    }

    private fun writeToFile(message: String, fileName: String) {
        try {
            val file = File(logDir, fileName)
            rotateIfNeeded(file)
            FileWriter(file, true).use { writer ->
                writer.appendLine(message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log", e)
        }
    }

    private fun rotateIfNeeded(file: File) {
        if (file.exists() && file.length() > MAX_LOG_SIZE) {
            val rotated = File(logDir, "${file.name}.1")
            file.renameTo(rotated)
        }
    }

    fun getLogFiles(): List<File> = logDir.listFiles()?.toList() ?: emptyList()

    fun clearLogs() {
        logDir.listFiles()?.forEach { it.delete() }
    }
}
