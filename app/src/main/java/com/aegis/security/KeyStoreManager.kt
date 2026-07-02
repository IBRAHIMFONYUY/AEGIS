package com.aegis.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.KeyStore.SecretKeyEntry
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class KeyStoreManager(private val context: Context) {

    companion object {
        private const val KEYSTORE_ALIAS = "aegis_master_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun initializeMasterKey(): Boolean {
        return try {
            if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getMasterKey(): SecretKey? {
        return try {
            keyStore.getEntry(KEYSTORE_ALIAS, null)?.let { entry ->
                (entry as SecretKeyEntry).secretKey
            }
        } catch (e: Exception) {
            null
        }
    }

    fun hasMasterKey(): Boolean = keyStore.containsAlias(KEYSTORE_ALIAS)
}
