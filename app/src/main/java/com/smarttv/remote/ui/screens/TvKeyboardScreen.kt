package com.smarttv.remote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttv.remote.ui.components.BottomNavBar
import com.smarttv.remote.ui.components.NavTab
import com.smarttv.remote.ui.components.TvHeaderBar
import com.smarttv.remote.ui.theme.*

/**
 * Type with the phone's own keyboard. Every change is sent to the TV (letters, backspace),
 * so open a search box on the TV first, then type here.
 */
@Composable
fun TvKeyboardScreen(
    tvName: String = "Lumio Vision 7",
    tvModel: String = "Google TV",
    isConnected: Boolean = true,
    currentText: String,
    tvTextFieldActive: Boolean = false,
    onTextChange: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onVoiceClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTvSelectClick: () -> Unit,
    onNavigateTab: (NavTab) -> Unit
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Column(modifier = Modifier.fillMaxSize().background(BgDark)) {
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
        ) {
            Spacer(Modifier.height(10.dp))
            Text("TV Keyboard", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (tvTextFieldActive) "TV text box detected. Type here." else "Open a search box on the TV, then type here.",
                color = if (tvTextFieldActive) NeonGreen else TextSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = currentText,
                onValueChange = onTextChange,
                placeholder = { Text("Type here, it appears on the TV", color = TextSecondary, fontSize = 14.sp) },
                trailingIcon = {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Voice",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp).clickable { onVoiceClick() }
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onEnter() }),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = NeonBlue,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = Color(0xFF141724),
                    unfocusedContainerColor = Color(0xFF141724)
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ActionButton(Icons.Default.Backspace, "Delete", Color(0xFF1B2034), Modifier.weight(1f), onBackspace)
                ActionButton(Icons.Default.Clear, "Clear", Color(0xFF1B2034), Modifier.weight(1f), onClear)
                ActionButton(Icons.Default.Send, "Enter", NeonBlue, Modifier.weight(1f), onEnter)
            }
        }

        BottomNavBar(selectedTab = NavTab.KEYBOARD, onTabSelected = onNavigateTab)
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    background: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
