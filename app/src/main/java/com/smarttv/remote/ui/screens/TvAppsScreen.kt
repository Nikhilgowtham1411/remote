package com.smarttv.remote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttv.remote.data.model.AppCategory
import com.smarttv.remote.data.model.TvApp
import com.smarttv.remote.ui.components.AppGridCard
import com.smarttv.remote.ui.components.BottomNavBar
import com.smarttv.remote.ui.components.NavTab
import com.smarttv.remote.ui.components.TvHeaderBar
import com.smarttv.remote.ui.theme.*

@Composable
fun TvAppsScreen(
    tvName: String = "Lumio Vision 7",
    tvModel: String = "Google TV",
    isConnected: Boolean = true,
    apps: List<TvApp>,
    selectedFilter: AppCategory,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onFilterChange: (AppCategory) -> Unit,
    onAppClick: (TvApp) -> Unit,
    onSettingsClick: () -> Unit,
    onTvSelectClick: () -> Unit,
    onNavigateTab: (NavTab) -> Unit
) {
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = "TV Apps",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search apps on TV...", color = TextSecondary, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = NeonBlue,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = Color(0xFF141724),
                    unfocusedContainerColor = Color(0xFF141724)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips: All Apps | Streaming | Installed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChipPill(
                    label = "All Apps",
                    isSelected = selectedFilter == AppCategory.ALL,
                    onClick = { onFilterChange(AppCategory.ALL) }
                )

                FilterChipPill(
                    label = "Streaming",
                    isSelected = selectedFilter == AppCategory.STREAMING,
                    onClick = { onFilterChange(AppCategory.STREAMING) }
                )

                FilterChipPill(
                    label = "Installed",
                    isSelected = selectedFilter == AppCategory.INSTALLED,
                    onClick = { onFilterChange(AppCategory.INSTALLED) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4-Column Grid of App Cards
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(apps) { app ->
                    val initial = if (app.name.isNotEmpty()) app.name.take(1).uppercase() else "A"
                    AppGridCard(
                        name = app.name,
                        accentColor = Color(app.primaryColor),
                        iconLetter = initial,
                        onClick = { onAppClick(app) }
                    )
                }
            }
        }

        // Bottom Navigation Bar
        BottomNavBar(
            selectedTab = NavTab.APPS,
            onTabSelected = onNavigateTab
        )
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) NeonBlue else Color(0xFF141724))
            .border(1.dp, if (isSelected) NeonBlue else GlassBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
