package com.mistymessenger.core.di

import android.content.Context
import androidx.room.Room
import com.mistymessenger.core.db.AppDatabase
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

// SocketManager, RetrofitClient, PresenceService, WorkerScheduler all use
// @Singleton + @Inject constructor so Hilt binds them automatically — no @Provides needed.
