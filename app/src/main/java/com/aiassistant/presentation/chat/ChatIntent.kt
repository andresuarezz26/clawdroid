package com.aiassistant.presentation.chat

sealed interface ChatIntent {

  data class UpdateInput(val input: String) : ChatIntent
  object RunCommand : ChatIntent
}