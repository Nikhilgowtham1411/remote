package com.smarttv.remote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
    tvName: String = "Lumio Vision 7",
    tvModel: String = "Google TV",
    isConnected: Boolean = true,
    onCastScreenClick: () -> Unit,
    onCastVideoClick: () -> Unit,
    onCastPhotosClick: () -> Unit,
    onCastMusicClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTvSelectClick: () -> Unit,
    onNavigateTab: (NavTab) -> Unit
) {
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
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Cast Screen
            CastActionCard(
                icon = Icons.Default.ScreenShare,
                title = "Cast Screen",
                subtitle = "Mirror your phone screen",
                isSelected = false,
                onClick = onCastScreenClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Cast Video (Highlighted in reference image with glowing blue border)
            CastActionCard(
                icon = Icons.Default.PlayCircle,
                title = "Cast Video",
                subtitle = "Send a video to TV",
                isSelected = true,
                onClick = onCastVideoClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Cast Photos
            CastActionCard(
                icon = Icons.Default.Image,
                title = "Cast Photos",
                subtitle = "Show photos on TV",
                isSelected = false,
                onClick = onCastPhotosClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Cast Music
            CastActionCard(
                icon = Icons.Default.MusicNote,
                title = "Cast Music",
                subtitle = "Play music on TV",
                isSelected = false,
                onClick = onCastMusicClick
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bottom Navigation Bar
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
