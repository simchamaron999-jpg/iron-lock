package com.mistymessenger.core.db.dao

import androidx.room.*
import com.mistymessenger.core.db.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: String): Flow<UserEntity?>

    @Query("UPDATE users SET isOnline = :online, lastSeen = :lastSeen WHERE id = :id")
    suspend fun updatePresence(id: String, online: Boolean, lastSeen: Long)

    @Query("SELECT * FROM users WHERE id IN (:ids)")
    suspend fun getUsersByIds(ids: List<String>): List<UserEntity>
}

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE name LIKE '%' || :q || '%' OR phone LIKE '%' || :q || '%'")
    fun searchContacts(q: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE userId = :id")
    suspend fun getContactById(id: String): ContactEntity?

    @Query("UPDATE contacts SET isBlocked = :blocked WHERE userId = :id")
    suspend fun setBlocked(id: String, blocked: Boolean)

    @Query("UPDATE contacts SET customNotificationSoundUri = :uri WHERE userId = :id")
    suspend fun setNotificationSound(id: String, uri: String)
}

@Dao
interface StatusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(statuses: List<StatusEntity>)

    @Query("SELECT * FROM statuses WHERE expiresAt > :now ORDER BY createdAt DESC")
    fun getActiveStatuses(now: Long = System.currentTimeMillis()): Flow<List<StatusEntity>>

    @Query("SELECT * FROM statuses WHERE userId = :userId AND expiresAt > :now ORDER BY createdAt ASC")
    fun getStatusesForUser(userId: String, now: Long = System.currentTimeMillis()): Flow<List<StatusEntity>>

    @Query("DELETE FROM statuses WHERE expiresAt < :now")
    suspend fun deleteExpired(now: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(status: StatusEntity)
}

@Dao
interface CallLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(call: CallLogEntity)

    @Query("SELECT * FROM call_logs ORDER BY startedAt DESC")
    fun getAllCalls(): Flow<List<CallLogEntity>>

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 LIMIT 1")
    fun getActiveAccount(): Flow<AccountEntity?>

    @Query("UPDATE accounts SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE accounts SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: String)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface AutoReplyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: AutoReplyEntity)

    @Query("SELECT * FROM auto_replies WHERE isEnabled = 1")
    fun getEnabledRules(): Flow<List<AutoReplyEntity>>

    @Delete
    suspend fun delete(rule: AutoReplyEntity)

    @Query("UPDATE auto_replies SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)
}

@Dao
interface ScheduledMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(msg: ScheduledMessageEntity)

    @Query("SELECT * FROM scheduled_messages WHERE status = 'pending' ORDER BY scheduledAt ASC")
    fun getPending(): Flow<List<ScheduledMessageEntity>>

    @Query("UPDATE scheduled_messages SET status = 'sent' WHERE id = :id")
    suspend fun markSent(id: String)

    @Query("UPDATE scheduled_messages SET status = 'cancelled' WHERE id = :id")
    suspend fun cancel(id: String)

    @Query("DELETE FROM scheduled_messages WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ChatSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: ChatSettingsEntity)

    @Query("SELECT * FROM chat_settings WHERE chatId = :chatId")
    fun getSettings(chatId: String): Flow<ChatSettingsEntity?>
}

@Dao
interface StarredMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(starred: StarredMessageEntity)

    @Query("DELETE FROM starred_messages WHERE messageId = :messageId")
    suspend fun delete(messageId: String)

    @Query("SELECT * FROM starred_messages ORDER BY starredAt DESC")
    fun getAll(): Flow<List<StarredMessageEntity>>
}
