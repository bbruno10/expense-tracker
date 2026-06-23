package com.brunobrandao.expensetracker.di

import android.content.Context
import com.brunobrandao.expensetracker.data.preferences.UserPreferencesRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        @ApplicationContext appContext: Context,
        firestore: FirebaseFirestore
    ): UserPreferencesRepository {
        return UserPreferencesRepository(appContext, firestore)
    }
}
