package com.saurabhsandav.core.ui.barreplay.session.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.state

@Composable
internal fun ReplayControls(
    onNewReplay: () -> Unit,
    onResetReplay: () -> Unit,
    onAdvanceReplay: () -> Unit,
    onIsAutoNextEnabledChange: (Boolean) -> Unit,
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {

        Button(
            onClick = onNewReplay,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("New Replay")
        }

        Button(
            onClick = onResetReplay,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Reset Replay")
        }

        Button(
            onClick = onAdvanceReplay,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Advance Replay")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text("Auto next: ")

            var isAutoNextEnabled by state { false }

            Switch(
                checked = isAutoNextEnabled,
                onCheckedChange = {
                    onIsAutoNextEnabledChange(it)
                    isAutoNextEnabled = it
                },
            )
        }
    }
}
