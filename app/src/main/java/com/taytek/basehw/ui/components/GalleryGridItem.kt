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
    
    val defaultBorderColor = if (isDark) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline

    // Animated shimmer beam for STH cards
    val infiniteTransition = rememberInfiniteTransition(label = "sth_shimmer")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sth_angle"
    )

    val sthGoldDark = Color(0xFF8B6914)
    val sthGoldMid = Color(0xFFB8860B)

    val cardShape = RoundedCornerShape(14.dp)

    // Generate animated sweep gradient brush for STH border
    val shimmerBorderModifier = if (isSthCar && !isSelected) {
        val fraction = angle / 360f
        val shimmerColors = buildList {
            val beamWidth = 0.12f
            val steps = 48
            for (i in 0 until steps) {
                val t = i.toFloat() / steps
                val dist = kotlin.math.min(
                    kotlin.math.abs(t - fraction),
                    kotlin.math.min(
                        kotlin.math.abs(t - fraction + 1f),
                        kotlin.math.abs(t - fraction - 1f)
                    )
                )
                val brightness = (1f - (dist / beamWidth).coerceIn(0f, 1f))
                val color = androidx.compose.ui.graphics.lerp(sthGoldMid, Color.White, brightness * brightness)
                add(color)
            }
            add(first()) // close the loop
        }
        val shimmerBrush = Brush.sweepGradient(shimmerColors)
        Modifier.border(2.5.dp, shimmerBrush, cardShape)
    } else if (isChaseCar && !isSelected) {
        val chaseColor = if (isDark) Color.White else Color.Black
        Modifier.border(2.dp, chaseColor, cardShape)
    } else if (!isSelected) {
        Modifier.border(1.dp, defaultBorderColor.copy(alpha = 0.05f), cardShape)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .then(shimmerBorderModifier)

            .clip(cardShape)
            .shadow(elevation = if (isSelected) 0.dp else 1.dp, shape = cardShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .then(
                if (!isSelected) {
                    Modifier.background(Brush.linearGradient(colors = listOf(baseColor, darkerColor)))
                } else {
                    Modifier
                }
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

                if (car.isCustom) {
                    Surface(
                        color = Color(0xFF4CAF50), // Green for custom
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "CUSTOM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp
                            ),
                            color = Color.White,
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
