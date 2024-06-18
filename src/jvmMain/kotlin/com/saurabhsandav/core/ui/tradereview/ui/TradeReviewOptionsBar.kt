package com.saurabhsandav.core.ui.tradereview.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.saurabhsandav.core.ui.common.PrimaryOptionsBar
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.Tab

@Composable
internal fun TradeReviewOptionsBar(
    selectedTab: Tab,
    isFilterEnabled: Boolean,
    onFilter: () -> Unit,
    onMarkAllTrades: () -> Unit,
    tradesAreMarked: Boolean,
    onClearMarkedTrades: () -> Unit,
) {

    PrimaryOptionsBar {

        AnimatedVisibility(selectedTab == Tab.Profile) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    space = MaterialTheme.dimens.rowHorizontalSpacing,
                )
            ) {

                OutlinedButton(
                    onClick = onFilter,
                    shape = MaterialTheme.shapes.small,
                    enabled = isFilterEnabled,
                    content = { Text("Filter") },
                )

                OutlinedButton(
                    onClick = onMarkAllTrades,
                    shape = MaterialTheme.shapes.small,
                    content = { Text("Mark All") },
                )
            }
        }

        OutlinedButton(
            onClick = onClearMarkedTrades,
            shape = MaterialTheme.shapes.small,
            enabled = tradesAreMarked,
            content = { Text("Clear Marked") },
        )
    }
}
