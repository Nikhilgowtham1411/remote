package com.smarttv.remote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttv.remote.data.model.SkipDurationConfig
import com.smarttv.remote.data.model.TvDevice
import com.smarttv.remote.ui.components.GlassCard
import com.smarttv.remote.ui.theme.*

@Composable
fun SettingsScreen(
    currentDevice: TvDevice?,
    skipConfig: SkipDurationConfig,
    hapticEnabled: Boolean,
    easyModeEnabled: Boolean,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleEasyMode: (Boolean) -> Unit,
    onChangeTvClick: () -> Unit,
    onReconnectClick: () -> Unit,
    onSkipSettingsClick: () -> Unit,
    onBackClick: () -> Unit,
    isConnected: Boolean = false,
    connectionError: String? = null,
    protocolLog: List<String> = emptyList(),
    tvAwake: Boolean? = null,
    tmdbKey: String = "",
    onSaveKey: (String) -> Unit = {}
) {
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title with Back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Settings",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION: CURRENT TV
        Text(
            text = "CONNECTED TV",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF19223D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = currentDevice?.name ?: "No TV selected",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${currentDevice?.model ?: "Google TV"} • ${currentDevice?.host ?: "not paired"}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onReconnectClick,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF121626))
                    ) {
                        Text("Reconnect", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onChangeTvClick,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF121626))
                    ) {
                        Text("Change TV", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION: PREFERENCES & CONTROLS
        Text(
            text = "PREFERENCES",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Skip Duration Item
        SettingsItem(
            icon = Icons.Default.Update,
            title = "Skip Duration",
            subtitle = "Backward ${skipConfig.backwardSeconds}s / Forward ${skipConfig.forwardSeconds}s",
            onClick = onSkipSettingsClick
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Haptic Feedback Toggle
        SettingsToggleItem(
            icon = Icons.Default.Vibration,
            title = "Vibration & Sound",
            subtitle = "Tactile click feedback on button press",
            isChecked = hapticEnabled,
            onCheckedChange = onToggleHaptic
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Easy Mode Toggle (Accessibility)
        SettingsToggleItem(
            icon = Icons.Default.AccessibilityNew,
            title = "Easy Mode / Large Buttons",
            subtitle = "Larger D-PAD and controllers for comfortable usage",
            isChecked = easyModeEnabled,
            onCheckedChange = onToggleEasyMode
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Connection Diagnostics Item
        SettingsItem(
            icon = Icons.Default.Speed,
            title = "Connection Diagnostics",
            subtitle = "Inspect Wi-Fi latency, ports 6466/6467 & TLS status",
            onClick = { showDiagnosticsDialog = true }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION: ABOUT
        Text(
            text = "ABOUT",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Smart TV Remote",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Version 1.0.0 (Production Build)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Google TV Protocol v2 • Native Android Jetpack Compose • Wi-Fi Only • Optimized for Lumio Vision 7 (2026)",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showDiagnosticsDialog) {
        AlertDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            title = {
                Text(text = "Connection Diagnostics", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticLine("Target Host", currentDevice?.host ?: "not paired")
                    DiagnosticLine("Remote Port (TLS)", "${currentDevice?.port ?: 6466}")
                    DiagnosticLine("Pairing Port (TLS)", "${currentDevice?.pairingPort ?: 6467}")
                    DiagnosticLine("Paired", if (currentDevice?.isPaired == true) "Yes" else "No")
                    DiagnosticLine("Session", if (isConnected) "Connected" else "Offline")
                    DiagnosticLine("Protocol", "Google TV Remote v2")
                    DiagnosticLine("TV screen", when (tvAwake) { true -> "On"; false -> "Off (standby)"; null -> "Unknown" })
                    if (protocolLog.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Recent activity", color = TextSecondary, fontSize = 12.sp)
                        protocolLog.takeLast(12).forEach {
                            Text(text = it, color = TextPrimary, fontSize = 11.sp)
                        }
                    }
                    if (!isConnected && connectionError != null) {
                        Text(text = connectionError, color = PowerRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiagnosticsDialog = false }) {
                    Text("Close", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF141724),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun DiagnosticLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Text(text = value, color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF161C30)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF161C30)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = NeonBlue,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = Color(0xFF1F253B)
                )
            )
        }
    }
}
