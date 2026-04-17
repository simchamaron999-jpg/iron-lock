package com.mistymessenger.auth.repository

import com.google.firebase.messaging.FirebaseMessaging
import com.mistymessenger.auth.model.AuthApiService
import com.mistymessenger.auth.model.FcmTokenRequest
import com.mistymessenger.auth.model.SaveProfileRequest
import com.mistymessenger.auth.model.SendOTPRequest
import com.mistymessenger.auth.model.VerifyOTPRequest
import com.mistymessenger.core.network.RetrofitClient
import com.mistymessenger.core.network.SocketManager
import com.mistymessenger.core.network.TokenProvider
import com.mistymessenger.core.worker.WorkerScheduler
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    retrofitClient: RetrofitClient,
    private val tokenProvider: TokenProvider,
    private val socketManager: SocketManager,
    private val workerScheduler: WorkerScheduler
) {
    private val api = retrofitClient.retrofit.create(AuthApiService::class.java)

    suspend fun sendOTP(phone: String): Result<Unit> = runCatching {
        api.sendOTP(SendOTPRequest(phone))
    }

    suspend fun verifyOTP(phone: String, otp: String): Result<Unit> = runCatching {
        val response = api.verifyOTP(VerifyOTPRequest(phone, otp))
        tokenProvider.save(response.accessToken, response.refreshToken, response.userId)
        // Connect socket immediately after auth
        socketManager.connect()
        // Register FCM token
        runCatching {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            api.registerFcmToken(FcmTokenRequest(fcmToken))
        }
    }

    suspend fun saveProfile(name: String, bio: String, avatarUrl: String = ""): Result<Unit> = runCatching {
        api.saveProfile(SaveProfileRequest(name, bio, avatarUrl))
        // Kick off contact sync after profile is set up
        workerScheduler.scheduleContactSync()
        workerScheduler.scheduleStatusCleanup()
    }

    suspend fun logout() {
        tokenProvider.clear()
        socketManager.disconnect()
    }
}
