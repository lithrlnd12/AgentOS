package com.agentos.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.agentos.R
import com.agentos.di.FloatingAgentServiceEntryPoint
import com.agentos.ui.chat.ChatViewModel
import com.agentos.ui.main.MainActivity
import com.agentos.ui.overlay.FloatingAgentOverlay
import com.agentos.ui.overlay.FloatingBubbleView
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground service that manages floating overlay UI.
 * Provides bubble for quick access and full overlay for interaction.
 */
@AndroidEntryPoint
class FloatingAgentService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private lateinit var windowManager: WindowManager
    private lateinit var chatViewModel: ChatViewModel
    private var bubbleView: View? = null
    private var overlayView: ComposeView? = null
    private var outlineView: View? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val _viewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    companion object {
        private const val CHANNEL_ID = "agentos_floating_service"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_SHOW_BUBBLE = "com.agentos.action.SHOW_BUBBLE"
        const val ACTION_SHOW_OVERLAY = "com.agentos.action.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.agentos.action.HIDE_OVERLAY"
        const val ACTION_STOP_SERVICE = "com.agentos.action.STOP_SERVICE"
        const val ACTION_VOICE_MODE = "com.agentos.action.VOICE_MODE"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _overlayState = MutableStateFlow(OverlayState.BUBBLE)
        val overlayState: StateFlow<OverlayState> = _overlayState.asStateFlow()

        fun start(context: Context, voiceMode: Boolean = false): Boolean {
            // Check for overlay permission
            if (!Settings.canDrawOverlays(context)) {
                return false
            }

            val intent = Intent(context, FloatingAgentService::class.java).apply {
                action = if (voiceMode) ACTION_VOICE_MODE else ACTION_SHOW_BUBBLE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            return true
        }

        fun canDrawOverlays(context: Context): Boolean {
            return Settings.canDrawOverlays(context)
        }

        fun openOverlaySettings(context: Context) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingAgentService::class.java))
        }

        fun showOverlay(context: Context) {
            context.startService(Intent(context, FloatingAgentService::class.java).apply {
                action = ACTION_SHOW_OVERLAY
            })
        }

        fun hideOverlay(context: Context) {
            context.startService(Intent(context, FloatingAgentService::class.java).apply {
                action = ACTION_HIDE_OVERLAY
            })
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        // Inject ChatViewModel via EntryPoint (hiltViewModel() doesn't work in Service context)
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            FloatingAgentServiceEntryPoint::class.java
        )
        chatViewModel = entryPoint.chatViewModel()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        _isRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Check for overlay permission first
        if (!Settings.canDrawOverlays(this)) {
            // Don't try to show overlay without permission
            // Just stay in background as a service
            _overlayState.value = OverlayState.HIDDEN
            return START_STICKY
        }

        when (intent?.action) {
            ACTION_SHOW_BUBBLE -> {
                showBubble()
                _overlayState.value = OverlayState.BUBBLE
            }
            ACTION_SHOW_OVERLAY -> {
                hideBubble()
                showOverlay()
                _overlayState.value = OverlayState.EXPANDED
            }
            ACTION_HIDE_OVERLAY -> {
                hideOverlay()
                showBubble()
                _overlayState.value = OverlayState.BUBBLE
            }
            ACTION_VOICE_MODE -> {
                hideBubble()
                showOverlay(voiceMode = true)
                _overlayState.value = OverlayState.EXPANDED
            }
            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
            else -> {
                showBubble()
                _overlayState.value = OverlayState.BUBBLE
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        hideBubble()
        hideOverlay()
        hideOutline()
        _viewModelStore.clear()
        _isRunning.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AgentOS Floating Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps AgentOS assistant running in the background"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val activateIntent = Intent(this, FloatingAgentService::class.java).apply {
            action = ACTION_SHOW_OVERLAY
        }
        val activatePendingIntent = PendingIntent.getService(
            this, 1, activateIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloatingAgentService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AgentOS Active")
            .setContentText("Tap bubble or say \"AgentOS\" to activate")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(mainPendingIntent)
            .addAction(R.drawable.ic_notification, "Activate", activatePendingIntent)
            .addAction(R.drawable.ic_notification, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun showBubble() {
        if (bubbleView != null) return

        val bubbleLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }

        bubbleView = FloatingBubbleView(this).apply {
            setOnClickListener {
                hideBubble()
                showOverlay()
                _overlayState.value = OverlayState.EXPANDED
            }
            setOnLongClickListener {
                // Show quick actions menu
                true
            }
        }

        setupDragBehavior(bubbleView!!, bubbleLayoutParams)
        windowManager.addView(bubbleView, bubbleLayoutParams)

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    private fun hideBubble() {
        bubbleView?.let {
            windowManager.removeView(it)
            bubbleView = null
        }
    }

    private fun showOverlay(voiceMode: Boolean = false) {
        if (overlayView != null) return

        val overlayLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingAgentService)
            setViewTreeSavedStateRegistryOwner(this@FloatingAgentService)
            setViewTreeViewModelStoreOwner(this@FloatingAgentService)

            setContent {
                FloatingAgentOverlay(
                    viewModel = chatViewModel,
                    onMinimize = {
                        hideOverlay()
                        showBubble()
                        _overlayState.value = OverlayState.BUBBLE
                    },
                    onClose = {
                        stopSelf()
                    },
                    startInVoiceMode = voiceMode
                )
            }
        }

        windowManager.addView(overlayView, overlayLayoutParams)
        showOutline()
    }

    private fun hideOverlay() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
        hideOutline()
    }

    private fun showOutline() {
        // Screen outline will be shown based on workflow state
        // Implementation in ScreenOutlineView composable
    }

    private fun hideOutline() {
        outlineView?.let {
            windowManager.removeView(it)
            outlineView = null
        }
    }

    private fun setupDragBehavior(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = true

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    if (dx * dx + dy * dy > 100) {
                        isClick = false
                    }

                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        v.performClick()
                    }
                    true
                }
                else -> false
            }
        }
    }
}

enum class OverlayState {
    BUBBLE,
    EXPANDED,
    HIDDEN
}
