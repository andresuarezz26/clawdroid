package com.aiassistant.presentation.chat

data class ChatState(
  val isLoading: Boolean = false,
  val messages: List<String> = listOf(),
  val inputText: String = "",
  val isExecuting: Boolean = false,)
