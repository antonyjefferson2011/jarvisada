package com.jarvis.mobile.data.remote

data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
)

open class BackendException(message: String) : Exception(message)
