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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttv.remote.data.model.SkipDurationConfig
import com.smarttv.remote.ui.components.BottomNavBar
import com.smarttv.remote.ui.components.GlassCard
import com.smarttv.remote.ui.components.NavTab
import com.smarttv.remote.ui.components.NeonGradientButton
import com.smarttv.remote.ui.theme.*

enum class SkipTab {
    BACKWARD,
    FORWARD
}

@Composable
fun SetSkipDurationScreen(
    currentConfig: SkipDurationConfig,
    onSaveConfig: (backward: Int, forward: Int, custom: Int) -> Unit,
    onBackClick: () -> Unit,
    onNavigateTab: (NavTab) -> Unit
) {
    var selectedTab by remember { mutableStateOf(SkipTab.BACKWARD) }
    var backwardDuration by remember { mutableStateOf(currentConfig.backwardSeconds) }
    var forwardDuration by remember { mutableStateOf(currentConfig.forwardSeconds) }
    var customDuration by remember { mutableStateOf(currentConfig.customDuration) }

    val currentSelection = if (selectedTab == SkipTab.BACKWARD) backwardDuration else forwardDuration
    val standardOptions = listOf(5, 10, 15, 30, 60)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
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
                    text = "Set Skip Duration",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Two Tabs: Backward (↺) and Forward (↻)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF141724))
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selectedTab == SkipTab.BACKWARD) NeonBlue else Color.Transparent)
                        .clickable { selectedTab = SkipTab.BACKWARD }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Backward (↺)",
                        color = if (selectedTab == SkipTab.BACKWARD) Color.White else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selectedTab == SkipTab.FORWARD) NeonBlue else Color.Transparent)
                        .clickable { selectedTab = SkipTab.FORWARD }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Forward (↻)",
                        color = if (selectedTab == SkipTab.FORWARD) Color.White else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Radio Button Options
            standardOptions.forEach { seconds ->
                val isSelected = currentSelection == seconds
                DurationOptionCard(
                    label = "$seconds seconds",
                    badge = if (seconds == 10) "(Default)" else null,
                    isSelected = isSelected,
                    onClick = {
                        if (selectedTab == SkipTab.BACKWARD) {
                            backwardDuration = seconds
                        } else {
                            forwardDuration = seconds
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Custom Duration Option
            val isCustomSelected = !standardOptions.contains(currentSelection)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                isSelected = isCustomSelected,
                cornerRadius = 14.dp,
                onClick = {
                    if (selectedTab == SkipTab.BACKWARD) {
                        backwardDuration = customDuration
                    } else {
                        forwardDuration = customDuration
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Custom",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Minus Button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1B233D))
                                .clickable {
                                    if (customDuration > 5) {
                                        customDuration -= 5
                                        if (selectedTab == SkipTab.BACKWARD) backwardDuration = customDuration
                                        else forwardDuration = customDuration
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Number display
                        Text(
                            text = "${customDuration} s",
                            color = NeonCyan,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Plus Button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1B233D))
                                .clickable {
                                    if (customDuration < 300) {
                                        customDuration += 5
                                        if (selectedTab == SkipTab.BACKWARD) backwardDuration = customDuration
                                        else forwardDuration = customDuration
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save Button
            NeonGradientButton(
                text = "Save",
                onClick = {
                    onSaveConfig(backwardDuration, forwardDuration, customDuration)
                    onBackClick()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Bottom Navigation Bar
        BottomNavBar(
            selectedTab = NavTab.REMOTE,
            onTabSelected = onNavigateTab
        )
    }
}

@Composable
private fun DurationOptionCard(
    label: String,
    badge: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        isSelected = isSelected,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = badge,
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = NeonBlue,
                    unselectedColor = TextSecondary
                )
            )
        }
    }
}
