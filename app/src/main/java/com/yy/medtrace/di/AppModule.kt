package com.yy.medtrace.di

import android.content.Context
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.dao.FamilyMemberDao
import com.yy.medtrace.data.dao.HealthTodoDao
import com.yy.medtrace.data.dao.MedicalRecordDao
import com.yy.medtrace.data.repository.MemberRepository
import com.yy.medtrace.data.repository.MemberRepositoryImpl
import com.yy.medtrace.data.repository.RecordRepository
import com.yy.medtrace.data.repository.RecordRepositoryImpl
import com.yy.medtrace.data.repository.TodoRepository
import com.yy.medtrace.data.repository.TodoRepositoryImpl
import com.yy.medtrace.data.settings.LlmSettingsStore
import com.yy.medtrace.data.settings.OnboardingStore
import com.yy.medtrace.data.settings.PrivacyConsentStore
import com.yy.medtrace.data.settings.SecuritySettingsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    
    @Provides
    fun provideFamilyMemberDao(database: AppDatabase): FamilyMemberDao {
        return database.familyMemberDao()
    }
    
    @Provides
    fun provideMedicalRecordDao(database: AppDatabase): MedicalRecordDao {
        return database.medicalRecordDao()
    }
    
    @Provides
    fun provideHealthTodoDao(database: AppDatabase): HealthTodoDao {
        return database.healthTodoDao()
    }
    
    @Provides
    @Singleton
    fun provideMemberRepository(familyMemberDao: FamilyMemberDao): MemberRepository {
        return MemberRepositoryImpl(familyMemberDao)
    }
    
    @Provides
    @Singleton
    fun provideRecordRepository(medicalRecordDao: MedicalRecordDao): RecordRepository {
        return RecordRepositoryImpl(medicalRecordDao)
    }
    
    @Provides
    @Singleton
    fun provideTodoRepository(database: AppDatabase, healthTodoDao: HealthTodoDao): TodoRepository {
        return TodoRepositoryImpl(database, healthTodoDao)
    }
    
    @Provides
    @Singleton
    fun provideLlmSettingsStore(@ApplicationContext context: Context): LlmSettingsStore {
        return LlmSettingsStore(context)
    }
    
    @Provides
    @Singleton
    fun provideSecuritySettingsStore(@ApplicationContext context: Context): SecuritySettingsStore {
        return SecuritySettingsStore(context)
    }
    
    @Provides
    @Singleton
    fun provideOnboardingStore(@ApplicationContext context: Context): OnboardingStore {
        return OnboardingStore(context)
    }
    
    @Provides
    @Singleton
    fun providePrivacyConsentStore(@ApplicationContext context: Context): PrivacyConsentStore {
        return PrivacyConsentStore(context)
    }
}