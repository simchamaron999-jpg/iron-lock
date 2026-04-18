package com.mistymessenger.chat.repository

import android.net.Uri
import com.mistymessenger.core.db.dao.ChatDao
import com.mistymessenger.core.db.dao.ContactDao
import com.mistymessenger.core.db.entity.ChatEntity
import com.mistymessenger.core.media.S3UploadService
import com.mistymessenger.core.media.UploadState
import com.mistymessenger.core.network.RetrofitClient
import com.mistymessenger.core.network.TokenProvider
import retrofit2.http.*
import javax.inject.Inject
import javax.inject.Singleton

// ── API models ─────────���──────────────────────────��─────────────────────────

data class CreateGroupRequest(
    val name: String,
    val memberIds: List<String>,
    val avatarUrl: String = ""
)

data class GroupResponse(
    val _id: String,
    val name: String,
    val avatarUrl: String?,
    val memberIds: List<String>,
    val adminIds: List<String>,
    val inviteLink: String?,
    val description: String?
)

data class UpdateGroupRequest(
    val name: String? = null,
    val description: String? = null,
    val avatarUrl: String? = null
)

data class MemberActionRequest(val userId: String)
data class InviteLinkResponse(val link: String)

interface GroupApiService {
    @POST("chats/group")
    suspend fun createGroup(@Body req: CreateGroupRequest): GroupResponse

    @GET("chats/{chatId}/group")
    suspend fun getGroupInfo(@Path("chatId") chatId: String): GroupResponse

    @PATCH("chats/{chatId}/group")
    suspend fun updateGroup(@Path("chatId") chatId: String, @Body req: UpdateGroupRequest): GroupResponse

    @POST("chats/{chatId}/group/members")
    suspend fun addMember(@Path("chatId") chatId: String, @Body req: MemberActionRequest): GroupResponse

    @DELETE("chats/{chatId}/group/members/{userId}")
    suspend fun removeMember(@Path("chatId") chatId: String, @Path("userId") userId: String): GroupResponse

    @POST("chats/{chatId}/group/admins")
    suspend fun promoteAdmin(@Path("chatId") chatId: String, @Body req: MemberActionRequest): GroupResponse

    @DELETE("chats/{chatId}/group/admins/{userId}")
    suspend fun demoteAdmin(@Path("chatId") chatId: String, @Path("userId") userId: String): GroupResponse

    @POST("chats/{chatId}/group/invite")
    suspend fun generateInviteLink(@Path("chatId") chatId: String): InviteLinkResponse

    @DELETE("chats/{chatId}/group/leave")
    suspend fun leaveGroup(@Path("chatId") chatId: String)
}

@Singleton
class GroupRepository @Inject constructor(
    private val retrofitClient: RetrofitClient,
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val s3UploadService: S3UploadService,
    private val tokenProvider: TokenProvider
) {
    private val api by lazy { retrofitClient.retrofit.create(GroupApiService::class.java) }

    suspend fun createGroup(name: String, memberIds: List<String>, avatarUri: Uri?): ChatEntity {
        var avatarUrl = ""
        if (avatarUri != null) {
            s3UploadService.uploadUri(avatarUri, "image/jpeg").collect { state ->
                if (state is UploadState.Success) avatarUrl = state.result.url
            }
        }
        val resp = api.createGroup(CreateGroupRequest(name, memberIds + tokenProvider.getUserId(), avatarUrl))
        val entity = resp.toEntity()
        chatDao.insertAll(listOf(entity))
        return entity
    }

    suspend fun getGroupInfo(chatId: String): GroupResponse = api.getGroupInfo(chatId)

    suspend fun updateGroup(chatId: String, name: String?, description: String?, avatarUri: Uri?): GroupResponse {
        var avatarUrl: String? = null
        if (avatarUri != null) {
            s3UploadService.uploadUri(avatarUri, "image/jpeg").collect { state ->
                if (state is UploadState.Success) avatarUrl = state.result.url
            }
        }
        val resp = api.updateGroup(chatId, UpdateGroupRequest(name, description, avatarUrl))
        chatDao.updateGroupInfo(chatId, resp.name, resp.avatarUrl ?: "")
        return resp
    }

    suspend fun addMember(chatId: String, userId: String) = api.addMember(chatId, MemberActionRequest(userId))
    suspend fun removeMember(chatId: String, userId: String) = api.removeMember(chatId, userId)
    suspend fun promoteAdmin(chatId: String, userId: String) = api.promoteAdmin(chatId, MemberActionRequest(userId))
    suspend fun demoteAdmin(chatId: String, userId: String) = api.demoteAdmin(chatId, userId)
    suspend fun generateInviteLink(chatId: String) = api.generateInviteLink(chatId).link
    suspend fun leaveGroup(chatId: String) {
        api.leaveGroup(chatId)
        chatDao.setArchived(chatId, true)
    }

    private fun GroupResponse.toEntity() = ChatEntity(
        id = _id,
        name = name,
        avatarUrl = avatarUrl ?: "",
        type = "group",
        memberIds = memberIds,
        adminIds = adminIds,
        description = description ?: "",
        inviteLink = inviteLink ?: "",
        lastMessageText = "",
        lastMessageAt = System.currentTimeMillis()
    )
}
