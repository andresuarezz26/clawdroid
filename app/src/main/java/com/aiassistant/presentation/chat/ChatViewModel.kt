package com.aiassistant.presentation.chat

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {

  private val _state = MutableStateFlow(ChatState())
  val state: StateFlow<ChatState> = _state.asStateFlow()

  fun processIntent(intent: ChatIntent) {
    when (intent) {
      is ChatIntent.UpdateInput -> {
        _state.update {
          it.copy(inputText = intent.input)
        }
        // Handle load data intent
      }
      ChatIntent.RunCommand -> {

      }
    }
  }
}