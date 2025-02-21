package com.saurabhsandav.core.ui.barreplay.newreplayform

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.Form
import com.saurabhsandav.core.ui.common.controls.DateTimePickerField
import com.saurabhsandav.core.ui.common.controls.OutlinedListSelectionField
import com.saurabhsandav.core.ui.common.errorsMessagesAsSupportingText
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.profiles.ProfileSelectorField
import com.saurabhsandav.core.ui.tickerselectiondialog.TickerSelectionField
import com.saurabhsandav.core.ui.tickerselectiondialog.TickerSelectionType
import com.saurabhsandav.core.utils.NIFTY500

@Composable
internal fun NewReplayForm(
    model: NewReplayFormModel,
) {

    Form {

        val initialFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) { initialFocusRequester.requestFocus() }

        OutlinedListSelectionField(
            modifier = Modifier.focusRequester(initialFocusRequester),
            items = remember { enumValues<Timeframe>().toList() },
            itemText = { it.toLabel() },
            onSelect = { model.baseTimeframeField.value = it },
            selection = model.baseTimeframeField.value,
            label = { Text("Base Timeframe") },
            placeholderText = "Select Timeframe...",
            isError = model.baseTimeframeField.isError,
            supportingText = model.baseTimeframeField.errorsMessagesAsSupportingText(),
        )

        OutlinedTextField(
            value = model.candlesBeforeField.value,
            onValueChange = { model.candlesBeforeField.value = it.trim() },
            label = { Text("Candles Before") },
            isError = model.candlesBeforeField.isError,
            supportingText = model.candlesBeforeField.errorsMessagesAsSupportingText(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        DateTimePickerField(
            value = model.replayFromField.value,
            onValidValueChange = { model.replayFromField.value = it },
            label = { Text("Replay From") },
            isError = model.replayFromField.isError,
            supportingText = model.replayFromField.errorsMessagesAsSupportingText(),
        )

        DateTimePickerField(
            value = model.dataToField.value,
            onValidValueChange = { model.dataToField.value = it },
            label = { Text("Data To") },
            isError = model.dataToField.isError,
            supportingText = model.dataToField.errorsMessagesAsSupportingText(),
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {

            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                onClick = { model.replayFullBar = false },
                selected = !model.replayFullBar,
                label = { Text("OHLC") },
            )

            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                onClick = { model.replayFullBar = true },
                selected = model.replayFullBar,
                // SegmentedButton breaks it into 2 lines. Forcing single line causes weird layout.
                // Works fine with NBSP.
                label = { Text("Full\u00A0Bar") },
            )
        }

        HorizontalDivider()

        TickerSelectionField(
            type = TickerSelectionType.Regular,
            tickers = NIFTY500,
            selected = model.initialTickerField.value,
            onSelect = { model.initialTickerField.value = it },
            isError = model.initialTickerField.isError,
            supportingText = model.initialTickerField.errorsMessagesAsSupportingText(),
        )

        ProfileSelectorField(
            selectedProfileId = model.profileField.value,
            onProfileSelected = { model.profileField.value = it },
            trainingOnly = true,
        )

        HorizontalDivider()

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = model.validator::submit,
            enabled = model.validator.canSubmit,
            content = { Text("Launch") },
        )
    }
}
