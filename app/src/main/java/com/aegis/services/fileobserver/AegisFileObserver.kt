package com.aegis.services.fileobserver

import android.os.FileObserver
import android.os.Environment
import java.io.File

class AegisFileObserver : FileObserver(
    File(Environment.getExternalStorageDirectory(), "Download").absolutePath,
    CREATE or MOVED_TO or CLOSE_WRITE
) {
    private val suspiciousExtensions = setOf("apk", "dex", "jar", "exe", "vbs", "ps1", "bat", "cmd")

    override fun onEvent(event: Int, path: String?) {
        if (path == null) return
        when (event) {
            CREATE, MOVED_TO -> handleNewFile(path)
            CLOSE_WRITE -> handleFileWritten(path)
        }
    }

    private fun handleNewFile(path: String) {
        val extension = path.substringAfterLast('.', "").lowercase()
        if (extension in suspiciousExtensions) {
            onSuspiciousFileDetected(path, extension)
        }
    }

    private fun handleFileWritten(path: String) {
    }

    private fun onSuspiciousFileDetected(path: String, extension: String) {
    }

    companion object {
        fun createObservers(): List<AegisFileObserver> {
            val directories = listOf(
                Environment.DIRECTORY_DOWNLOADS,
                Environment.DIRECTORY_DOCUMENTS,
                Environment.DIRECTORY_PICTURES,
                Environment.DIRECTORY_MOVIES
            )
            return directories.mapNotNull { dir ->
                val file = Environment.getExternalStoragePublicDirectory(dir)
                if (file.exists()) AegisFileObserver() else null
            }
        }
    }
}
