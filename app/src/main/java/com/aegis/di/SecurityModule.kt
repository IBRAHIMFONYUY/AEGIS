package com.aegis.di

import android.content.Context
import com.aegis.security.CryptoManager
import com.aegis.security.KeyStoreManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideCryptoManager(): CryptoManager {
        return CryptoManager().apply {
            generateAESKey()
            generateRSAKeyPair()
        }
    }

    @Provides
    @Singleton
    fun provideKeyStoreManager(@ApplicationContext context: Context): KeyStoreManager {
        return KeyStoreManager(context).apply {
            initializeMasterKey()
        }
    }

    @Provides
    @Singleton
    fun provideBiometricHelper(@ApplicationContext context: Context): com.aegis.security.BiometricHelper {
        return com.aegis.security.BiometricHelper(context)
    }
}
