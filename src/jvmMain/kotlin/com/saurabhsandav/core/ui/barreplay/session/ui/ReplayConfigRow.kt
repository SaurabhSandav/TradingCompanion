package com.saurabhsandav.core.ui.barreplay.session.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.profiles.ProfileSwitcher

@Composable
internal fun ReplayConfigRow(
    selectedProfileId: Long?,
    onSelectProfile: (Long) -> Unit,
    onNewReplay: () -> Unit,
    onResetReplay: () -> Unit,
    onAdvanceReplay: () -> Unit,
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {

        ProfileSwitcher(
            modifier = Modifier.weight(1F),
            selectedProfileId = selectedProfileId,
            onSelectProfile = onSelectProfile,
            trainingOnly = true,
        )

        TextButton(
            modifier = Modifier.weight(1F),
            onClick = onNewReplay,
        ) {
            Text("New Replay")
        }

        TextButton(
            modifier = Modifier.weight(1F),
            onClick = onResetReplay,
        ) {
            Text("Reset Replay")
        }

        TextButton(
            modifier = Modifier.weight(1F),
            onClick = onAdvanceReplay,
        ) {
            Text("Advance Replay")
        }
    }
}
