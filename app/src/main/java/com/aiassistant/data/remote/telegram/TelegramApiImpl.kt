package com.aiassistant.data.remote.telegram

import com.aiassistant.data.di.TelegramTokenProvider
import com.aiassistant.data.remote.telegram.model.Message
import com.aiassistant.data.remote.telegram.model.TelegramResponse
import com.aiassistant.data.remote.telegram.model.Update
import com.aiassistant.data.remote.telegram.model.User
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramApiImpl @Inject constructor(
    private val tokenProvider: TelegramTokenProvider
) : TelegramApi {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 35_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 35_000
            }
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
        }
    }

    private fun getBaseUrl(): String {
        val token = tokenProvider.getToken()
            ?: throw IllegalStateException("Telegram bot token not configured")
        return "https://api.telegram.org/bot$token"
    }

    override suspend fun getUpdates(offset: Long?, timeout: Int): List<Update> {
        val response: TelegramResponse<List<Update>> = client.get("${getBaseUrl()}/getUpdates") {
            parameter("timeout", timeout)
            offset?.let { parameter("offset", it) }
        }.body()

        if (!response.ok) {
            throw TelegramApiException(response.description ?: "Unknown error", response.errorCode)
        }

        return response.result ?: emptyList()
    }

    override suspend fun sendMessage(chatId: Long, text: String): Message {
        val response: TelegramResponse<Message> = client.post("${getBaseUrl()}/sendMessage") {
            setBody(SendMessageRequest(chat_id = chatId, text = text))
        }.body()

        if (!response.ok) {
            throw TelegramApiException(response.description ?: "Unknown error", response.errorCode)
        }

        return response.result ?: throw TelegramApiException("No message returned", null)
    }

    override suspend fun getMe(): User {
        val response: TelegramResponse<User> = client.get("${getBaseUrl()}/getMe").body()

        if (!response.ok) {
            throw TelegramApiException(response.description ?: "Unknown error", response.errorCode)
        }

        return response.result ?: throw TelegramApiException("No user returned", null)
    }
}

@Serializable
private data class SendMessageRequest(
    val chat_id: Long,
    val text: String
)

class TelegramApiException(
    message: String,
    val errorCode: Int?
) : Exception(message)
