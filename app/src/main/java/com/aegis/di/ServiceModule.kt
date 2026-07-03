package com.aegis.di

import android.content.Context
import com.aegis.services.overlay.ThreatOverlayManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideThreatOverlayManager(@ApplicationContext context: Context): ThreatOverlayManager =
        ThreatOverlayManager(context)
}
