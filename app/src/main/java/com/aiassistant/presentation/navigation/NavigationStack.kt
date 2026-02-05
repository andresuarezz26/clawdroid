package com.aiassistant.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aiassistant.presentation.chat.ChatScreen
import com.aiassistant.presentation.settings.TelegramSettingsScreen

@Composable
fun NavigationStack() {
  val navController = rememberNavController()

  NavHost(navController = navController, startDestination = NavigationScreen.Main) {
    composable<NavigationScreen.Main> {
      ChatScreen(
        onNavigateToTelegramSettings = {
          navController.navigate(NavigationScreen.TelegramSettings)
        }
      )
    }
    composable<NavigationScreen.TelegramSettings> {
      TelegramSettingsScreen(
        onNavigateBack = { navController.popBackStack() }
      )
    }
  }
}