package com.smarttv.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smarttv.remote.ui.theme.CardGlassGradient
import com.smarttv.remote.ui.theme.GlassBorder
import com.smarttv.remote.ui.theme.GlassBorderSelected

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderModifier = if (isSelected) {
        Modifier.border(width = 1.5.dp, color = GlassBorderSelected, shape = shape)
    } else {
        Modifier.border(width = 1.dp, color = GlassBorder, shape = shape)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(CardGlassGradient)
            .then(borderModifier)
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            ),
        content = content
    )
}
