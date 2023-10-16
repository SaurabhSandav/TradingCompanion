package com.saurabhsandav.core.ui.barreplay.newreplayform

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.controls.DateTimePickerField
import com.saurabhsandav.core.ui.common.controls.OutlinedListSelectionField
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.utils.NIFTY50
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun NewReplayForm(
    model: NewReplayFormModel,
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

            val initialFocusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) { initialFocusRequester.requestFocus() }

            OutlinedListSelectionField(
                modifier = Modifier.focusRequester(initialFocusRequester),
                items = remember { persistentListOf(*enumValues<Timeframe>()) },
                itemText = { it.toLabel() },
                onSelection = { model.baseTimeframeField.value = it },
                selection = model.baseTimeframeField.value,
                label = { Text("Base Timeframe") },
                placeholderText = "Select Timeframe...",
                isError = model.baseTimeframeField.isError,
                supportingText = model.baseTimeframeField.errorMessage?.let { { Text(it) } },
            )

            OutlinedTextField(
                value = model.candlesBeforeField.value,
                onValueChange = { model.candlesBeforeField.value = it.trim() },
                label = { Text("Candles Before") },
                isError = model.candlesBeforeField.isError,
                supportingText = model.candlesBeforeField.errorMessage?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            DateTimePickerField(
                value = model.replayFromField.value,
                onValidValueChange = { model.replayFromField.value = it },
                label = { Text("Replay From") },
                isError = model.replayFromField.isError,
                supportingText = model.replayFromField.errorMessage?.let { { Text(it) } },
            )

            DateTimePickerField(
                value = model.dataToField.value,
                onValidValueChange = { model.dataToField.value = it },
                label = { Text("Data To") },
                isError = model.dataToField.isError,
                supportingText = model.dataToField.errorMessage?.let { { Text(it) } },
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

            OutlinedListSelectionField(
                items = NIFTY50,
                itemText = { it },
                onSelection = { model.initialTickerField.value = it },
                selection = model.initialTickerField.value,
                label = { Text("Ticker") },
                placeholderText = "Select Ticker...",
                isError = model.initialTickerField.isError,
                supportingText = model.initialTickerField.errorMessage?.let { { Text(it) } },
            )

            Divider()

            Button(
                onClick = onLaunchReplay,
                enabled = model.validator.isValid,
                content = { Text("Launch") },
            )
        }
    }
}
