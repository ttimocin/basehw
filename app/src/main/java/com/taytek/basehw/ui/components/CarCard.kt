package com.taytek.basehw.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.ui.theme.*
import com.taytek.basehw.ui.theme.MajoretteYellow
import com.taytek.basehw.ui.theme.JadaPurple

@Composable
fun CarCard(
    car: UserCar,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == DarkNavy
    val baseColor = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer
    val darkerColor = if (isDark) Color(0xFF121416) else Color(0xFFE2E8F0)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(baseColor, darkerColor)
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Square thumbnail with 20dp corners
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val imageUrl = car.userPhotoUrl ?: car.masterData?.imageUrl
                    if (!imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = car.masterData?.modelName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // White Text Stack
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = car.masterData?.modelName ?: "Unknown Model",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    val subtitle = car.masterData?.series?.takeIf { it.isNotBlank() }
                        ?: car.manualSeries?.takeIf { it.isNotBlank() }
                        ?: car.masterData?.brand?.displayName
                        ?: "Mainline"
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val boxStatus = if (car.isOpened) "Açık" else "Kutulu"
                    val brandStr = car.masterData?.brand?.shortCode ?: ""
                    val brandPrefix = if (brandStr.isNotEmpty()) "$brandStr • " else ""
                    val countLabel = "$brandPrefix${car.masterData?.year ?: "Belirsiz"} • $boxStatus"

                    Text(
                        text = countLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                    )
                }
            }

            // Regular / Premium badge — sadece Hot Wheels için
            if (car.masterData?.brand == Brand.HOT_WHEELS) {
                val isPremium = car.masterData?.isPremium == true
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isPremium) "🏁 Premium" else "Regular",
                        style = MaterialTheme.typography.labelSmall,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
