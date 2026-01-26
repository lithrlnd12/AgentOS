package com.agentos.ui.main

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.agentos.ui.chat.ChatScreen
import com.agentos.ui.chat.ChatViewModel
import com.agentos.ui.permissions.PermissionScreen
import com.agentos.ui.permissions.areAllPermissionsGranted
import com.agentos.ui.theme.AgentOSTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var chatViewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var showPermissionScreen by remember { mutableStateOf(!areAllPermissionsGranted(this)) }
            var permissionsSkipped by remember { mutableStateOf(false) }

            AgentOSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showPermissionScreen && !permissionsSkipped) {
                        PermissionScreen(
                            onAllPermissionsGranted = {
                                showPermissionScreen = false
                            },
                            onSkip = {
                                permissionsSkipped = true
                            }
                        )
                    } else {
                        ChatScreen(
                            viewModel = chatViewModel,
                            onRequestAccessibility = { openAccessibilitySettings() },
                            onRequestOverlay = { openOverlaySettings() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions when returning to the app
        // This is handled by the PermissionScreen's LaunchedEffect
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openOverlaySettings() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
    }
}
