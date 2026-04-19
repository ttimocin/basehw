package com.taytek.basehw.ui.screens.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.taytek.basehw.R
import com.taytek.basehw.ui.screens.wishlist.WishlistScreen

@Composable
fun CollectionScreenWithTabs(
    selectedContentTab: Int,
    onContentTabSelected: (Int) -> Unit,
    onAddCollectionCarClick: () -> Unit,
    onCollectionCarClick: (Long) -> Unit,
    onAddWishlistCarClick: () -> Unit,
    onWishlistCarClick: (Long) -> Unit,
    onAddCarWithMasterId: (Long, Long?) -> Unit,
    initialWishlistTab: Int,
    onConsumeWishlistTab: () -> Unit,
    onStatisticsClick: () -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues? = null
) {
    val tabTitles = listOf(
        stringResource(R.string.nav_home),
        stringResource(R.string.wishlist_title)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            tabTitles.forEachIndexed { index, title ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onContentTabSelected(index) }
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = if (selectedContentTab == index) FontWeight.ExtraBold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (selectedContentTab == index) {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        val bottomPadding = contentPadding 
            ?: WindowInsets.navigationBars.asPaddingValues()
        
        when (selectedContentTab) {
            0 -> CollectionScreen(
                onAddCarClick = onAddCollectionCarClick,
                onCarClick = onCollectionCarClick,
                onStatisticsClick = onStatisticsClick,
                showHeader = false,
                contentPadding = bottomPadding
            )

            else -> WishlistScreen(
                onAddCarClick = onAddWishlistCarClick,
                onCarClick = onWishlistCarClick,
                onAddCarWithMasterId = onAddCarWithMasterId,
                initialTab = initialWishlistTab,
                onConsumeInitialTab = onConsumeWishlistTab,
                contentPadding = bottomPadding
            )
        }
    }
}
