package com.aiassistant.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aiassistant.presentation.chat.ChatScreen

@Composable
fun NavigationStack() {
  val navController = rememberNavController()

  NavHost(navController = navController, startDestination = NavigationScreen.Main) {
    composable<NavigationScreen.Main> {
      ChatScreen()
    }
  }
}