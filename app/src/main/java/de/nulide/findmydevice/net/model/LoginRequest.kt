package de.nulide.findmydevice.net.model

import androidx.annotation.Keep

@Keep
data class LoginRequest(
    val userName: String,
    val passwordHash64: String,
    val sessionDurationSeconds: Int,
)
