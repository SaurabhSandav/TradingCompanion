package com.saurabhsandav.core.ui.barreplay.session.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.AppColor

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
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
