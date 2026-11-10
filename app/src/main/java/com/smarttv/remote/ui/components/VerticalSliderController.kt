package com.smarttv.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import com.smarttv.remote.ui.theme.TextSecondary

@Composable
fun VerticalSliderController(
    label: String,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 62.dp,
    height: Dp = 180.dp
) {
    val shape = RoundedCornerShape(31.dp)

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shadow(12.dp, shape, ambientColor = Color(0x33000000))
            .clip(shape)
            .background(Color(0xFF131728))
            .border(1.2.dp, GlassBorder, shape)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Plus button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { onPlus() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "$label Plus",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Middle Label
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Minus button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { onMinus() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "$label Minus",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
