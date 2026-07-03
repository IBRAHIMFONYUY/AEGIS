package com.aegis.ai

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File

class ImageAnalyzer(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromImage(imagePath: String): String? {
        val file = File(imagePath)
        if (!file.exists()) return null

        return try {
            val image = InputImage.fromFilePath(context, Uri.fromFile(file))
            val result = recognizer.process(image).await()
            result.text
        } catch (e: Exception) {
            null
        }
    }

    suspend fun extractTextFromUri(uri: Uri): String? {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            result.text
        } catch (e: Exception) {
            null
        }
    }
}
