package com.aiassistant.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiassistant.agent.LLMProvider
import com.aiassistant.domain.preference.SharedPreferenceDataSource
import com.aiassistant.domain.repository.telegram.ConversationModel
import com.aiassistant.domain.usecase.messages.ObserveConversationUseCase
import com.aiassistant.domain.usecase.telegram.ValidateTelegramTokenUseCase
import com.aiassistant.framework.notification.NotificationListenerServiceState
import com.aiassistant.framework.permission.PermissionManager
import com.aiassistant.framework.telegram.TelegramBotService
import com.aiassistant.framework.telegram.TelegramServiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TelegramSettingsState(
  val token: String = "",
  val isTokenSet: Boolean = false,
  val isTelegramBotRunning: Boolean = false,
  val isValidating: Boolean = false,
  val validationResult: ValidationResult? = null,
  val conversations: List<ConversationModel> = emptyList(),
  val isNotificationListenerEnabled: Boolean = false,
  val isNotificationForwardingEnabled: Boolean = false,
  val openAiKey: String = "",
  val anthropicKey: String = "",
  val googleKey: String = "",
  val isOpenAiKeySet: Boolean = false,
  val isAnthropicKeySet: Boolean = false,
  val isGoogleKeySet: Boolean = false
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
  data object CheckNotificationListenerStatus : TelegramSettingsIntent
  data object OpenNotificationListenerSettings : TelegramSettingsIntent
  data object ToggleNotificationForwarding : TelegramSettingsIntent
  data class UpdateApiKey(val provider: LLMProvider, val key: String) : TelegramSettingsIntent
  data class SaveApiKey(val provider: LLMProvider) : TelegramSettingsIntent
  data class ClearApiKey(val provider: LLMProvider) : TelegramSettingsIntent
}

@HiltViewModel
class TelegramSettingsViewModel @Inject constructor(
  private val preferences: SharedPreferenceDataSource,
  private val validateTelegramTokenUseCase: ValidateTelegramTokenUseCase,
  private val observeConversationUseCase: ObserveConversationUseCase,
  @ApplicationContext private val context: Context,
  private var serviceState: TelegramServiceState,
  private val permissionManager: PermissionManager,
  private val notificationListenerServiceState: NotificationListenerServiceState,
) : ViewModel() {

  private val _state = MutableStateFlow(TelegramSettingsState())
  val state: StateFlow<TelegramSettingsState> = _state.asStateFlow()

  init {
    loadInitialState()
    observeConversations()
  }

  private fun loadInitialState() {
    viewModelScope.launch {
      val hasToken = preferences.hasToken()
      val forwardingEnabled = preferences.getNotificationForwardingEnabled()
      val hasOpenAi = preferences.hasApiKey(LLMProvider.OPENAI)
      val hasAnthropic = preferences.hasApiKey(LLMProvider.ANTHROPIC)
      val hasGoogle = preferences.hasApiKey(LLMProvider.GOOGLE)
      _state.update {
        it.copy(
          isTokenSet = hasToken,
          token = if (hasToken) "********" else "",
          isNotificationForwardingEnabled = forwardingEnabled,
          isOpenAiKeySet = hasOpenAi,
          openAiKey = if (hasOpenAi) "********" else "",
          isAnthropicKeySet = hasAnthropic,
          anthropicKey = if (hasAnthropic) "********" else "",
          isGoogleKeySet = hasGoogle,
          googleKey = if (hasGoogle) "********" else ""
        )
      }

      serviceState.isRunning.collect { running ->
        _state.update { it.copy(isTelegramBotRunning = running) }
      }
    }

    viewModelScope.launch {
      notificationListenerServiceState.isConnected.collect { connected ->
        _state.update { it.copy(isNotificationListenerEnabled = connected) }
      }
    }
  }

  private fun observeConversations() {
    viewModelScope.launch {
      observeConversationUseCase().collect { conversations ->
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
      is TelegramSettingsIntent.CheckNotificationListenerStatus -> checkNotificationListenerStatus()
      is TelegramSettingsIntent.OpenNotificationListenerSettings -> openNotificationListenerSettings()
      is TelegramSettingsIntent.ToggleNotificationForwarding -> toggleNotificationForwarding()
      is TelegramSettingsIntent.UpdateApiKey -> updateApiKey(intent.provider, intent.key)
      is TelegramSettingsIntent.SaveApiKey -> saveApiKey(intent.provider)
      is TelegramSettingsIntent.ClearApiKey -> clearApiKey(intent.provider)
    }
  }

  private fun saveToken() {
    val token = _state.value.token.trim()
    if (token.isBlank() || token == "********") {
      _state.update { it.copy(validationResult = ValidationResult.Error("Please enter a valid token")) }
      return
    }

    preferences.setToken(token)
    _state.update {
      it.copy(
        isTokenSet = true,
        token = "********",
        validationResult = ValidationResult.Success
      )
    }
  }

  private fun clearToken() {
    if (_state.value.isTelegramBotRunning) {
      TelegramBotService.stop(context)
    }
    preferences.clearToken()
    _state.update {
      it.copy(
        isTokenSet = false,
        token = "",
        isTelegramBotRunning = false,
        validationResult = null
      )
    }
  }

  private fun testConnection() {

    viewModelScope.launch(Dispatchers.Main) {
      _state.update { it.copy(isValidating = true, validationResult = null) }

      val isValid = withContext(Dispatchers.IO) {
        validateTelegramTokenUseCase()
      }

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
    val isRunning = _state.value.isTelegramBotRunning
    if (isRunning) {
      TelegramBotService.stop(context)
      _state.update { it.copy(isTelegramBotRunning = false) }
    } else {
      if (!_state.value.isTokenSet) {
        _state.update { it.copy(validationResult = ValidationResult.Error("Please set a bot token first")) }
      } else {
        TelegramBotService.start(context)
        _state.update { it.copy(isTelegramBotRunning = true) }
      }
    }
  }

  private fun checkNotificationListenerStatus() {
    val enabled = permissionManager.isNotificationListenerEnabled()
    _state.update { it.copy(isNotificationListenerEnabled = enabled) }
  }

  private fun openNotificationListenerSettings() {
    permissionManager.openNotificationListenerSettings(context)
  }

  private fun toggleNotificationForwarding() {
    val newValue = !_state.value.isNotificationForwardingEnabled
    preferences.setNotificationForwardingEnabled(newValue)
    _state.update { it.copy(isNotificationForwardingEnabled = newValue) }
  }

  private fun updateApiKey(provider: LLMProvider, key: String) {
    _state.update {
      when (provider) {
        LLMProvider.OPENAI -> it.copy(openAiKey = key)
        LLMProvider.ANTHROPIC -> it.copy(anthropicKey = key)
        LLMProvider.GOOGLE -> it.copy(googleKey = key)
      }
    }
  }

  private fun saveApiKey(provider: LLMProvider) {
    val key = when (provider) {
      LLMProvider.OPENAI -> _state.value.openAiKey
      LLMProvider.ANTHROPIC -> _state.value.anthropicKey
      LLMProvider.GOOGLE -> _state.value.googleKey
    }.trim()

    if (key.isBlank() || key == "********") {
      _state.update { it.copy(validationResult = ValidationResult.Error("Please enter a valid API key")) }
      return
    }

    preferences.setApiKey(provider, key)
    _state.update {
      when (provider) {
        LLMProvider.OPENAI -> it.copy(isOpenAiKeySet = true, openAiKey = "********")
        LLMProvider.ANTHROPIC -> it.copy(isAnthropicKeySet = true, anthropicKey = "********")
        LLMProvider.GOOGLE -> it.copy(isGoogleKeySet = true, googleKey = "********")
      }
    }
  }

  private fun clearApiKey(provider: LLMProvider) {
    preferences.clearApiKey(provider)
    _state.update {
      when (provider) {
        LLMProvider.OPENAI -> it.copy(isOpenAiKeySet = false, openAiKey = "")
        LLMProvider.ANTHROPIC -> it.copy(isAnthropicKeySet = false, anthropicKey = "")
        LLMProvider.GOOGLE -> it.copy(isGoogleKeySet = false, googleKey = "")
      }
    }
  }
}
