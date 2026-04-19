package com.taytek.basehw.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taytek.basehw.R

@Composable
fun CustomBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onAddClick: () -> Unit  // Geriye dönük uyumluluk için tutuldu, artık kullanılmıyor
) {
    // Figma tasarımı: beyaz bar + hafif üst border, 5 tab, FAB YOK
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            // navigationBars alone can be 0 on first frame / gesture nav; safeDrawing includes the bottom safe area.
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp
    ) {
        // Hafif üst çizgi (Figma'daki separator)
        Column {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 0: ANASAYFA (Figma Redesign)
                NavTab(
                    icon = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                    label = stringResource(R.string.nav_anasayfa),
                    isSelected = selectedTab == 0,
                    onClick = { onTabSelected(0) }
                )
                // Tab 8: COLLECTION (Legacy/Full list)
                NavTab(
                    icon = if (selectedTab == 8) Icons.Filled.DirectionsCar else Icons.Outlined.DirectionsCar,
                    label = stringResource(com.taytek.basehw.R.string.nav_home),
                    isSelected = selectedTab == 8,
                    onClick = { onTabSelected(8) }
                )
                // Tab 7: STH
                NavTab(
                    icon = if (selectedTab == 7) Icons.Filled.Star else Icons.Outlined.Star,
                    label = stringResource(R.string.sth_label),
                    isSelected = selectedTab == 7,
                    onClick = { onTabSelected(7) }
                )
                // Tab 2: COMMUNITY
                NavTab(
                    icon = if (selectedTab == 2) Icons.Filled.Groups else Icons.Outlined.Groups,
                    label = stringResource(R.string.nav_community),
                    isSelected = selectedTab == 2,
                    onClick = { onTabSelected(2) }
                )
                // Tab 3: PROFILE
                NavTab(
                    icon = if (selectedTab == 3) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                    label = stringResource(R.string.nav_profile),
                    isSelected = selectedTab == 3,
                    onClick = { onTabSelected(3) }
                )
            }
        }
    }
}

@Composable
private fun NavTab(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
        )
    }
}
