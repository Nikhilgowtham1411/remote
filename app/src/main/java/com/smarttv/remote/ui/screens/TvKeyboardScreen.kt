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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttv.remote.ui.components.BottomNavBar
import com.smarttv.remote.ui.components.NavTab
import com.smarttv.remote.ui.components.TvHeaderBar
import com.smarttv.remote.ui.theme.*

@Composable
fun TvKeyboardScreen(
    tvName: String = "Lumio Vision 7",
    tvModel: String = "Google TV",
    isConnected: Boolean = true,
    currentText: String,
    onKeyInput: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onVoiceClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTvSelectClick: () -> Unit,
    onNavigateTab: (NavTab) -> Unit
) {
    var isShiftActive by remember { mutableStateOf(false) }
    var isNumericMode by remember { mutableStateOf(false) }

    val row1 = if (isNumericMode) listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    else if (isShiftActive) listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
    else listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")

    val row2 = if (isNumericMode) listOf("-", "/", ":", ";", "(", ")", "$", "&", "@", "\"")
    else if (isShiftActive) listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
    else listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")

    val row3 = if (isNumericMode) listOf(".", ",", "?", "!", "'")
    else if (isShiftActive) listOf("Z", "X", "C", "V", "B", "N", "M")
    else listOf("z", "x", "c", "v", "b", "n", "m")

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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "TV Keyboard",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Type text on your TV easily.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Input Bar with Mic Shortcut
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF141724))
                        .border(1.dp, GlassBorder, RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (currentText.isEmpty()) "Search or enter text..." else currentText,
                        color = if (currentText.isEmpty()) TextSecondary else TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onVoiceClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Virtual Keyboard Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ROW 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row1.forEach { key ->
                        KeyboardKey(
                            label = key,
                            onClick = { onKeyInput(key) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ROW 2
                Row(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row2.forEach { key ->
                        KeyboardKey(
                            label = key,
                            onClick = { onKeyInput(key) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ROW 3: Shift, Characters, Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shift Key
                    SpecialKey(
                        icon = Icons.Default.ArrowUpward,
                        isActive = isShiftActive,
                        onClick = { isShiftActive = !isShiftActive },
                        modifier = Modifier.weight(1.3f)
                    )

                    row3.forEach { key ->
                        KeyboardKey(
                            label = key,
                            onClick = { onKeyInput(key) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Backspace Key
                    SpecialKey(
                        icon = Icons.Default.Backspace,
                        isActive = false,
                        onClick = onBackspace,
                        modifier = Modifier.weight(1.3f)
                    )
                }

                // ROW 4: 123 / ABC, Emoji, Spacebar ("English"), Enter Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 123 / ABC Toggle
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1B2034))
                            .clickable { isNumericMode = !isNumericMode },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isNumericMode) "ABC" else "123",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Emoji Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1B2034))
                            .clickable { onKeyInput("😊") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mood,
                            contentDescription = "Emoji",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Spacebar ("English")
                    Box(
                        modifier = Modifier
                            .weight(3.8f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF161B2E))
                            .clickable { onKeyInput(" ") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "English",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    // Enter Button (Cyan/Blue action)
                    Box(
                        modifier = Modifier
                            .weight(1.4f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonBlue)
                            .clickable { onEnter() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Enter",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Bottom Navigation Bar
        BottomNavBar(
            selectedTab = NavTab.KEYBOARD,
            onTabSelected = onNavigateTab
        )
    }
}

@Composable
private fun KeyboardKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF14192B))
            .border(1.dp, Color(0xFF222942), RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SpecialKey(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) NeonBlue else Color(0xFF1B2034))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) Color.White else TextPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}
