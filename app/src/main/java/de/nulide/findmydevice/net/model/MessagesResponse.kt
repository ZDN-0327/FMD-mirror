package de.nulide.findmydevice.net.model

import androidx.annotation.Keep

@Keep
data class MessagesResponse(
    val messages: List<ServerMessage>,
)
