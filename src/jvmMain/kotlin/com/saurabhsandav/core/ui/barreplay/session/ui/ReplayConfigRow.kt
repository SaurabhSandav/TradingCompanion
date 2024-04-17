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
import com.saurabhsandav.core.ui.profiles.ProfileSwitcherBox

@Composable
internal fun ReplayConfigRow(
    selectedProfileId: ProfileId?,
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

        var profileSwitcherExpanded by state { false }

        ProfileSwitcherBox(
            expanded = profileSwitcherExpanded,
            onExpandedChange = { profileSwitcherExpanded = it },
            selectedProfileId = selectedProfileId,
            onProfileSelected = onProfileSelected,
            trainingOnly = true,
        ) { profileName ->

            TextButton(
                modifier = Modifier.weight(1F),
                onClick = { profileSwitcherExpanded = true },
                content = { Text("Profile: ${profileName ?: "None"}") },
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
