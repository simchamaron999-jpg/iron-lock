package com.mistymessenger.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mistymessenger.core.db.converter.Converters
import com.mistymessenger.core.db.dao.*
import com.mistymessenger.core.db.entity.*

@Database(
    entities = [
        UserEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        ContactEntity::class,
        StatusEntity::class,
        CallLogEntity::class,
        AccountEntity::class,
        AutoReplyEntity::class,
        ScheduledMessageEntity::class,
        ChatSettingsEntity::class,
        StarredMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao
    abstract fun statusDao(): StatusDao
    abstract fun callLogDao(): CallLogDao
    abstract fun accountDao(): AccountDao
    abstract fun autoReplyDao(): AutoReplyDao
    abstract fun scheduledMessageDao(): ScheduledMessageDao
    abstract fun chatSettingsDao(): ChatSettingsDao
    abstract fun starredMessageDao(): StarredMessageDao
}
