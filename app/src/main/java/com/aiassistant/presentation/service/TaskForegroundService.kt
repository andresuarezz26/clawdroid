package com.aiassistant.presentation.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aiassistant.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// framework/service/TaskForegroundService.kt
class TaskForegroundService : Service() {

  companion object {
    private const val CHANNEL_ID = "task_execution_channel"
    private const val NOTIFICATION_ID = 1001
    const val ACTION_START = "com.aiassistant.START_TASK"
    const val ACTION_STOP = "com.aiassistant.STOP_TASK"
    const val EXTRA_COMMAND = "extra_command"

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun start(context: Context, command: String) {
      val intent = Intent(context, TaskForegroundService::class.java).apply {
        action = ACTION_START
        putExtra(EXTRA_COMMAND, command)
      }
      ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
      val intent = Intent(context, TaskForegroundService::class.java).apply {
        action = ACTION_STOP
      }
      context.startService(intent)
    }
  }

  private val _currentStep = MutableStateFlow("")
  val currentStep: StateFlow<String> = _currentStep.asStateFlow()

  @RequiresApi(Build.VERSION_CODES.O)
  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> {
        val command = intent.getStringExtra(EXTRA_COMMAND) ?: return START_NOT_STICKY
        startForeground(NOTIFICATION_ID, buildNotification("Executing: $command"))
        _isRunning.value = true
      }
      ACTION_STOP -> {
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
      }
    }
    return START_NOT_STICKY
  }

  fun updateNotification(stepDescription: String) {
    _currentStep.value = stepDescription
    val manager = getSystemService(NotificationManager::class.java)
    manager.notify(NOTIFICATION_ID, buildNotification(stepDescription))
  }

  private fun buildNotification(content: String): Notification {
    // Tapping notification opens the app
    val openIntent = packageManager.getLaunchIntentForPackage(packageName)
    val openPending = PendingIntent.getActivity(
      this, 0, openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Stop button in notification
    val stopIntent = Intent(this, TaskForegroundService::class.java).apply {
      action = ACTION_STOP
    }
    val stopPending = PendingIntent.getService(
      this, 1, stopIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Clawdroid")
      .setContentText(content)
      //.setSmallIcon(R.drawable.ic_notification)  // your app icon
      //.setOngoing(true)
      .setContentIntent(openPending)
      .addAction(R.drawable.ic_launcher_background, "Stop", stopPending)
      .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
      .build()
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createNotificationChannel() {
    val channel = NotificationChannel(
      CHANNEL_ID,
      "Task Execution",
      NotificationManager.IMPORTANCE_LOW    // low = no sound, just persistent icon
    ).apply {
      description = "Shows progress while automating tasks"
    }
    getSystemService(NotificationManager::class.java)
      .createNotificationChannel(channel)
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    _isRunning.value = false
    super.onDestroy()
  }
}