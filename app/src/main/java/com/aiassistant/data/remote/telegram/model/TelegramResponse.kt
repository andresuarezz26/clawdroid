package com.aiassistant.data.remote.telegram.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TelegramResponse<T>(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("result")
    val result: T? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("error_code")
    val errorCode: Int? = null
)
