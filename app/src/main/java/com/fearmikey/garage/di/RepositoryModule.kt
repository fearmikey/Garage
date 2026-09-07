package com.fearmikey.garage.di

import android.content.Context
import com.fearmikey.garage.data.local.PreferencesManager
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.PreferencesRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager =
        PreferencesManager(context)

    @Provides
    @Singleton
    fun providePreferencesRepository(
        preferencesManager: PreferencesManager
    ): PreferencesRepository = PreferencesRepositoryImpl(preferencesManager)
}
