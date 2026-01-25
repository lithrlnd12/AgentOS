package com.agentos.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.RemoteViews
import com.agentos.R
import com.agentos.service.FloatingAgentService
import com.agentos.ui.permissions.PermissionActivity

/**
 * Home screen widget providing quick access to AgentOS.
 * Features search bar with text and voice activation.
 */
class AgentOSWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Called when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Called when the last widget is removed
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // Debounce rapid clicks
        if (intent.action == ACTION_ACTIVATE_TEXT || intent.action == ACTION_ACTIVATE_VOICE) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < DEBOUNCE_DELAY_MS) {
                return
            }
            lastClickTime = currentTime
        }

        when (intent.action) {
            ACTION_ACTIVATE_TEXT -> {
                // Check for overlay permission
                if (!Settings.canDrawOverlays(context)) {
                    // Launch permission screen
                    launchPermissionScreen(context, voiceMode = false, startOverlay = true)
                    return
                }

                // Launch floating overlay in text mode
                val started = FloatingAgentService.start(context, voiceMode = false)
                if (started) {
                    FloatingAgentService.showOverlay(context)
                }
            }
            ACTION_ACTIVATE_VOICE -> {
                // Check for overlay permission
                if (!Settings.canDrawOverlays(context)) {
                    // Launch permission screen
                    launchPermissionScreen(context, voiceMode = true, startOverlay = false)
                    return
                }

                // Launch floating overlay in voice mode
                FloatingAgentService.start(context, voiceMode = true)
            }
        }
    }

    private fun launchPermissionScreen(context: Context, voiceMode: Boolean, startOverlay: Boolean) {
        val intent = Intent(context, PermissionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(PermissionActivity.EXTRA_START_VOICE_MODE, voiceMode)
            putExtra(PermissionActivity.EXTRA_START_OVERLAY, startOverlay)
        }
        context.startActivity(intent)
    }

    companion object {
        const val ACTION_ACTIVATE_TEXT = "com.agentos.widget.ACTION_ACTIVATE_TEXT"
        const val ACTION_ACTIVATE_VOICE = "com.agentos.widget.ACTION_ACTIVATE_VOICE"

        private const val DEBOUNCE_DELAY_MS = 500L
        private var lastClickTime = 0L

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_search_bar)

            // Set up click on search bar (text mode)
            val textIntent = Intent(context, AgentOSWidgetProvider::class.java).apply {
                action = ACTION_ACTIVATE_TEXT
            }
            val textPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 2,  // Unique per widget instance
                textIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, textPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_search_text, textPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_logo, textPendingIntent)

            // Set up click on mic button (voice mode)
            val voiceIntent = Intent(context, AgentOSWidgetProvider::class.java).apply {
                action = ACTION_ACTIVATE_VOICE
            }
            val voicePendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 2 + 1,  // Unique per widget instance
                voiceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_mic_button, voicePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
