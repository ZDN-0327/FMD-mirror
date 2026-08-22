package de.nulide.findmydevice.net.model

data class EncryptedItem(
    val clientItemIdHex: String,
    val unixMillis: Long,
    val ciphertext64: String,
)
