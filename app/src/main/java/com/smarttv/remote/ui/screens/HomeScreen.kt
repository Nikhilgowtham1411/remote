package com.smarttv.remote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.smarttv.remote.data.content.ContentItem
import com.smarttv.remote.data.content.ContentState
import com.smarttv.remote.data.content.Platform
import com.smarttv.remote.ui.components.BottomNavBar
import com.smarttv.remote.ui.components.NavTab
import com.smarttv.remote.ui.components.TvHeaderBar
import com.smarttv.remote.ui.theme.*

@Composable
fun HomeScreen(
    tvName: String,
    tvModel: String,
    isConnected: Boolean,
    contentState: ContentState,
    tmdbKey: String,
    onSaveKey: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenPlatform: (Platform) -> Unit,
    onPlayItem: (ContentItem) -> Unit,
    onTypeTitle: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onTvSelectClick: () -> Unit,
    onNavigateTab: (NavTab) -> Unit
) {
    var selected by remember { mutableStateOf<ContentItem?>(null) }
    var showKeyDialog by remember { mutableStateOf(false) }

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
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "What do you want to watch?",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlatformButton(
                    label = "JioHotstar",
                    brush = Brush.horizontalGradient(listOf(Color(0xFF1434CB), Color(0xFF8A2BE2))),
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenPlatform(Platform.JIOHOTSTAR) }
                )
                PlatformButton(
                    label = "Netflix",
                    brush = Brush.horizontalGradient(listOf(Color(0xFFB20710), Color(0xFFE50914))),
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenPlatform(Platform.NETFLIX) }
                )
            }
            Spacer(Modifier.height(20.dp))

            when (contentState) {
                ContentState.Idle, ContentState.Loading -> {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = NeonCyan, strokeWidth = 2.dp)
                        Text("Finding shows for you...", color = TextSecondary, fontSize = 16.sp)
                    }
                }
                ContentState.NeedsKey -> {
                    InfoCard(
                        title = "See Telugu series and movies here",
                        body = "Add a free TMDB key once (about 2 minutes) and this page will list what is on JioHotstar and Netflix.",
                        button = "Add key",
                        onClick = { showKeyDialog = true }
                    )
                }
                is ContentState.Error -> {
                    InfoCard(
                        title = "Could not load shows",
                        body = contentState.message,
                        button = "Try again",
                        onClick = onRetry,
                        secondButton = "Change key",
                        onSecondClick = { showKeyDialog = true }
                    )
                }
                is ContentState.Loaded -> {
                    contentState.rows.forEach { row ->
                        Text(
                            text = row.title,
                            color = TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(row.items, key = { "${row.title}-${it.id}" }) { item ->
                                PosterCard(item) { selected = item }
                            }
                        }
                        Spacer(Modifier.height(22.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        BottomNavBar(selectedTab = NavTab.HOME, onTabSelected = onNavigateTab)
    }

    selected?.let { item ->
        DetailDialog(
            item = item,
            onDismiss = { selected = null },
            onPlay = { onPlayItem(item); selected = null },
            onType = { onTypeTitle(item.title); selected = null }
        )
    }

    if (showKeyDialog) {
        KeyDialog(
            initial = tmdbKey,
            onDismiss = { showKeyDialog = false },
            onSave = { onSaveKey(it); showKeyDialog = false }
        )
    }
}

@Composable
private fun PlatformButton(label: String, brush: Brush, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(brush)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Open on TV", color = Color(0xCCFFFFFF), fontSize = 13.sp)
        }
    }
}

@Composable
private fun PosterCard(item: ContentItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(132.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = item.posterUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(198.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(GlassSurface)
                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    button: String,
    onClick: () -> Unit,
    secondButton: String? = null,
    onSecondClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GlassSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(body, color = TextSecondary, fontSize = 15.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)) {
                Text(button, fontSize = 16.sp)
            }
            if (secondButton != null) {
                OutlinedButton(onClick = onSecondClick) { Text(secondButton, fontSize = 16.sp) }
            }
        }
    }
}

@Composable
private fun DetailDialog(item: ContentItem, onDismiss: () -> Unit, onPlay: () -> Unit, onType: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSurface,
        title = {
            Text(item.title, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.platform.label} • ${if (item.isSeries) "Series" else "Movie"}" +
                            if (item.year.isNotBlank()) " • ${item.year}" else "",
                        color = NeonCyan,
                        fontSize = 14.sp
                    )
                    if (item.rating > 0) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Star, null, tint = Color(0xFFF5C518), modifier = Modifier.size(16.dp))
                        Text(String.format("%.1f", item.rating), color = TextPrimary, fontSize = 14.sp)
                    }
                }
                if (item.originalTitle.isNotBlank() && item.originalTitle != item.title) {
                    Spacer(Modifier.height(4.dp))
                    Text(item.originalTitle, color = TextSecondary, fontSize = 14.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = item.overview.ifBlank { "No description available." },
                    color = TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Tap the first button to open ${item.platform.label} on your TV. In its search, use the second button to type this name.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                Button(onClick = onPlay, colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)) {
                    Text("Open ${item.platform.label} on TV", fontSize = 16.sp)
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onType) { Text("Type this name on TV", fontSize = 15.sp) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary, fontSize = 16.sp) }
        }
    )
}

@Composable
private fun KeyDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSurface,
        title = { Text("Add your TMDB key", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "1. Open themoviedb.org and create a free account.\n" +
                        "2. Settings > API > request a key (choose Personal use).\n" +
                        "3. Paste the API Key here.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text("API key") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(text) }, colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)) {
                Text("Save", fontSize = 16.sp)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}
