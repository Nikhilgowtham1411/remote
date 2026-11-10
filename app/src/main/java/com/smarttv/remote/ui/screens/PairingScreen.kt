package com.smarttv.remote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttv.remote.ui.components.NeonGradientButton
import com.smarttv.remote.ui.theme.*

@Composable
fun PairingScreen(
    tvName: String = "Lumio Vision 7",
    pairingCode: String,
    isLoading: Boolean,
    errorMessage: String?,
    onCodeChange: (String) -> Unit,
    onPairClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header with Back button
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
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pair with $tvName",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // TV Visual graphic with ambient glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = NeonPurple.copy(alpha = 0.5f),
                        spotColor = NeonBlue.copy(alpha = 0.6f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF101322))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(NeonPurple.copy(alpha = 0.6f), NeonBlue.copy(alpha = 0.3f))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 70.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E1333))
                            .border(1.dp, NeonPurple.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (pairingCode.isNotEmpty()) pairingCode else "• • • • • •",
                            color = NeonCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 3.dp)
                            .background(Color(0xFF2B324D), RoundedCornerShape(2.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Instructions
            Text(
                text = "Your TV is showing a 6 character code.\nEnter it below (numbers 0-9, letters A-F).",
                color = TextSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 4 Large Square Input Boxes
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 6) {
                    val char = if (i < pairingCode.length) pairingCode[i].toString() else ""
                    val isCurrentFocus = i == pairingCode.length

                    Box(
                        modifier = Modifier
                            .size(width = 46.dp, height = 62.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF131728))
                            .border(
                                width = if (isCurrentFocus) 1.8.dp else 1.dp,
                                color = if (isCurrentFocus) NeonCyan else GlassBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                focusRequester.requestFocus()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            color = TextPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Hidden text field to capture numeric keyboard input
            OutlinedTextField(
                value = pairingCode,
                onValueChange = {
                    onCodeChange(it)
                },
                modifier = Modifier
                    .size(1.dp)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, capitalization = KeyboardCapitalization.Characters),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = errorMessage,
                    color = PowerRed,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Bottom CTA & instructions
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            NeonGradientButton(
                text = if (isLoading) "Pairing..." else "Pair & Connect",
                enabled = !isLoading,
                onClick = onPairClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Can't see the code? Go back and select your TV again.\nThe TV must be on and showing the pairing screen.",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
