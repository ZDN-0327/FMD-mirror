package de.nulide.findmydevice.net.model

data class PasswordRequest(
    val newSalt64: String,
    val newPasswordHash64: String,
    val newEncMasterKey64: String,
)
