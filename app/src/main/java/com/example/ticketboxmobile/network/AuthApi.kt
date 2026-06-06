package com.example.ticketboxmobile.network

import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String?,
    val role: String?,
    val name: String?,
    val error: String?
)

interface AuthApi {
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): retrofit2.Response<LoginResponse>
}
