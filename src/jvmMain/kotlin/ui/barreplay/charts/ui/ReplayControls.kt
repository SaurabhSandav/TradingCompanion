package ui.barreplay.charts.ui

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
import ui.common.state

@Composable
internal fun ReplayControls(
    onNewReplay: () -> Unit,
    onReset: () -> Unit,
    onNext: () -> Unit,
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
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Reset Replay")
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Next")
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
