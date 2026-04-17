package com.mistymessenger.auth.repository

import com.mistymessenger.auth.model.AuthApiService
import com.mistymessenger.auth.model.SaveProfileRequest
import com.mistymessenger.auth.model.SendOTPRequest
import com.mistymessenger.auth.model.VerifyOTPRequest
import com.mistymessenger.core.network.RetrofitClient
import com.mistymessenger.core.network.TokenProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    retrofitClient: RetrofitClient,
    private val tokenProvider: TokenProvider
) {
    private val api = retrofitClient.retrofit.create(AuthApiService::class.java)

    suspend fun sendOTP(phone: String): Result<Unit> = runCatching {
        api.sendOTP(SendOTPRequest(phone))
    }

    suspend fun verifyOTP(phone: String, otp: String): Result<Unit> = runCatching {
        val response = api.verifyOTP(VerifyOTPRequest(phone, otp))
        tokenProvider.save(response.accessToken, response.refreshToken, response.userId)
    }

    suspend fun saveProfile(name: String, bio: String): Result<Unit> = runCatching {
        api.saveProfile(SaveProfileRequest(name, bio))
    }
}
