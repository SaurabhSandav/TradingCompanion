package com.saurabhsandav.core.ui.tradereview.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.profiles.ProfileSelectorDialog
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState

@Composable
internal fun MainTabRow(
    selectedTab: TradeReviewState.Tab,
    onSelectTab: (TradeReviewState.Tab) -> Unit,
    selectedProfileId: ProfileId?,
    selectedProfileName: String?,
    onProfileSelected: (ProfileId?) -> Unit,
) {

    SecondaryTabRow(
        selectedTabIndex = if (selectedTab == TradeReviewState.Tab.Profile) 0 else 1,
    ) {

        Tab(
            selected = selectedTab == TradeReviewState.Tab.Profile,
            onClick = { onSelectTab(TradeReviewState.Tab.Profile) },
            text = {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    var showProfileSelector by state { false }

                    Text(
                        modifier = Modifier.weight(1F),
                        text = "Profile: ${selectedProfileName ?: "None"}",
                        textAlign = TextAlign.Center,
                    )

                    IconButtonWithTooltip(
                        onClick = { showProfileSelector = true },
                        tooltipText = "Open Profile switcher",
                    ) {

                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Open Profile switcher",
                        )
                    }

                    if (showProfileSelector) {

                        ProfileSelectorDialog(
                            onDismissRequest = { showProfileSelector = false },
                            selectedProfileId = selectedProfileId,
                            onProfileSelected = onProfileSelected,
                        )
                    }
                }
            },
        )

        Tab(
            selected = selectedTab == TradeReviewState.Tab.Marked,
            onClick = { onSelectTab(TradeReviewState.Tab.Marked) },
            text = { Text("Marked") },
        )
    }
}
