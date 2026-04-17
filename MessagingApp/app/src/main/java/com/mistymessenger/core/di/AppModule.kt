package com.mistymessenger.core.di

import android.content.Context
import androidx.room.Room
import com.mistymessenger.core.db.AppDatabase
import com.mistymessenger.core.network.RetrofitClient
import com.mistymessenger.core.network.SocketManager
import com.mistymessenger.core.network.TokenProvider
import com.mistymessenger.core.service.PresenceService
import com.mistymessenger.core.worker.WorkerScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "misty_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserDao(db: AppDatabase) = db.userDao()
    @Provides fun provideChatDao(db: AppDatabase) = db.chatDao()
    @Provides fun provideMessageDao(db: AppDatabase) = db.messageDao()
    @Provides fun provideContactDao(db: AppDatabase) = db.contactDao()
    @Provides fun provideStatusDao(db: AppDatabase) = db.statusDao()
    @Provides fun provideCallLogDao(db: AppDatabase) = db.callLogDao()
    @Provides fun provideAccountDao(db: AppDatabase) = db.accountDao()
    @Provides fun provideAutoReplyDao(db: AppDatabase) = db.autoReplyDao()
    @Provides fun provideScheduledMessageDao(db: AppDatabase) = db.scheduledMessageDao()
    @Provides fun provideChatSettingsDao(db: AppDatabase) = db.chatSettingsDao()
    @Provides fun provideStarredMessageDao(db: AppDatabase) = db.starredMessageDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideRetrofitClient(tokenProvider: TokenProvider) = RetrofitClient(tokenProvider)

    @Provides
    @Singleton
    fun provideSocketManager(tokenProvider: TokenProvider) = SocketManager(tokenProvider)

    @Provides
    @Singleton
    fun providePresenceService(socketManager: SocketManager, userDao: com.mistymessenger.core.db.dao.UserDao) =
        PresenceService(socketManager, userDao)

    @Provides
    @Singleton
    fun provideWorkerScheduler(@ApplicationContext ctx: Context) = WorkerScheduler(ctx)
}
