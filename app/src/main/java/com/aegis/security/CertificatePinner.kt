package com.aegis.security

import android.content.Context
import okhttp3.CertificatePinner
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class CertificatePinner(private val context: Context) {

    private val pins = mutableListOf<String>()

    companion object {
        private const val HASH_ALGORITHM = "sha256"
    }

    fun addPin(hostname: String, certificateHash: String) {
        pins.add("$hostname/$HASH_ALGORITHM/$certificateHash")
    }

    fun addPinFromCertificate(hostname: String, certResourceId: Int) {
        try {
            val inputStream = context.resources.openRawResource(certResourceId)
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val certificate = certificateFactory.generateCertificate(inputStream) as X509Certificate
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(certificate.publicKey.encoded)
            val hashString = hash.joinToString("") { "%02X".format(it) }
            addPin(hostname, hashString)
        } catch (e: Exception) {
            // Certificate pinning setup failed
        }
    }

    fun build(): CertificatePinner {
        val builder = CertificatePinner.Builder()
        pins.forEach { pin ->
            val parts = pin.split("/")
            if (parts.size == 3) {
                builder.add(parts[0], "${parts[1]}/${parts[2]}")
            }
        }
        return builder.build()
    }

    fun clear() {
        pins.clear()
    }
}
