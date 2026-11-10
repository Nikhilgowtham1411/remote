package com.smarttv.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttv.remote.ui.theme.GlassBorder
import com.smarttv.remote.ui.theme.TextPrimary

@Composable
fun CircularDPad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(16.dp, CircleShape, ambientColor = Color(0x33000000), spotColor = Color(0x4D3D7BFF))
            .clip(CircleShape)
            .background(Color(0xFF131728))
            .border(1.5.dp, GlassBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // UP Button
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(size / 3)
                .clickable { onUp() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Up",
                tint = TextPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        // DOWN Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(size / 3)
                .clickable { onDown() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Down",
                tint = TextPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        // LEFT Button
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(size / 3)
                .fillMaxHeight()
                .clickable { onLeft() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Left",
                tint = TextPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        // RIGHT Button
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(size / 3)
                .fillMaxHeight()
                .clickable { onRight() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Right",
                tint = TextPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        // CENTER OK Button
        Box(
            modifier = Modifier
                .size(size / 2.5f)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF1B2035))
                .border(1.5.dp, Color(0xFF2C3554), CircleShape)
                .clickable { onOk() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OK",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
