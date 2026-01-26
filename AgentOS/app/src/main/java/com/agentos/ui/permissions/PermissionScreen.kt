package com.agentos.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agentos.accessibility.AgentAccessibilityService
import com.agentos.ui.theme.AgentOSColors

/**
 * Permission request screen that guides users through granting necessary permissions.
 */
@Composable
fun PermissionScreen(
    onAllPermissionsGranted: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Permission states
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasAccessibilityPermission by remember {
        mutableStateOf(AgentAccessibilityService.instance.value != null)
    }
    var hasMicrophonePermission by remember { mutableStateOf(false) }

    // Check microphone permission
    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicrophonePermission = isGranted
    }

    // Launcher for overlay settings
    val overlaySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }

    // Launcher for accessibility settings
    val accessibilitySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasAccessibilityPermission = AgentAccessibilityService.instance.value != null
    }

    // Check permissions on resume
    LaunchedEffect(Unit) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        hasAccessibilityPermission = AgentAccessibilityService.instance.value != null
    }

    // Observe accessibility service state
    LaunchedEffect(Unit) {
        AgentAccessibilityService.instance.collect { service ->
            hasAccessibilityPermission = service != null
        }
    }

    // Check if all permissions granted
    val allPermissionsGranted = hasOverlayPermission && hasAccessibilityPermission && hasMicrophonePermission

    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted) {
            onAllPermissionsGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AgentOSColors.NavyDark,
                        AgentOSColors.NavyPrimary
                    )
                )
            )
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Header
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = AgentOSColors.CyanPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "AgentOS needs a few permissions to control your phone and respond to voice commands.",
            style = MaterialTheme.typography.bodyLarge,
            color = AgentOSColors.Silver,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Permission cards
        PermissionCard(
            icon = Icons.Default.Layers,
            title = "Display Over Other Apps",
            description = "Required to show the floating assistant bubble and overlay on top of other apps.",
            isGranted = hasOverlayPermission,
            onRequestPermission = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                overlaySettingsLauncher.launch(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionCard(
            icon = Icons.Default.Accessibility,
            title = "Accessibility Service",
            description = "Required to read screen content and perform taps, swipes, and other actions on your behalf.",
            isGranted = hasAccessibilityPermission,
            onRequestPermission = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                accessibilitySettingsLauncher.launch(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionCard(
            icon = Icons.Default.Mic,
            title = "Microphone Access",
            description = "Required for voice commands and wake word detection (\"Hey AgentOS\").",
            isGranted = hasMicrophonePermission,
            onRequestPermission = {
                microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Continue button
        if (allPermissionsGranted) {
            Button(
                onClick = onAllPermissionsGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AgentOSColors.CyanPrimary
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "All Set! Continue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Show skip option
            TextButton(
                onClick = onSkip,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    "Skip for now",
                    color = AgentOSColors.SilverDark
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Privacy note
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = AgentOSColors.NavySurface.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = AgentOSColors.CyanLight,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Your privacy matters. AgentOS processes commands locally when possible and only sends data to AI services when you actively use the assistant.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AgentOSColors.Silver
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                AgentOSColors.Success.copy(alpha = 0.1f)
            else
                AgentOSColors.NavySurface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with status indicator
            Box {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = if (isGranted)
                        AgentOSColors.Success.copy(alpha = 0.2f)
                    else
                        AgentOSColors.CyanDark.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isGranted) AgentOSColors.Success else AgentOSColors.CyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Status badge
                if (isGranted) {
                    Surface(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd),
                        shape = CircleShape,
                        color = AgentOSColors.Success
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Granted",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AgentOSColors.Silver
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action button
            if (!isGranted) {
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AgentOSColors.CyanPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "Grant",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Check if all required permissions are granted.
 */
fun areAllPermissionsGranted(context: Context): Boolean {
    val hasOverlay = Settings.canDrawOverlays(context)
    val hasAccessibility = AgentAccessibilityService.instance.value != null
    // Microphone is optional for basic functionality
    return hasOverlay && hasAccessibility
}

/**
 * Check which permissions are missing.
 */
data class MissingPermissions(
    val overlay: Boolean,
    val accessibility: Boolean,
    val microphone: Boolean
)

fun getMissingPermissions(context: Context): MissingPermissions {
    return MissingPermissions(
        overlay = !Settings.canDrawOverlays(context),
        accessibility = AgentAccessibilityService.instance.value == null,
        microphone = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
    )
}
