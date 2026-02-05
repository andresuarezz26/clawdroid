package com.aiassistant.data.remote.telegram.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Update(
    @SerialName("update_id")
    val updateId: Long,
    @SerialName("message")
    val message: Message? = null,
    @SerialName("edited_message")
    val editedMessage: Message? = null
)
