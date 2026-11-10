package com.smarttv.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
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
import com.smarttv.remote.ui.theme.NeonBlue
import com.smarttv.remote.ui.theme.NeonCyan
import com.smarttv.remote.ui.theme.TextPrimary
import com.smarttv.remote.ui.theme.TextSecondary

@Composable
fun GlowingMicButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .shadow(
                    elevation = if (isListening) 20.dp else 10.dp,
                    shape = CircleShape,
                    ambientColor = NeonCyan.copy(alpha = 0.5f),
                    spotColor = NeonBlue.copy(alpha = 0.7f)
                )
                .clip(CircleShape)
                .background(Color(0xFF14192A))
                .border(
                    width = if (isListening) 2.dp else 1.2.dp,
                    color = if (isListening) NeonCyan else Color(0xFF2E3858),
                    shape = CircleShape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Search",
                tint = if (isListening) NeonCyan else TextPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Assistant dots and label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF4285F4)))
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFEA4335)))
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFFBBC05)))
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF34A853)))

            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isListening) "Listening..." else "Hold to talk to Google TV",
                color = if (isListening) NeonCyan else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
