package com.saurabhsandav.core.ui.stockchart.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.controls.TimePickerDialog
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.icons.AppIcons
import com.saurabhsandav.core.ui.icons.EventUpcoming
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartDecorationType
import com.saurabhsandav.core.ui.stockchart.StockChartsSyncPrefs
import com.saurabhsandav.core.utils.nowIn
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
internal fun StockChartTopBar(
    stockChart: StockChart,
    decorationType: StockChartDecorationType,
    onOpenTickerSelection: () -> Unit,
    onOpenTimeframeSelection: () -> Unit,
    onGoToDateTime: (LocalDateTime) -> Unit,
    onGoToLatest: () -> Unit,
    layout: ChartsLayout,
    onSetLayout: (ChartsLayout) -> Unit,
    syncPrefs: StockChartsSyncPrefs,
    onToggleSyncCrosshair: (Boolean?) -> Unit,
    onToggleSyncTime: (Boolean?) -> Unit,
    onToggleSyncDateRange: (Boolean?) -> Unit,
    onNewWindow: () -> Unit,
) {

    Row(
        modifier = Modifier.height(48.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        TextButton(
            onClick = onOpenTickerSelection,
            icon = {

                Icon(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                )
            },
            text = { Text(stockChart.params.ticker) },
        )

        VerticalDivider()

        TextButton(
            onClick = onOpenTimeframeSelection,
            icon = {

                Icon(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                )
            },
            text = { Text(stockChart.params.timeframe.toLabel()) },
        )

        VerticalDivider()

        GoTo(
            modifier = Modifier.fillMaxHeight(),
            onGoToDateTime = onGoToDateTime,
        )

        VerticalDivider()

        IconButtonWithTooltip(
            modifier = Modifier.fillMaxHeight(),
            onClick = onGoToLatest,
            tooltipText = "Go to latest",
//            shape = RectangleShape, TODO CMP 1.8
            content = { Icon(Icons.AutoMirrored.Filled.LastPage, contentDescription = "Go to latest") },
        )

        VerticalDivider()

        if (decorationType is StockChartDecorationType.Charts) {

            TextButton(
                onClick = decorationType.onOpenTradeReview,
                text = { Text("Trade Review") },
            )
        }

        VerticalDivider()

        Spacer(Modifier.weight(1F))

        VerticalDivider()

        ChartLayout(
            layout = layout,
            onSetLayout = onSetLayout,
        )

        VerticalDivider()

        Sync(
            modifier = Modifier.fillMaxHeight(),
            syncPrefs = syncPrefs,
            onToggleSyncCrosshair = onToggleSyncCrosshair,
            onToggleSyncTime = onToggleSyncTime,
            onToggleSyncDateRange = onToggleSyncDateRange,
        )

        VerticalDivider()

        IconButtonWithTooltip(
            modifier = Modifier.fillMaxHeight(),
            onClick = onNewWindow,
            tooltipText = "New window",
            content = { Icon(Icons.Default.OpenInBrowser, contentDescription = "New window") },
        )
    }
}

@Composable
private fun TextButton(
    onClick: () -> Unit,
    icon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    text: @Composable () -> Unit,
) {

    TextButton(
        modifier = Modifier.fillMaxHeight(),
        onClick = onClick,
        shape = RectangleShape,
        enabled = enabled,
        contentPadding = when {
            icon == null -> ButtonDefaults.TextButtonContentPadding
            else -> ButtonDefaults.TextButtonWithIconContentPadding
        },
    ) {

        if (icon != null) {

            icon()

            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        }

        text()
    }
}

@Composable
private fun GoTo(
    modifier: Modifier,
    onGoToDateTime: (LocalDateTime) -> Unit,
) {

    var showDateDialog by state { false }
    var showTimeDialog by state { false }

    var goToDateTime by state(showDateDialog || showTimeDialog) {
        Clock.System.nowIn(TimeZone.currentSystemDefault())
    }

    IconButtonWithTooltip(
        modifier = modifier,
        onClick = { showDateDialog = true },
        tooltipText = "Go to",
//            shape = RectangleShape, TODO CMP 1.8
        content = { Icon(AppIcons.EventUpcoming, contentDescription = "Go to") },
    )

    if (showDateDialog) {

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember {
                goToDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            },
            selectableDates = remember {

                val currentMillis = Clock.System.now().toEpochMilliseconds()
                val currentTime = Clock.System.nowIn(TimeZone.currentSystemDefault())

                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= currentMillis

                    override fun isSelectableYear(year: Int): Boolean = year <= currentTime.year
                }
            },
        )

        DatePickerDialog(
            onDismissRequest = { showDateDialog = false },
            confirmButton = {

                TextButton(
                    onClick = onClick@{

                        val selectedDateMillis = datePickerState.selectedDateMillis ?: return@onClick

                        goToDateTime = Instant.fromEpochMilliseconds(selectedDateMillis)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date
                            .atTime(goToDateTime.time)

                        showDateDialog = false
                        showTimeDialog = true
                    },
                    content = { Text("Next") },
                )
            },
            dismissButton = {

                TextButton(
                    onClick = { showDateDialog = false },
                    content = { Text("Cancel") },
                )
            },
        ) {

            DatePicker(state = datePickerState)
        }
    }

    if (showTimeDialog) {

        val timePickerState = rememberTimePickerState(
            initialHour = goToDateTime.hour,
            initialMinute = goToDateTime.minute,
            is24Hour = true,
        )
        var showingPicker by state { true }

        TimePickerDialog(
            title = when {
                showingPicker -> "Select Time"
                else -> "Enter Time"
            },
            onCancel = { showTimeDialog = false },
            onConfirm = {

                goToDateTime = goToDateTime.date.atTime(
                    hour = timePickerState.hour,
                    minute = timePickerState.minute,
                )

                showTimeDialog = false

                onGoToDateTime(goToDateTime)
            },
            toggle = {

                val hint = when {
                    showingPicker -> "Switch to Text Input"
                    else -> "Switch to Picker Input"
                }

                IconButtonWithTooltip(
                    onClick = { showingPicker = !showingPicker },
                    tooltipText = hint,
                ) {

                    Icon(
                        imageVector = when {
                            showingPicker -> Icons.Outlined.Keyboard
                            else -> Icons.Outlined.Schedule
                        },
                        contentDescription = hint,
                    )
                }
            },
        ) {

            when {
                showingPicker -> TimePicker(state = timePickerState)
                else -> TimeInput(state = timePickerState)
            }
        }
    }
}

@Composable
private fun Sync(
    modifier: Modifier,
    syncPrefs: StockChartsSyncPrefs,
    onToggleSyncCrosshair: (Boolean?) -> Unit,
    onToggleSyncTime: (Boolean?) -> Unit,
    onToggleSyncDateRange: (Boolean?) -> Unit,
) {

    var expanded by state { false }

    Box(
        modifier = Modifier.height(IntrinsicSize.Max).then(modifier),
        contentAlignment = Alignment.Center,
    ) {

        IconButtonWithTooltip(
            onClick = { expanded = true },
            tooltipText = "Sync",
//            shape = RectangleShape, TODO CMP 1.8
            content = { Icon(Icons.Default.Sync, contentDescription = "Sync") },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {

            DropdownMenuItem(
                onClick = { onToggleSyncCrosshair(null) },
                text = { Text("Crosshair") },
                trailingIcon = {

                    Switch(
                        checked = syncPrefs.crosshair,
                        onCheckedChange = onToggleSyncCrosshair,
                    )
                },
            )

            DropdownMenuItem(
                onClick = { onToggleSyncTime(null) },
                text = { Text("Time") },
                trailingIcon = {

                    Switch(
                        checked = syncPrefs.time,
                        onCheckedChange = onToggleSyncTime,
                    )
                },
            )

            DropdownMenuItem(
                onClick = { onToggleSyncDateRange(null) },
                text = { Text("Date Range") },
                trailingIcon = {

                    Switch(
                        checked = syncPrefs.dateRange,
                        onCheckedChange = onToggleSyncDateRange,
                    )
                },
            )
        }
    }
}

@Composable
private fun ChartLayout(
    layout: ChartsLayout,
    onSetLayout: (ChartsLayout) -> Unit,
) {

    var expanded by state { false }
    var show2PanesSelectionDialog by state { false }
    var show3PanesSelectionDialog by state { false }
    var show4PanesSelectionDialog by state { false }

    Box {

        TextButton(
            onClick = { expanded = true },
            icon = {

                Icon(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    imageVector = Icons.Default.GridView,
                    contentDescription = "Charts Layout",
                )
            },
            text = {

                val text = when (layout) {
                    Tabs -> "Tabs"
                    is TwoPanes -> "2 Panes"
                    is ThreePanes -> "3 Panes"
                    is FourPanes -> "4 Panes"
                }

                Text(text)
            },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {

            DropdownMenuItem(
                text = { Text("Tabs") },
                onClick = {
                    onSetLayout(Tabs)
                    expanded = false
                },
            )

            DropdownMenuItem(
                text = { Text("2 panes") },
                onClick = {
                    show2PanesSelectionDialog = true
                    expanded = false
                },
            )

            DropdownMenuItem(
                text = { Text("3 panes") },
                onClick = {
                    show3PanesSelectionDialog = true
                    expanded = false
                },
            )

            DropdownMenuItem(
                text = { Text("4 panes") },
                onClick = {
                    show4PanesSelectionDialog = true
                    expanded = false
                },
            )
        }
    }

    if (show2PanesSelectionDialog) {

        Panes2SelectionDialog(
            onDismissRequest = { show2PanesSelectionDialog = false },
            onSetLayout = { layout ->
                onSetLayout(layout)
                show2PanesSelectionDialog = false
            },
        )
    }

    if (show3PanesSelectionDialog) {

        Panes3SelectionDialog(
            onDismissRequest = { show3PanesSelectionDialog = false },
            onSetLayout = { layout ->
                onSetLayout(layout)
                show3PanesSelectionDialog = false
            },
        )
    }

    if (show4PanesSelectionDialog) {

        Panes4SelectionDialog(
            onDismissRequest = { show4PanesSelectionDialog = false },
            onSetLayout = { layout ->
                onSetLayout(layout)
                show4PanesSelectionDialog = false
            },
        )
    }
}
