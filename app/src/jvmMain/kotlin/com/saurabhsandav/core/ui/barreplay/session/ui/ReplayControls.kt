package com.saurabhsandav.core.ui.barreplay.session.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
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

    Row {

        VerticalDivider()

        TextButton(
            modifier = Modifier.fillMaxHeight(),
            onClick = onAdvanceReplay,
            shape = RectangleShape,
        ) {
            Text("Advance")
        }

        if (!replayFullBar) {

            VerticalDivider()

            TextButton(
                modifier = Modifier.fillMaxHeight(),
                onClick = onAdvanceReplayByBar,
                shape = RectangleShape,
            ) {
                Text("Advance By Bar")
            }
        }

        VerticalDivider()
    }

    Row(
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

        Row {

            VerticalDivider()

            TextButton(
                modifier = Modifier.fillMaxHeight(),
                onClick = onBuy,
                colors = ButtonDefaults.textButtonColors(contentColor = AppColor.ProfitGreen),
                content = { Text("BUY") },
                enabled = isTradingEnabled,
                shape = RectangleShape,
            )

            VerticalDivider()

            TextButton(
                modifier = Modifier.fillMaxHeight(),
                onClick = onSell,
                colors = ButtonDefaults.textButtonColors(contentColor = AppColor.LossRed),
                content = { Text("SELL") },
                enabled = isTradingEnabled,
                shape = RectangleShape,
            )

            VerticalDivider()
        }
    }
}
