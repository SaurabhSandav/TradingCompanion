package ui.closetradeform

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import com.saurabhsandav.core.TradeTag
import kotlinx.coroutines.flow.collectLatest
import optionalContent
import ui.common.OutlinedTextField
import ui.common.app.AppWindow
import ui.common.controls.DateTimeField
import ui.common.controls.ListSelectionField
import ui.common.flow.FlowRow
import ui.common.form.isError
import ui.common.state
import utils.NIFTY50

@Composable
internal fun CloseTradeFormWindow(
    state: CloseTradeFormWindowState,
) {

    val windowState = rememberWindowState()

    AppWindow(
        onCloseRequest = state.params.onCloseRequest,
        state = windowState,
        title = "Close Trade",
    ) {

        Box(Modifier.wrapContentSize()) {

            when {
                state.isReady -> MainForm(
                    model = state.model,
                    showDetails = state.showDetails,
                    onShowDetails = state::onShowDetails,
                    detailModel = state.detailModel,
                    onCalculateMFE = state::onCalculateMFE,
                    onCalculateMAE = state::onCalculateMAE,
                    tagsSuggestionProvider = state::getSuggestedTags,
                    onAddTag = state::onAddTag,
                    onSelectTag = state::onSelectTag,
                    onRemoveTag = state::onRemoveTag,
                    onSaveTrade = state::onSaveTrade,
                )

                else -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun MainForm(
    model: CloseTradeFormModel,
    showDetails: Boolean,
    onShowDetails: () -> Unit,
    detailModel: CloseTradeDetailFormModel?,
    onCalculateMFE: () -> Unit,
    onCalculateMAE: () -> Unit,
    tagsSuggestionProvider: suspend (String) -> List<TradeTag>,
    onAddTag: (String) -> Unit,
    onSelectTag: (Long) -> Unit,
    onRemoveTag: (Long) -> Unit,
    onSaveTrade: () -> Unit,
) {

    Column(
        modifier = Modifier.padding(16.dp).width(TextFieldDefaults.MinWidth).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        ListSelectionField(
            items = NIFTY50,
            onSelection = { model.ticker.value = it },
            selection = model.ticker.value,
            label = { Text("Ticker") },
            isError = model.ticker.isError,
            errorText = optionalContent(model.ticker.errorMessage) { Text(it) },
        )

        OutlinedTextField(
            value = model.quantity.value,
            onValueChange = { model.quantity.value = it.trim() },
            label = { Text("Quantity") },
            isError = model.quantity.isError,
            errorText = optionalContent(model.quantity.errorMessage) { Text(it) },
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text("Short")

            Switch(
                checked = model.isLong.value,
                onCheckedChange = { model.isLong.value = it },
            )

            Text("Long")
        }

        OutlinedTextField(
            value = model.entry.value,
            onValueChange = { model.entry.value = (it.trim()) },
            label = { Text("Entry") },
            isError = model.entry.isError,
            errorText = optionalContent(model.entry.errorMessage) { Text(it) },
            singleLine = true,
        )

        OutlinedTextField(
            value = model.stop.value,
            onValueChange = { model.stop.value = (it.trim()) },
            label = { Text("Stop") },
            isError = model.stop.isError,
            errorText = optionalContent(model.stop.errorMessage) { Text(it) },
            singleLine = true,
        )

        DateTimeField(
            value = model.entryDateTime.value,
            onValidValueChange = { model.entryDateTime.value = it },
            label = { Text("Entry DateTime") },
            isError = model.entryDateTime.isError,
            errorText = optionalContent(model.entryDateTime.errorMessage) { Text(it) },
        )

        OutlinedTextField(
            value = model.target.value,
            onValueChange = { model.target.value = it.trim() },
            label = { Text("Target") },
            isError = model.target.isError,
            errorText = optionalContent(model.target.errorMessage) { Text(it) },
            singleLine = true,
        )

        val focusRequester = remember { FocusRequester() }

        OutlinedTextField(
            modifier = Modifier.focusRequester(focusRequester),
            value = model.exit.value,
            onValueChange = { model.exit.value = it.trim() },
            label = { Text("Exit") },
            isError = model.exit.isError,
            errorText = optionalContent(model.exit.errorMessage) { Text(it) },
            singleLine = true,
        )

        if (detailModel == null) {

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        DateTimeField(
            value = model.exitDateTime.value,
            onValidValueChange = { model.exitDateTime.value = it },
            label = { Text("Exit DateTime") },
            isError = model.exitDateTime.isError,
            errorText = optionalContent(model.exitDateTime.errorMessage) { Text(it) },
        )

        if (detailModel != null) {

            AnimatedVisibility(
                visible = !showDetails,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {

                Button(onClick = onShowDetails) {

                    Text("Show Details")
                }
            }

            AnimatedVisibility(showDetails) {

                DetailForm(
                    model = detailModel,
                    onCalculateMFE = onCalculateMFE,
                    onCalculateMAE = onCalculateMAE,
                    tagsSuggestionProvider = tagsSuggestionProvider,
                    onAddTag = onAddTag,
                    onSelectTag = onSelectTag,
                    onRemoveTag = onRemoveTag,
                )
            }
        }

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onSaveTrade,
        ) {

            Text("Add")
        }
    }
}

@Composable
private fun DetailForm(
    model: CloseTradeDetailFormModel,
    onCalculateMFE: () -> Unit,
    onCalculateMAE: () -> Unit,
    onAddTag: (String) -> Unit,
    onSelectTag: (Long) -> Unit,
    onRemoveTag: (Long) -> Unit,
    tagsSuggestionProvider: suspend (String) -> List<TradeTag>,
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        Divider(Modifier.padding(16.dp))

        OutlinedTextField(
            value = model.maxFavorableExcursion.value,
            onValueChange = { model.maxFavorableExcursion.value = it.trim() },
            label = { Text("Max Favorable Excursion") },
            isError = model.maxFavorableExcursion.isError,
            errorText = optionalContent(model.maxFavorableExcursion.errorMessage) { Text(it) },
            singleLine = true,
            trailingIcon = {
                TextButton(onClick = onCalculateMFE) {
                    Text("CALCULATE")
                }
            },
        )

        OutlinedTextField(
            value = model.maxAdverseExcursion.value,
            onValueChange = { model.maxAdverseExcursion.value = it.trim() },
            label = { Text("Max Adverse Excursion") },
            isError = model.maxAdverseExcursion.isError,
            errorText = optionalContent(model.maxAdverseExcursion.errorMessage) { Text(it) },
            singleLine = true,
            trailingIcon = {
                TextButton(onClick = onCalculateMAE) {
                    Text("CALCULATE")
                }
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text("Persisted")

            Switch(
                checked = model.persisted,
                onCheckedChange = { model.persisted = it },
            )
        }

        TagsBox(
            tags = model.tags,
            onAddTag = onAddTag,
            onSelectTag = onSelectTag,
            onRemoveTag = onRemoveTag,
            tagsSuggestionProvider = tagsSuggestionProvider,
        )

        OutlinedTextField(
            value = model.notes.value,
            onValueChange = { model.notes.value = it.trim() },
            label = { Text("Notes") },
        )
    }
}

@Composable
private fun TagsBox(
    tags: List<TradeTag>,
    onAddTag: (String) -> Unit,
    onSelectTag: (Long) -> Unit,
    onRemoveTag: (Long) -> Unit,
    tagsSuggestionProvider: suspend (String) -> List<TradeTag>,
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        var showTextField by state { false }

        FlowRow(
            mainAxisSpacing = 8.dp,
        ) {

            tags.forEach { tag ->

                InputChip(
                    onClick = {},
                    label = { Text(tag.name) },
                    trailingIcon = {
                        IconButton(
                            onClick = { onRemoveTag(tag.id) },
                            modifier = Modifier.size(InputChipDefaults.IconSize),
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove tag")
                        }
                    },
                )
            }

            InputChip(
                onClick = { showTextField = !showTextField },
                label = { Text("Add Tag") },
                leadingIcon = {

                    Crossfade(showTextField) {

                        Icon(
                            imageVector = if (showTextField) Icons.Default.Remove else Icons.Default.Add,
                            contentDescription = if (showTextField) "Remove" else "Add",
                            modifier = Modifier.size(InputChipDefaults.IconSize),
                        )
                    }
                },
            )
        }

        AnimatedVisibility(showTextField) {

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {

                Divider()

                Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {

                    var text by state { "" }

                    androidx.compose.material3.OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        trailingIcon = {
                            TextButton(
                                onClick = {
                                    onAddTag(text)
                                    text = ""
                                    showTextField = false
                                },
                            ) {
                                Text("ADD")
                            }
                        }
                    )

                    val suggestions by produceState<List<TradeTag>>(emptyList()) {
                        snapshotFlow { text }.collectLatest { value = tagsSuggestionProvider(text) }
                    }

                    DropdownMenu(
                        expanded = suggestions.isNotEmpty(),
                        focusable = false,
                        onDismissRequest = { text = "" },
                    ) {

                        suggestions.forEach { tag ->

                            DropdownMenuItem(
                                modifier = Modifier.background(MaterialTheme.colorScheme.background),
                                onClick = {
                                    text = ""
                                    onSelectTag(tag.id)
                                },
                            ) {

                                Text(tag.name)
                            }
                        }
                    }
                }
            }
        }
    }
}
