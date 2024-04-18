package com.saurabhsandav.core.ui.barreplay.session.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun ReplayConfigRow(
    onNewReplay: () -> Unit,
    replayFullBar: Boolean,
    onAdvanceReplay: () -> Unit,
    onAdvanceReplayByBar: () -> Unit,
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {

        TextButton(
            modifier = Modifier.weight(1F),
            onClick = onNewReplay,
        ) {
            Text("New Replay")
        }

        TextButton(
            modifier = Modifier.weight(1F),
            onClick = onAdvanceReplay,
        ) {
            Text("Advance Replay")
        }

        if (!replayFullBar) {

            TextButton(
                modifier = Modifier.weight(1F),
                onClick = onAdvanceReplayByBar,
            ) {
                Text("Advance Replay By Bar")
            }
        }
    }
}
