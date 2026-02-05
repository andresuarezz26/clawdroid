package com.aiassistant.presentation.navigation

import kotlinx.serialization.Serializable

sealed class NavigationScreen {

  @Serializable
  object Main: NavigationScreen()

  @Serializable
  object TelegramSettings: NavigationScreen()
}