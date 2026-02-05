package com.aiassistant.data.remote.telegram.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    @SerialName("message_id")
    val messageId: Long,
    @SerialName("from")
    val from: User? = null,
    @SerialName("chat")
    val chat: Chat,
    @SerialName("date")
    val date: Long,
    @SerialName("text")
    val text: String? = null,
    @SerialName("reply_to_message")
    val replyToMessage: Message? = null
)
