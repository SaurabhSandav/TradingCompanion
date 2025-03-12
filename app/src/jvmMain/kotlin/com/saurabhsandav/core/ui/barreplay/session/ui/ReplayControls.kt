package com.saurabhsandav.core.ui.barreplay.session.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.theme.dimens

@Composable
internal fun ReplayControls(
    replayFullBar: Boolean,
    onAdvanceReplay: () -> Unit,
    onAdvanceReplayByBar: () -> Unit,
    isAutoNextEnabled: Boolean,
    onIsAutoNextEnabledChange: (Boolean) -> Unit,
    isTradingEnabled: Boolean,
    onBuy: () -> Unit,
    onSell: () -> Unit,
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.dimens.columnVerticalSpacing,
            alignment = Alignment.CenterVertically,
        ),
    ) {

        Button(
            onClick = onAdvanceReplay,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Advance Replay")
        }

        if (!replayFullBar) {

            Button(
                onClick = onAdvanceReplayByBar,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Advance Replay By Bar")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text("Auto next: ")

            Switch(
                checked = isAutoNextEnabled,
                onCheckedChange = onIsAutoNextEnabledChange,
            )
        }

        AnimatedVisibility(isTradingEnabled) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
            ) {

                Button(
                    modifier = Modifier.weight(1F),
                    onClick = onBuy,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColor.ProfitGreen),
                    content = { Text("BUY") },
                    enabled = isTradingEnabled,
                )

                Button(
                    modifier = Modifier.weight(1F),
                    onClick = onSell,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColor.LossRed),
                    content = { Text("SELL") },
                    enabled = isTradingEnabled,
                )
            }
        }
    }
}
