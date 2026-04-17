package com.mistymessenger.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable data class SendOTPRequest(@SerialName("phone") val phone: String)
@Serializable data class VerifyOTPRequest(@SerialName("phone") val phone: String, @SerialName("otp") val otp: String)
@Serializable data class SaveProfileRequest(@SerialName("name") val name: String, @SerialName("bio") val bio: String)
@Serializable data class AuthResponse(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String,
    @SerialName("userId") val userId: String
)

interface AuthApiService {
    @POST("auth/send-otp") suspend fun sendOTP(@Body req: SendOTPRequest)
    @POST("auth/verify-otp") suspend fun verifyOTP(@Body req: VerifyOTPRequest): AuthResponse
    @POST("auth/profile") suspend fun saveProfile(@Body req: SaveProfileRequest)
}
