package com.mistymessenger.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.mistymessenger.BuildConfig
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitClient @Inject constructor(
    private val tokenProvider: TokenProvider
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val retrofit: Retrofit by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = tokenProvider.getAccessToken()
                val request = if (token.isNotEmpty()) {
                    chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
                } else chain.request()
                val response = chain.proceed(request)

                // Auto-refresh on 401
                if (response.code == 401 && token.isNotEmpty()) {
                    response.close()
                    val newToken = runBlocking { refreshAccessToken() }
                    if (newToken != null) {
                        val retried = chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer $newToken")
                            .build()
                        chain.proceed(retried)
                    } else response
                } else response
            }
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL + "/api/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private suspend fun refreshAccessToken(): String? {
        return try {
            val refreshToken = tokenProvider.getRefreshToken()
            if (refreshToken.isEmpty()) return null

            // Raw OkHttp call to avoid recursive interceptor
            val rawClient = OkHttpClient()
            val body = okhttp3.RequestBody.create(
                "application/json".toMediaType(),
                """{"refreshToken":"$refreshToken"}"""
            )
            val request = Request.Builder()
                .url("${BuildConfig.BASE_URL}/api/auth/refresh")
                .post(body)
                .build()
            val response = rawClient.newCall(request).execute()
            if (response.isSuccessful) {
                val text = response.body?.string() ?: return null
                val parsed = json.decodeFromString<RefreshResponse>(text)
                tokenProvider.saveAccessToken(parsed.accessToken)
                parsed.accessToken
            } else null
        } catch (e: Exception) { null }
    }
}

@Serializable
private data class RefreshResponse(val accessToken: String)
