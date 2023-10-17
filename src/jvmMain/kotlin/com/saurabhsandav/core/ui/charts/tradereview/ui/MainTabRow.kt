package com.saurabhsandav.core.ui.charts.tradereview.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.profiles.ProfileSwitcherBox

@Composable
internal fun MainTabRow(
    selectedTab: TradeReviewState.Tab,
    onSelectTab: (TradeReviewState.Tab) -> Unit,
    selectedProfileId: ProfileId?,
    onSelectProfile: (ProfileId) -> Unit,
) {

    TabRow(
        selectedTabIndex = if (selectedTab == TradeReviewState.Tab.Profile) 0 else 1,
    ) {

        Tab(
            selected = selectedTab == TradeReviewState.Tab.Profile,
            onClick = { onSelectTab(TradeReviewState.Tab.Profile) },
            text = {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    var profileSwitcherExpanded by state { false }
                    var currentProfileName by state<String?> { null }

                    Text(
                        modifier = Modifier.weight(1F),
                        text = "Profile: ${currentProfileName ?: "None"}",
                        textAlign = TextAlign.Center,
                    )

                    ProfileSwitcherBox(
                        expanded = profileSwitcherExpanded,
                        onExpandedChange = { profileSwitcherExpanded = it },
                        selectedProfileId = selectedProfileId,
                        onSelectProfile = onSelectProfile,
                    ) { profileName ->

                        SideEffect { currentProfileName = profileName }

                        IconButton(onClick = { profileSwitcherExpanded = true }) {

                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Open Profile switcher",
                            )
                        }
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
