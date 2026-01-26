package com.agentos.service

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Quick Settings tile for quick access to AgentOS.
 * Allows toggling the floating overlay from the notification shade.
 */
@RequiresApi(Build.VERSION_CODES.N)
class AgentOSTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()

        // Observe service state
        FloatingAgentService.isRunning.onEach {
            updateTileState()
        }.launchIn(scope)
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        // Check for overlay permission
        if (!Settings.canDrawOverlays(this)) {
            // Open overlay permission settings
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivityAndCollapse(intent)
            return
        }

        // Toggle the floating agent service
        if (FloatingAgentService.isRunning.value) {
            when (FloatingAgentService.overlayState.value) {
                OverlayState.BUBBLE -> {
                    // Show overlay
                    FloatingAgentService.showOverlay(this)
                }
                OverlayState.EXPANDED -> {
                    // Hide overlay to bubble
                    FloatingAgentService.hideOverlay(this)
                }
                OverlayState.HIDDEN -> {
                    // Stop service
                    FloatingAgentService.stop(this)
                }
            }
        } else {
            // Start service
            FloatingAgentService.start(this)
        }

        updateTileState()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun updateTileState() {
        qsTile?.let { tile ->
            val isRunning = FloatingAgentService.isRunning.value
            val overlayState = FloatingAgentService.overlayState.value

            tile.state = when {
                !isRunning -> Tile.STATE_INACTIVE
                overlayState == OverlayState.EXPANDED -> Tile.STATE_ACTIVE
                else -> Tile.STATE_ACTIVE
            }

            tile.label = when {
                !isRunning -> "AgentOS"
                overlayState == OverlayState.EXPANDED -> "AgentOS Active"
                else -> "AgentOS Ready"
            }

            tile.subtitle = when {
                !isRunning -> "Tap to activate"
                overlayState == OverlayState.EXPANDED -> "Tap to minimize"
                else -> "Tap to expand"
            }

            tile.updateTile()
        }
    }
}
