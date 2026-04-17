package com.mistymessenger.core.db.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.mistymessenger.core.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId AND isDeletedForMe = 0 ORDER BY createdAt DESC")
    fun getMessagesForChat(chatId: String): PagingSource<Int, MessageEntity>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt DESC LIMIT 1")
    fun getLastMessage(chatId: String): Flow<MessageEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE messages SET isDeletedForMe = 1 WHERE id = :id")
    suspend fun deleteForMe(id: String)

    @Query("UPDATE messages SET isDeletedForEveryone = 1, content = '' WHERE id = :id")
    suspend fun deleteForEveryone(id: String)

    // Anti-delete: mark as deleted by sender but keep content visible
    @Query("UPDATE messages SET deletedByOther = 1 WHERE id = :id")
    suspend fun markDeletedByOther(id: String)

    @Query("UPDATE messages SET isStarred = :starred WHERE id = :id")
    suspend fun setStarred(id: String, starred: Boolean)

    @Query("SELECT * FROM messages WHERE isStarred = 1 ORDER BY createdAt DESC")
    fun getStarredMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' AND isDeletedForMe = 0 ORDER BY createdAt DESC LIMIT 50")
    suspend fun searchMessages(query: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: String): MessageEntity?

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteAllInChat(chatId: String)

    @Query("UPDATE messages SET reactions = :reactions WHERE id = :id")
    suspend fun updateReactions(id: String, reactions: List<String>)
}
