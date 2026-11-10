package com.smarttv.remote.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import com.smarttv.remote.data.protocol.CastManager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttv.remote.ui.components.BottomNavBar
import com.smarttv.remote.ui.components.GlassCard
import com.smarttv.remote.ui.components.NavTab
import com.smarttv.remote.ui.components.TvHeaderBar
import com.smarttv.remote.ui.theme.*

@Composable
fun CastScreen(
    tvName: String = "No TV selected",
    tvModel: String = "Google TV",
    isConnected: Boolean = true,
    castConnected: Boolean,
    castDeviceName: String?,
    castStatus: String?,
    routes: List<CastManager.CastRoute>,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onSelectRoute: (String) -> Unit,
    onCastFile: (Uri, String) -> Unit,
    onCastScreenClick: () -> Unit,
    onStopCast: () -> Unit,
    onSettingsClick: () -> Unit,
    onTvSelectClick: () -> Unit,
    onNavigateTab: (NavTab) -> Unit
) {
    LaunchedEffect(Unit) { onStartScan() }
    DisposableEffect(Unit) { onDispose { onStopScan() } }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onCastFile(uri, "video/mp4")
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onCastFile(uri, "image/jpeg")
    }
    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onCastFile(uri, "audio/mpeg")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        TvHeaderBar(
            tvName = tvName,
            tvModel = tvModel,
            isConnected = isConnected,
            onSettingsClick = onSettingsClick,
            onTvSelectClick = onTvSelectClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Cast to Your TV",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (!castConnected) {
                Text(
                    text = "Step 1: Tap your TV below",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (routes.isEmpty()) {
                    Text(
                        text = "Looking for your TV...\nKeep the TV on and on the same Wi-Fi.",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        lineHeight = 21.sp
                    )
                } else {
                    routes.forEach { route ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            isSelected = false,
                            cornerRadius = 18.dp,
                            onClick = { onSelectRoute(route.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Cast, null, tint = NeonCyan, modifier = Modifier.size(30.dp))
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = route.name,
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                if (castStatus != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = castStatus, color = NeonCyan, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    isSelected = true,
                    cornerRadius = 18.dp,
                    onClick = {}
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = NeonGreen, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ready to cast", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(castStatus ?: (castDeviceName ?: "TV"), color = TextSecondary, fontSize = 13.sp)
                        }
                        Text(
                            text = "Stop",
                            color = PowerRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onStopCast() }
                                .padding(10.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Step 2: Choose what to show",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            CastActionCard(
                icon = Icons.Default.PlayCircle,
                title = "Cast Video",
                subtitle = "Pick a video from your phone",
                isSelected = castConnected,
                onClick = { if (castConnected) videoPicker.launch("video/*") }
            )
            Spacer(modifier = Modifier.height(14.dp))
            CastActionCard(
                icon = Icons.Default.Image,
                title = "Cast Photos",
                subtitle = "Show a photo on the TV",
                isSelected = false,
                onClick = { if (castConnected) photoPicker.launch("image/*") }
            )
            Spacer(modifier = Modifier.height(14.dp))
            CastActionCard(
                icon = Icons.Default.MusicNote,
                title = "Cast Music",
                subtitle = "Play a song on the TV",
                isSelected = false,
                onClick = { if (castConnected) musicPicker.launch("audio/*") }
            )
            Spacer(modifier = Modifier.height(14.dp))
            CastActionCard(
                icon = Icons.Default.ScreenShare,
                title = "Cast Screen",
                subtitle = "Mirror your phone (opens phone's Cast setting)",
                isSelected = false,
                onClick = onCastScreenClick
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        BottomNavBar(
            selectedTab = NavTab.CAST,
            onTabSelected = onNavigateTab
        )
    }
}

@Composable
private fun CastActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        isSelected = isSelected,
        cornerRadius = 18.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) Color(0xFF1B284D) else Color(0xFF14192B))
                    .border(1.dp, if (isSelected) NeonCyan else GlassBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected) NeonCyan else TextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}
