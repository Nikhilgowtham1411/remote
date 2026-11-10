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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttv.remote.data.model.RemoteCommand
import com.smarttv.remote.data.model.TvApp
import com.smarttv.remote.ui.components.*
import com.smarttv.remote.ui.theme.*

@Composable
fun MainRemoteScreen(
    tvName: String = "Lumio Vision 7",
    tvModel: String = "Google TV",
    isConnected: Boolean = true,
    isListeningVoice: Boolean = false,
    easyMode: Boolean = false,
    onCommand: (RemoteCommand) -> Unit,
    onPowerClick: () -> Unit,
    onAppClick: (TvApp) -> Unit,
    onVoiceClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTvSelectClick: () -> Unit,
    onNavigateTab: (NavTab) -> Unit,
    onPlaybackClick: () -> Unit
) {
    val dpadSize = if (easyMode) 210.dp else 185.dp
    val sliderHeight = if (easyMode) 195.dp else 175.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Top Header Bar
        TvHeaderBar(
            tvName = tvName,
            tvModel = tvModel,
            isConnected = isConnected,
            onSettingsClick = onSettingsClick,
            onTvSelectClick = onTvSelectClick
        )

        // Main Remote Content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Power & Mute Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Power Button (Red glow)
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(12.dp, CircleShape, ambientColor = PowerRed.copy(alpha = 0.5f))
                            .clip(CircleShape)
                            .background(Color(0xFF231114))
                            .border(1.2.dp, PowerRed.copy(alpha = 0.7f), CircleShape)
                            .clickable { onPowerClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Power",
                            tint = PowerRed,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Media Playback shortcut badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141724))
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .clickable { onPlaybackClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Media",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Mute Button
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFF131728))
                            .border(1.dp, GlassBorder, CircleShape)
                            .clickable { onCommand(RemoteCommand.MUTE) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeMute,
                            contentDescription = "Mute",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Middle Controller Section: Volume, D-PAD, Channel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Volume Vertical Slider
                    VerticalSliderController(
                        label = "VOL",
                        onPlus = { onCommand(RemoteCommand.VOLUME_UP) },
                        onMinus = { onCommand(RemoteCommand.VOLUME_DOWN) },
                        height = sliderHeight
                    )

                    // Circular D-PAD
                    CircularDPad(
                        onUp = { onCommand(RemoteCommand.DPAD_UP) },
                        onDown = { onCommand(RemoteCommand.DPAD_DOWN) },
                        onLeft = { onCommand(RemoteCommand.DPAD_LEFT) },
                        onRight = { onCommand(RemoteCommand.DPAD_RIGHT) },
                        onOk = { onCommand(RemoteCommand.DPAD_CENTER) },
                        size = dpadSize
                    )

                    // Channel Vertical Slider
                    VerticalSliderController(
                        label = "CH",
                        onPlus = { onCommand(RemoteCommand.CHANNEL_UP) },
                        onMinus = { onCommand(RemoteCommand.CHANNEL_DOWN) },
                        height = sliderHeight
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Navigation row below D-PAD: Back, Home, Recent Apps
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavPillButton(
                        icon = Icons.Default.Undo,
                        contentDescription = "Back",
                        onClick = { onCommand(RemoteCommand.BACK) }
                    )

                    NavPillButton(
                        icon = Icons.Default.Home,
                        contentDescription = "Home",
                        onClick = { onCommand(RemoteCommand.HOME) }
                    )

                    NavPillButton(
                        icon = Icons.Default.AspectRatio,
                        contentDescription = "Recent Apps",
                        onClick = { onCommand(RemoteCommand.RECENT_APPS) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Popular Apps Row: Netflix, YouTube, JioHotstar, Prime Video
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppShortcutPill(
                        name = "NETFLIX",
                        accentColor = Color(0xFFE50914),
                        onClick = { onAppClick(TvApp.DEFAULT_APPS[0]) },
                        modifier = Modifier.weight(1f)
                    )

                    AppShortcutPill(
                        name = "YouTube",
                        accentColor = Color(0xFFFF0000),
                        onClick = { onAppClick(TvApp.DEFAULT_APPS[1]) },
                        modifier = Modifier.weight(1f)
                    )

                    AppShortcutPill(
                        name = "JioHotstar",
                        accentColor = Color(0xFF9D4EDD),
                        onClick = { onAppClick(TvApp.DEFAULT_APPS[2]) },
                        modifier = Modifier.weight(1f)
                    )

                    AppShortcutPill(
                        name = "prime video",
                        accentColor = Color(0xFF00A8E1),
                        onClick = { onAppClick(TvApp.DEFAULT_APPS[3]) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Bottom Voice Command
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GlowingMicButton(
                    isListening = isListeningVoice,
                    onClick = onVoiceClick
                )
            }
        }

        // Bottom Navigation Bar
        BottomNavBar(
            selectedTab = NavTab.REMOTE,
            onTabSelected = onNavigateTab
        )
    }
}

@Composable
private fun NavPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 62.dp, height = 44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF131728))
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}
