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
    fun provideTodoRepository(healthTodoDao: HealthTodoDao): TodoRepository {
        return TodoRepositoryImpl(healthTodoDao)
    }
}