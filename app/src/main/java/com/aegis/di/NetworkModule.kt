package com.aegis.di

import android.content.Context
import com.aegis.network.ThreatIntelClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideThreatIntelClient(@ApplicationContext context: Context): ThreatIntelClient {
        return ThreatIntelClient(context).apply {
            initialize()
        }
    }
}
