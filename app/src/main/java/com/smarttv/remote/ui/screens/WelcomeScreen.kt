package com.smarttv.remote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttv.remote.ui.components.NeonGradientButton
import com.smarttv.remote.ui.theme.BgDark
import com.smarttv.remote.ui.theme.GlassBorder
import com.smarttv.remote.ui.theme.NeonBlue
import com.smarttv.remote.ui.theme.NeonCyan
import com.smarttv.remote.ui.theme.NeonPurple
import com.smarttv.remote.ui.theme.TextPrimary
import com.smarttv.remote.ui.theme.TextSecondary

@Composable
fun WelcomeScreen(
    onConnectTvClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            // TV Ambient Display Mockup Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = NeonPurple.copy(alpha = 0.5f),
                        spotColor = NeonBlue.copy(alpha = 0.6f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF101322))
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            listOf(NeonPurple.copy(alpha = 0.7f), NeonBlue.copy(alpha = 0.3f))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // TV screen mockup inside
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 170.dp, height = 96.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF2C194D), Color(0xFF101322))
                                )
                            )
                            .border(1.dp, NeonPurple.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LUMIO VISION 7",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    // TV Stand base
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 50.dp, height = 4.dp)
                            .background(Color(0xFF2B324D), RoundedCornerShape(2.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Title
            Text(
                text = "Control Your TV\nFrom Your Phone",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 30.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 4 Benefits
            BenefitRow(
                icon = Icons.Default.Wifi,
                text = "Works over Wi-Fi"
            )
            Spacer(modifier = Modifier.height(14.dp))

            BenefitRow(
                icon = Icons.Default.PlayCircleOutline,
                text = "No TV app required"
            )
            Spacer(modifier = Modifier.height(14.dp))

            BenefitRow(
                icon = Icons.Default.TouchApp,
                text = "Easy & Big Buttons"
            )
            Spacer(modifier = Modifier.height(14.dp))

            BenefitRow(
                icon = Icons.Default.People,
                text = "For Everyone (Simple UI)"
            )
        }

        // Bottom CTA Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp)
        ) {
            NeonGradientButton(
                text = "Connect Your TV",
                onClick = onConnectTvClick,
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Make sure your phone and TV are connected to the same Wi-Fi.",
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun BenefitRow(
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFF141A2D))
                .border(1.dp, NeonBlue.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
