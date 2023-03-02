package com.saurabhsandav.core.ui.barreplay.launchform

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.TimeframeLabels
import com.saurabhsandav.core.ui.common.controls.DateTimeField
import com.saurabhsandav.core.ui.common.controls.ListSelectionField
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.common.optionalContent
import com.saurabhsandav.core.utils.NIFTY50

@Composable
internal fun ReplayLaunchFormScreen(
    model: ReplayLaunchFormModel,
    onLaunchReplay: () -> Unit,
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

            ListSelectionField(
                items = TimeframeLabels,
                onSelection = { model.baseTimeframe.value = it },
                selection = model.baseTimeframe.value,
                label = { Text("Base Timeframe") },
                placeholderText = "Select Timeframe...",
                isError = model.baseTimeframe.isError,
                supportingText = optionalContent(model.baseTimeframe.errorMessage) { Text(it) },
            )

            OutlinedTextField(
                value = model.candlesBefore.value,
                onValueChange = { model.candlesBefore.value = it.trim() },
                label = { Text("Candles Before") },
                isError = model.candlesBefore.isError,
                supportingText = optionalContent(model.candlesBefore.errorMessage) { Text(it) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            DateTimeField(
                value = model.replayFrom.value,
                onValidValueChange = { model.replayFrom.value = it },
                label = { Text("Replay From") },
                isError = model.replayFrom.isError,
                supportingText = optionalContent(model.replayFrom.errorMessage) { Text(it) },
            )

            DateTimeField(
                value = model.dataTo.value,
                onValidValueChange = { model.dataTo.value = it },
                label = { Text("Data To") },
                isError = model.dataTo.isError,
                supportingText = optionalContent(model.dataTo.errorMessage) { Text(it) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text("OHLC")

                Switch(
                    checked = model.replayFullBar,
                    onCheckedChange = { model.replayFullBar = it },
                )

                Text("Full Bar")
            }

            Divider()

            ListSelectionField(
                items = NIFTY50,
                onSelection = { model.initialTicker.value = it },
                selection = model.initialTicker.value,
                label = { Text("Ticker") },
                placeholderText = "Select Ticker...",
                isError = model.initialTicker.isError,
                supportingText = optionalContent(model.initialTicker.errorMessage) { Text(it) },
            )

            Divider()

            Button(onClick = onLaunchReplay) {
                Text("Launch")
            }
        }
    }
}
