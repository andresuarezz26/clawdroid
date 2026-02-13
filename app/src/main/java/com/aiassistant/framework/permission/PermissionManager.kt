package com.aiassistant.framework.permission

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    private var permissionLauncher: ActivityResultLauncher<String>? = null
    private var pendingCallback: ((Boolean) -> Unit)? = null

    /**
     * Call from Activity.onCreate() BEFORE setContent to register the launcher.
     */
    fun registerActivity(activity: ComponentActivity) {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            pendingCallback?.invoke(isGranted)
            pendingCallback = null
        }
    }

    fun unregisterActivity() {
        permissionLauncher = null
        pendingCallback = null
    }

    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(appContext, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request a runtime permission. Returns true if granted, false if denied or no Activity is available.
     */
    suspend fun requestPermission(permission: String): Boolean {
        if (hasPermission(permission)) return true

        val launcher = permissionLauncher ?: return false

        return suspendCancellableCoroutine { continuation ->
            pendingCallback = { granted ->
                if (continuation.isActive) {
                    continuation.resume(granted)
                }
            }
            continuation.invokeOnCancellation {
                pendingCallback = null
            }
            launcher.launch(permission)
        }
    }

    fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            appContext.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return enabledListeners.contains(appContext.packageName)
    }

    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
