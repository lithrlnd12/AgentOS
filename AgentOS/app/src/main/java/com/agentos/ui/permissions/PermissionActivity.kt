package com.agentos.ui.permissions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.agentos.service.FloatingAgentService
import com.agentos.ui.theme.AgentOSColors
import com.agentos.ui.theme.AgentOSTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity for requesting permissions.
 * Can be launched from widget or notification when permissions are missing.
 */
@AndroidEntryPoint
class PermissionActivity : ComponentActivity() {

    companion object {
        const val EXTRA_START_VOICE_MODE = "start_voice_mode"
        const val EXTRA_START_OVERLAY = "start_overlay"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startVoiceMode = intent.getBooleanExtra(EXTRA_START_VOICE_MODE, false)
        val startOverlay = intent.getBooleanExtra(EXTRA_START_OVERLAY, false)

        setContent {
            AgentOSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AgentOSColors.NavyDark
                ) {
                    PermissionScreen(
                        onAllPermissionsGranted = {
                            // Start the requested service/mode
                            if (startVoiceMode) {
                                FloatingAgentService.start(this, voiceMode = true)
                            } else if (startOverlay) {
                                FloatingAgentService.start(this, voiceMode = false)
                                FloatingAgentService.showOverlay(this)
                            }
                            finish()
                        },
                        onSkip = {
                            // Just close, don't start the service
                            finish()
                        }
                    )
                }
            }
        }
    }
}
