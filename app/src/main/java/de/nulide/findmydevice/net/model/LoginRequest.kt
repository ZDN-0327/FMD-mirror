package de.nulide.findmydevice.net.model

data class LoginRequest (
    val userName: String,
    val passwordHash64: String,
    val sessionDurationSeconds: Int,
)
