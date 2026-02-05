package com.aiassistant.presentation.chat

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aiassistant.presentation.chat.components.ChatBubble
import com.aiassistant.presentation.chat.components.CommandInput
import com.aiassistant.presentation.chat.components.ServiceDisconnectedBanner
import com.aiassistant.presentation.chat.components.StepIndicator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToTelegramSettings: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is ChatSideEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }
                is ChatSideEffect.ScrollToBottom -> {
                    scope.launch {
                        if (state.messages.isNotEmpty()) {
                            listState.animateScrollToItem(state.messages.lastIndex)
                        }
                    }
                }
                is ChatSideEffect.OpenAccessibilitySettings -> {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                is ChatSideEffect.BringToForeground -> {
                    val intent = context.packageManager
                        .getLaunchIntentForPackage(context.packageName)
                        ?.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                    context.startActivity(intent)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clawdroid") },
                actions = {
                    IconButton(onClick = onNavigateToTelegramSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Telegram Settings"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!state.isServiceConnected) {
                ServiceDisconnectedBanner(
                    onClick = { viewModel.processIntent(ChatIntent.ExecuteCommand("")) }
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    ChatBubble(message = message)
                }
            }

            if (state.isExecuting) {
                StepIndicator(
                    currentStep = state.currentStep,
                    stepCount = state.stepCount,
                    onCancel = { viewModel.processIntent(ChatIntent.CancelExecution) }
                )
            }

            CommandInput(
                value = state.inputText,
                onValueChange = { viewModel.processIntent(ChatIntent.UpdateInput(it)) },
                onSend = { viewModel.processIntent(ChatIntent.ExecuteCommand(state.inputText)) },
                enabled = !state.isExecuting
            )
        }
    }
}