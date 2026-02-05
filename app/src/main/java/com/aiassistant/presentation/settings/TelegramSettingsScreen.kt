package com.aiassistant.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: TelegramSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showToken by remember { mutableStateOf(false) }

    LaunchedEffect(state.validationResult) {
        when (val result = state.validationResult) {
            is ValidationResult.Success -> {
                Toast.makeText(context, "Connection successful!", Toast.LENGTH_SHORT).show()
                viewModel.processIntent(TelegramSettingsIntent.DismissValidationResult)
            }
            is ValidationResult.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                viewModel.processIntent(TelegramSettingsIntent.DismissValidationResult)
            }
            null -> { /* No action */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Telegram Bot Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Token Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Bot Token",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Create a bot with @BotFather on Telegram and paste the token here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = state.token,
                            onValueChange = { viewModel.processIntent(TelegramSettingsIntent.UpdateToken(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Bot Token") },
                            placeholder = { Text("123456789:ABCdefGHIjklMNOpqrsTUVwxyz") },
                            singleLine = true,
                            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { showToken = !showToken }) {
                                        Icon(
                                            if (showToken) Icons.Default.Close else Icons.Default.Edit,
                                            contentDescription = if (showToken) "Hide" else "Show"
                                        )
                                    }
                                    if (state.isTokenSet) {
                                        IconButton(onClick = { viewModel.processIntent(TelegramSettingsIntent.ClearToken) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Clear token"
                                            )
                                        }
                                    }
                                }
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.processIntent(TelegramSettingsIntent.SaveToken) },
                                enabled = state.token.isNotBlank() && state.token != "********"
                            ) {
                                Text("Save Token")
                            }

                            OutlinedButton(
                                onClick = { viewModel.processIntent(TelegramSettingsIntent.TestConnection) },
                                enabled = state.isTokenSet && !state.isValidating
                            ) {
                                if (state.isValidating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .width(16.dp)
                                            .height(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("Test Connection")
                            }
                        }
                    }
                }
            }

            // Bot Control Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Bot Status",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (state.isBotRunning) "Bot is running" else "Bot is stopped",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (state.isBotRunning) {
                                        "Listening for messages..."
                                    } else {
                                        "Enable to start receiving messages"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = state.isBotRunning,
                                onCheckedChange = { viewModel.processIntent(TelegramSettingsIntent.ToggleBot) },
                                enabled = state.isTokenSet
                            )
                        }
                    }
                }
            }

            // Conversations Section
            if (state.conversations.isNotEmpty()) {
                item {
                    Text(
                        text = "Active Conversations",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(state.conversations) { conversation ->
                    ConversationItem(conversation)
                }
            }

            // Instructions Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "How to Set Up",
                            style = MaterialTheme.typography.titleMedium
                        )

                        HorizontalDivider()

                        SetupStep(1, "Open Telegram and search for @BotFather")
                        SetupStep(2, "Send /newbot and follow the instructions")
                        SetupStep(3, "Copy the bot token and paste it above")
                        SetupStep(4, "Enable the bot and start chatting!")
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupStep(number: Int, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ConversationItem(conversation: com.aiassistant.domain.repository.telegram.TelegramConversation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = conversation.firstName ?: conversation.username ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge
                )
                conversation.username?.let {
                    Text(
                        text = "@$it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = formatTimestamp(conversation.lastMessageAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
