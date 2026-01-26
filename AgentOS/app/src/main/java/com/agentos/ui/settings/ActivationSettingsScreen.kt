package com.agentos.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agentos.service.FloatingAgentService
import com.agentos.ui.theme.AgentOSColors
import com.agentos.ui.theme.AgentOSTheme
import kotlinx.coroutines.flow.map

/**
 * Settings screen for configuring AgentOS activation methods.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivationSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Collect states
    val isFloatingBubbleEnabled by FloatingAgentService.isRunning.collectAsState()
    val canDrawOverlays = remember {
        Settings.canDrawOverlays(context)
    }

    // Preferences state (would be persisted in real implementation)
    var wakeWordEnabled by remember { mutableStateOf(true) }
    var volumeButtonShortcut by remember { mutableStateOf(false) }
    var showQuickSettingsTile by remember { mutableStateOf(true) }

    AgentOSTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Activation Settings") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
            ) {
                // Overlay Permission Section
                if (!canDrawOverlays) {
                    PermissionCard(
                        title = "Overlay Permission Required",
                        description = "AgentOS needs permission to display over other apps.",
                        buttonText = "Grant Permission",
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                        }
                    )
                }

                // Floating Bubble Section
                SettingsSection(title = "Floating Bubble") {
                    SettingsSwitchItem(
                        icon = Icons.Default.Lens,
                        title = "Show floating bubble",
                        description = "Display a floating bubble for quick access",
                        checked = isFloatingBubbleEnabled,
                        enabled = canDrawOverlays,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                FloatingAgentService.start(context)
                            } else {
                                FloatingAgentService.stop(context)
                            }
                        }
                    )
                }

                // Voice Activation Section
                SettingsSection(title = "Voice Activation") {
                    SettingsSwitchItem(
                        icon = Icons.Default.RecordVoiceOver,
                        title = "Wake word detection",
                        description = "Say \"AgentOS\" to activate (experimental)",
                        checked = wakeWordEnabled,
                        onCheckedChange = { wakeWordEnabled = it }
                    )

                    SettingsInfoItem(
                        icon = Icons.Default.Info,
                        title = "Supported wake words",
                        description = "\"AgentOS\", \"Hey AgentOS\", \"OK AgentOS\""
                    )
                }

                // Quick Access Section
                SettingsSection(title = "Quick Access") {
                    SettingsSwitchItem(
                        icon = Icons.Default.Dashboard,
                        title = "Quick Settings tile",
                        description = "Add tile to notification shade",
                        checked = showQuickSettingsTile,
                        onCheckedChange = { showQuickSettingsTile = it }
                    )

                    SettingsSwitchItem(
                        icon = Icons.Default.VolumeUp,
                        title = "Volume button shortcut",
                        description = "Long press volume to activate (requires accessibility)",
                        checked = volumeButtonShortcut,
                        onCheckedChange = { volumeButtonShortcut = it }
                    )

                    SettingsNavigationItem(
                        icon = Icons.Default.Widgets,
                        title = "Home screen widget",
                        description = "Add search widget to home screen",
                        onClick = {
                            // This would typically open the widget picker
                            // For now, show a hint
                        }
                    )
                }

                // About Section
                SettingsSection(title = "About") {
                    SettingsInfoItem(
                        icon = Icons.Default.Security,
                        title = "Privacy",
                        description = "Voice is processed locally. Commands are sent to Claude API."
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = AgentOSColors.CyanPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        content()
        Divider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = AgentOSColors.NavyLight
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { if (enabled) onCheckedChange(!checked) },
        enabled = enabled,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) AgentOSColors.Silver else AgentOSColors.SilverDark,
                modifier = Modifier.size(24.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AgentOSColors.CyanPrimary,
                    checkedTrackColor = AgentOSColors.CyanDark.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun SettingsInfoItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AgentOSColors.Silver,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsNavigationItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AgentOSColors.Silver,
                modifier = Modifier.size(24.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AgentOSColors.SilverDark
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AgentOSColors.Warning.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AgentOSColors.Warning
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AgentOSColors.Warning
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AgentOSColors.Warning
                )
            ) {
                Text(buttonText)
            }
        }
    }
}
