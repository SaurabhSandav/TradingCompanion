package com.saurabhsandav.core.ui.barreplay.session.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.profiles.ProfileSelectorDialog

@Composable
internal fun ReplayConfigRow(
    selectedProfileId: ProfileId?,
    selectedProfileName: String?,
    onProfileSelected: (ProfileId?) -> Unit,
    onNewReplay: () -> Unit,
    replayFullBar: Boolean,
    onAdvanceReplay: () -> Unit,
    onAdvanceReplayByBar: () -> Unit,
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {

        var showProfileSelector by state { false }

        TextButton(
            modifier = Modifier.weight(1F),
            onClick = { showProfileSelector = true },
            content = { Text("Profile: ${selectedProfileName ?: "None"}") },
        )

        if (showProfileSelector) {

            ProfileSelectorDialog(
                onCloseRequest = { showProfileSelector = false },
                selectedProfileId = selectedProfileId,
                onProfileSelected = onProfileSelected,
                trainingOnly = true,
            )
        }

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
