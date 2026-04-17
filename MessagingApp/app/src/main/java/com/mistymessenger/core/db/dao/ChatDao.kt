package com.mistymessenger.core.db.dao

import androidx.room.*
import com.mistymessenger.core.db.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats WHERE isArchived = 0 ORDER BY isPinned DESC, lastMessageAt DESC")
    fun getActiveChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isArchived = 1 ORDER BY lastMessageAt DESC")
    fun getArchivedChats(): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chats: List<ChatEntity>)

    @Query("SELECT * FROM chats WHERE id = :id")
    fun getChatById(id: String): Flow<ChatEntity?>

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun clearUnread(chatId: String)

    @Query("UPDATE chats SET lastMessageText = :text, lastMessageAt = :at WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, text: String, at: Long)

    @Query("UPDATE chats SET isMuted = :muted WHERE id = :chatId")
    suspend fun setMuted(chatId: String, muted: Boolean)

    @Query("UPDATE chats SET isArchived = :archived WHERE id = :chatId")
    suspend fun setArchived(chatId: String, archived: Boolean)

    @Query("UPDATE chats SET isPinned = :pinned WHERE id = :chatId")
    suspend fun setPinned(chatId: String, pinned: Boolean)

    @Query("SELECT * FROM chats WHERE name LIKE '%' || :query || '%'")
    suspend fun searchChats(query: String): List<ChatEntity>

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)

    @Query("UPDATE chats SET name = :name, avatarUrl = :avatarUrl WHERE id = :chatId")
    suspend fun updateGroupInfo(chatId: String, name: String, avatarUrl: String)

    @Query("UPDATE chats SET adminIds = :adminIds WHERE id = :chatId")
    suspend fun updateAdmins(chatId: String, adminIds: List<String>)

    @Query("UPDATE chats SET memberIds = :memberIds WHERE id = :chatId")
    suspend fun updateMembers(chatId: String, memberIds: List<String>)

    @Query("UPDATE chats SET inviteLink = :link WHERE id = :chatId")
    suspend fun updateInviteLink(chatId: String, link: String)
}
