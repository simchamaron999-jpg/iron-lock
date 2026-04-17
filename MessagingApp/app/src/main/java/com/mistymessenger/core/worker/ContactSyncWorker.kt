package com.mistymessenger.core.worker

import android.content.Context
import android.provider.ContactsContract
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mistymessenger.core.db.dao.ContactDao
import com.mistymessenger.core.db.entity.ContactEntity
import com.mistymessenger.core.network.RetrofitClient
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class SyncContactsRequest(val phones: List<String>)

@Serializable
data class SyncContactsResponse(
    val id: String,
    val phone: String,
    val name: String,
    val avatarUrl: String = "",
    val bio: String = ""
)

interface ContactApiService {
    @POST("users/sync-contacts")
    suspend fun syncContacts(@Body req: SyncContactsRequest): List<SyncContactsResponse>
}

@HiltWorker
class ContactSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val contactDao: ContactDao,
    private val retrofitClient: RetrofitClient
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val devicePhones = readDeviceContacts()
            if (devicePhones.isEmpty()) return Result.success()

            val api = retrofitClient.retrofit.create(ContactApiService::class.java)
            val registered = api.syncContacts(SyncContactsRequest(devicePhones.map { it.second }))

            // Build a phone→name map from device contacts for display names
            val phoneToName = devicePhones.toMap()

            val entities = registered.map { user ->
                val deviceName = phoneToName[user.phone]?.takeIf { it.isNotBlank() } ?: user.name
                ContactEntity(
                    userId = user.id,
                    name = deviceName,
                    phone = user.phone,
                    avatarUrl = user.avatarUrl
                )
            }

            contactDao.insertAll(entities)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    // Returns list of (displayName, normalizedPhone)
    private fun readDeviceContacts(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val cursor = applicationContext.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        ) ?: return result

        cursor.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val name = it.getString(nameIdx) ?: continue
                val rawPhone = it.getString(phoneIdx) ?: continue
                val normalized = rawPhone.replace(Regex("[^+\\d]"), "")
                if (normalized.length >= 7) result.add(name to normalized)
            }
        }
        return result.distinctBy { it.second }
    }
}
