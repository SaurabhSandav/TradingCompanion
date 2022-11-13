package ui.barreplay.launchform

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.common.controls.DateTimeField
import ui.common.controls.ListSelectionField
import ui.common.form.rememberFormScope
import utils.NIFTY50

@Composable
internal fun ReplayLaunchFormScreen(
    formModel: ReplayLaunchFormFields.Model,
    onLaunchReplay: (ReplayLaunchFormFields.Model) -> Unit,
) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {

        Column(
            modifier = Modifier.width(IntrinsicSize.Min),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            val formScope = rememberFormScope()

            val fields = remember {
                ReplayLaunchFormFields(
                    formScope = formScope,
                    initial = formModel,
                )
            }

            ListSelectionField(
                items = listOf("5m", "1D"),
                onSelection = fields.baseTimeframe.onSelectionChange,
                selection = fields.baseTimeframe.value,
                isError = fields.baseTimeframe.isError,
                label = { Text("Base Timeframe") },
                placeholderText = "Select Timeframe...",
            )

            DateTimeField(
                value = fields.dataFrom.value,
                onValidValueChange = fields.dataFrom.onValueChange,
                label = { Text("Data From") },
            )

            DateTimeField(
                value = fields.dataTo.value,
                onValidValueChange = fields.dataTo.onValueChange,
                label = { Text("Data To") },
            )

            DateTimeField(
                value = fields.replayFrom.value,
                onValidValueChange = fields.replayFrom.onValueChange,
                label = { Text("Replay From") },
            )

            Divider()

            ListSelectionField(
                items = NIFTY50,
                onSelection = fields.initialSymbol.onSelectionChange,
                selection = fields.initialSymbol.value,
                isError = fields.initialSymbol.isError,
                label = { Text("Initial Ticker") },
                placeholderText = "Select Ticker...",
            )

            Divider()

            Button(onClick = { fields.getModelIfValidOrNull()?.let(onLaunchReplay) }) {
                Text("Launch")
            }
        }
    }
}
