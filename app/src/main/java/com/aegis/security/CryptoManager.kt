package com.aegis.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoManager {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_KEY_ALIAS = "aegis_aes_key"
        private const val RSA_KEY_ALIAS = "aegis_rsa_key"
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun generateAESKey(): Boolean {
        if (keyStore.containsAlias(AES_KEY_ALIAS)) return true
        return try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                AES_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun generateRSAKeyPair(): Boolean {
        if (keyStore.containsAlias(RSA_KEY_ALIAS)) return true
        return try {
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                RSA_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT or
                        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setKeySize(2048)
                .build()
            keyPairGenerator.initialize(spec)
            keyPairGenerator.generateKeyPair()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun encryptAES(plaintext: ByteArray): ByteArray? {
        return try {
            val secretKey = keyStore.getKey(AES_KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext)
            iv + ciphertext
        } catch (e: Exception) {
            null
        }
    }

    fun decryptAES(encrypted: ByteArray): ByteArray? {
        return try {
            val secretKey = keyStore.getKey(AES_KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = encrypted.copyOfRange(0, 12)
            val ciphertext = encrypted.copyOfRange(12, encrypted.size)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            null
        }
    }

    fun encryptWithPublicKey(plaintext: ByteArray): ByteArray? {
        return try {
            val keyStoreEntry = keyStore.getEntry(RSA_KEY_ALIAS, null)
            val publicKey = (keyStoreEntry as KeyStore.PrivateKeyEntry).certificate.publicKey
            val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            cipher.doFinal(plaintext)
        } catch (e: Exception) {
            null
        }
    }

    fun decryptWithPrivateKey(encrypted: ByteArray): ByteArray? {
        return try {
            val privateKey = keyStore.getKey(RSA_KEY_ALIAS, null) as PrivateKey
            val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            cipher.doFinal(encrypted)
        } catch (e: Exception) {
            null
        }
    }

    fun generateDatabasePassphrase(): ByteArray {
        return try {
            // We need a stable 32-byte key for SQLCipher.
            // Since we can't export raw AES keys from Android KeyStore,
            // we'll sign a constant string using our persistent RSA key
            // and use the SHA-256 hash of that signature as the stable passphrase.
            val signature = Signature.getInstance("SHA256withRSA")
            val privateKey = keyStore.getKey(RSA_KEY_ALIAS, null) as PrivateKey
            signature.initSign(privateKey)
            signature.update("aegis_db_seed_2024".toByteArray())
            val signatureBytes = signature.sign()
            
            MessageDigest.getInstance("SHA-256").digest(signatureBytes)
        } catch (e: Exception) {
            throw SecurityException("Failed to generate database passphrase from keystore", e)
        }
    }

    fun getSignature(data: ByteArray): ByteArray? {
        return try {
            val privateKey = keyStore.getKey(RSA_KEY_ALIAS, null) as PrivateKey
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initSign(privateKey)
            signature.update(data)
            signature.sign()
        } catch (e: Exception) {
            null
        }
    }

    fun verifySignature(data: ByteArray, signatureBytes: ByteArray): Boolean {
        return try {
            val keyStoreEntry = keyStore.getEntry(RSA_KEY_ALIAS, null)
            val publicKey = (keyStoreEntry as KeyStore.PrivateKeyEntry).certificate.publicKey
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initVerify(publicKey)
            signature.update(data)
            signature.verify(signatureBytes)
        } catch (e: Exception) {
            false
        }
    }

    fun hasKeys(): Boolean {
        return keyStore.containsAlias(AES_KEY_ALIAS) && keyStore.containsAlias(RSA_KEY_ALIAS)
    }
}
