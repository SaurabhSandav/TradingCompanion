package com.saurabhsandav.core.ui.trades.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.PrimaryOptionsBar

@Composable
internal fun TradesOptionsBar(
    isFocusModeEnabled: Boolean,
    onSetFocusModeEnabled: (Boolean) -> Unit,
    onFilter: () -> Unit,
    onNewExecution: () -> Unit,
) {

    PrimaryOptionsBar {

        SingleChoiceSegmentedButtonRow {

            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                onClick = { onSetFocusModeEnabled(false) },
                selected = !isFocusModeEnabled,
                label = { Text("All") },
            )

            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                onClick = { onSetFocusModeEnabled(true) },
                selected = isFocusModeEnabled,
                label = { Text("Focus") },
            )
        }

        Spacer(Modifier.weight(1F))

        OutlinedButton(
            onClick = onFilter,
            shape = MaterialTheme.shapes.small,
            content = { Text("Filter") },
        )

        Button(
            onClick = onNewExecution,
            shape = MaterialTheme.shapes.small,
            content = { Text("New Execution") },
        )
    }
}
