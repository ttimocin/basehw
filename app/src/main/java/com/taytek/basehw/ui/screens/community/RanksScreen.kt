package com.taytek.basehw.ui.screens.community

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taytek.basehw.R
import com.taytek.basehw.domain.model.BadgeType
import com.taytek.basehw.ui.components.RankBadgeChip
import com.taytek.basehw.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RanksScreen(
    onNavigateBack: () -> Unit,
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userScore = uiState.currentUserRankScore
    val allRanks = BadgeType.entries
    val currentUserRank = uiState.currentUserRankBadge

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ranks_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // Current user rank card - spans both columns
            item(span = { GridItemSpan(2) }) {
                CurrentUserRankCard(
                    rank = currentUserRank,
                    userScore = userScore,
                    nextRank = allRanks.getOrNull(allRanks.indexOf(currentUserRank) + 1)
                )
            }

            // All ranks grid
            itemsIndexed(allRanks) { index, rank ->
                val isUnlocked = userScore >= rank.minScoreInclusive
                RankCard(
                    rank = rank,
                    isUnlocked = isUnlocked,
                    isCurrentRank = rank == currentUserRank
                )
            }
        }
    }
}

@Composable
private fun CurrentUserRankCard(
    rank: BadgeType,
    userScore: Double,
    nextRank: BadgeType?
) {
    val rankColor = Color(rank.color)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(2.dp, rankColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.ranks_your_rank),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(12.dp))

            RankBadgeChip(
                badge = rank,
                compact = false
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.ranks_score_label, userScore.toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.ranks_current_score),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppTheme.tokens.primaryAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = rankRangeString(rank),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.ranks_score_range),
                        style = MaterialTheme.typography.labelMedium,
                        color = rankColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            if (nextRank != null) {
                Spacer(Modifier.height(16.dp))
                Divider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                    thickness = 1.dp
                )
                Spacer(Modifier.height(12.dp))

                val pointsToNext = nextRank.minScoreInclusive - userScore
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "↑",
                        color = AppTheme.tokens.primaryAccent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.ranks_next_rank, nextRank.title),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.ranks_points_needed, pointsToNext.toInt()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.tokens.primaryAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.ranks_max_rank),
                    style = MaterialTheme.typography.bodyMedium,
                    color = rankColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RankCard(
    rank: BadgeType,
    isUnlocked: Boolean,
    isCurrentRank: Boolean
) {
    val rankColor = Color(rank.color)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCurrentRank) Modifier.border(2.dp, rankColor.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) {
                rankColor.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isUnlocked) {
                RankBadgeChip(
                    badge = rank,
                    compact = true
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(rankColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔒",
                        fontSize = 18.sp
                    )
                }
            }

            Text(
                text = if (isUnlocked) rank.title else "???",
                style = MaterialTheme.typography.labelSmall,
                color = if (isUnlocked) rankColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Text(
                text = rankRangeString(rank),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                fontSize = 10.sp
            )
        }
    }
}

private fun rankRangeString(rank: BadgeType): String {
    val min = rank.minScoreInclusive.toInt()
    val max = rank.maxScoreExclusive?.let { it.toInt() - 1 } ?: "+"
    return "$min-$max"
}
