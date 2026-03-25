package com.taytek.basehw.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryGridItem(
    car: UserCar,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val imageUrl = car.userPhotoUrl ?: car.masterData?.imageUrl
    val feature = car.masterData?.feature?.lowercase()
    val isSthCar = feature == "sth"
    val isChaseCar = feature == "chase"
    val isThCar = feature == "th"
    val isDark = MaterialTheme.colorScheme.background == DarkNavy
    val baseColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFFFDFB)
    val darkerColor = if (isDark) Color(0xFF121416) else Color(0xFFFFF7ED)
    
    val sthBorderColor = if (isSthCar) Color(0xFFB8860B) else if (isChaseCar) Color.Black else if (isThCar) Color(0xFF71797E) else Color.Transparent
    val defaultBorderColor = if (isDark) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline
 
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .border(
                width = if (isSthCar || isChaseCar) 2.dp else 1.dp,
                color = if (isSthCar || isChaseCar) sthBorderColor else defaultBorderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(baseColor, darkerColor)
                )
            )
            .combinedClickable(
                onClick = {
                    onClick()
                },
                onLongClick = onLongClick
            )
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = car.masterData?.modelName,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            // Placeholder with car icon and gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                AppPrimary.copy(alpha = 0.15f),
                                AppPrimary.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = AppPrimary.copy(alpha = 0.5f)
                    )
                    Text(
                        text = car.masterData?.brand?.shortCode ?: "?",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppPrimary.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Model name overlay at the bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                    )
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = car.masterData?.modelName ?: "?",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Favorite badge (top-right)
        if (car.isFavorite) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Favorite",
                tint = Color(0xFFFF4D6D),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .background(Color.Black.copy(alpha = 0.3f), androidx.compose.foundation.shape.CircleShape)
                    .padding(2.dp)
            )
        }

        // Top-right badges: STH, Chase, TH, MOC
        if (isSthCar || isChaseCar || isThCar || !car.isOpened) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = if (car.isFavorite) 28.dp else 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSthCar) {
                    Surface(
                        color = Color(0xFF1A1300),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "STH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFD54F),
                                shadow = Shadow(
                                    color = Color(0xFFFFD700).copy(alpha = 0.6f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 10f
                                )
                            ),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                if (isChaseCar) {
                    Surface(
                        color = Color.Black,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "CHASE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                if (isThCar) {
                    Surface(
                        color = Color(0xFFE5E4E2),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, Color.Black.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "TH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.DarkGray
                            ),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                if (!car.isOpened) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "MOC",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        if (car.quantity > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp, bottom = 28.dp),
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = "x${car.quantity}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Selection Overlay
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppPrimary.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color.White,
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = AppPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}
