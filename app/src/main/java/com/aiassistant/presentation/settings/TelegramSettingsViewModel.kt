package com.aiassistant.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiassistant.data.di.TelegramTokenProvider
import com.aiassistant.domain.repository.telegram.TelegramConversation
import com.aiassistant.domain.repository.telegram.TelegramRepository
import com.aiassistant.domain.usecase.telegram.ValidateTokenUseCase
import com.aiassistant.framework.telegram.TelegramBotService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TelegramSettingsState(
    val token: String = "",
    val isTokenSet: Boolean = false,
    val isBotRunning: Boolean = false,
    val isValidating: Boolean = false,
    val validationResult: ValidationResult? = null,
    val conversations: List<TelegramConversation> = emptyList()
)

sealed interface ValidationResult {
    data object Success : ValidationResult
    data class Error(val message: String) : ValidationResult
}

sealed interface TelegramSettingsIntent {
    data class UpdateToken(val token: String) : TelegramSettingsIntent
    data object SaveToken : TelegramSettingsIntent
    data object ClearToken : TelegramSettingsIntent
    data object TestConnection : TelegramSettingsIntent
    data object ToggleBot : TelegramSettingsIntent
    data object DismissValidationResult : TelegramSettingsIntent
}

@HiltViewModel
class TelegramSettingsViewModel @Inject constructor(
    private val tokenProvider: TelegramTokenProvider,
    private val validateTokenUseCase: ValidateTokenUseCase,
    private val telegramRepository: TelegramRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(TelegramSettingsState())
    val state: StateFlow<TelegramSettingsState> = _state.asStateFlow()

    init {
        loadInitialState()
        observeConversations()
    }

    private fun loadInitialState() {
        val hasToken = tokenProvider.hasToken()
        _state.update {
            it.copy(
                isTokenSet = hasToken,
                token = if (hasToken) "********" else ""
            )
        }
    }

    private fun observeConversations() {
        viewModelScope.launch {
            telegramRepository.observeConversations().collect { conversations ->
                _state.update { it.copy(conversations = conversations) }
            }
        }
    }

    fun processIntent(intent: TelegramSettingsIntent) {
        when (intent) {
            is TelegramSettingsIntent.UpdateToken -> {
                _state.update { it.copy(token = intent.token) }
            }
            is TelegramSettingsIntent.SaveToken -> saveToken()
            is TelegramSettingsIntent.ClearToken -> clearToken()
            is TelegramSettingsIntent.TestConnection -> testConnection()
            is TelegramSettingsIntent.ToggleBot -> toggleBot()
            is TelegramSettingsIntent.DismissValidationResult -> {
                _state.update { it.copy(validationResult = null) }
            }
        }
    }

    private fun saveToken() {
        val token = _state.value.token.trim()
        if (token.isBlank() || token == "********") {
            _state.update { it.copy(validationResult = ValidationResult.Error("Please enter a valid token")) }
            return
        }

        tokenProvider.setToken(token)
        _state.update {
            it.copy(
                isTokenSet = true,
                token = "********",
                validationResult = ValidationResult.Success
            )
        }
    }

    private fun clearToken() {
        if (_state.value.isBotRunning) {
            TelegramBotService.stop(context)
        }
        tokenProvider.clearToken()
        _state.update {
            it.copy(
                isTokenSet = false,
                token = "",
                isBotRunning = false,
                validationResult = null
            )
        }
    }

    private fun testConnection() {
        viewModelScope.launch {
            _state.update { it.copy(isValidating = true, validationResult = null) }

            val isValid = validateTokenUseCase()

            _state.update {
                it.copy(
                    isValidating = false,
                    validationResult = if (isValid) {
                        ValidationResult.Success
                    } else {
                        ValidationResult.Error("Invalid token or connection failed")
                    }
                )
            }
        }
    }

    private fun toggleBot() {
        val isRunning = _state.value.isBotRunning

        if (isRunning) {
            TelegramBotService.stop(context)
            _state.update { it.copy(isBotRunning = false) }
        } else {
            if (!_state.value.isTokenSet) {
                _state.update { it.copy(validationResult = ValidationResult.Error("Please set a bot token first")) }
                return
            }
            TelegramBotService.start(context)
            _state.update { it.copy(isBotRunning = true) }
        }
    }
}
