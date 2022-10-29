package studies.barreplay

import AppModule
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chart.createChart
import chart.options.ChartOptions
import chart.options.CrosshairMode
import chart.options.CrosshairOptions
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import studies.Study
import trading.data.CandleRepository
import ui.common.ResizableChart
import ui.common.controls.DateTimeField
import ui.common.controls.ListSelectionField
import ui.common.form.rememberFormScope
import ui.common.state
import utils.NIFTY50

internal class BarReplayStudy(
    private val appModule: AppModule,
    private val candleRepo: CandleRepository = CandleRepository(appModule),
) : Study {

    @Composable
    override fun render() {

        val formScope = rememberFormScope()
        var screen by state { Screen.LaunchForm }

        val fields = remember {
            BarReplayFormFields(
                formScope = formScope,
                initial = BarReplayFormFields.Model(
                    symbol = null,
                    timeframe = null,
                    dataFrom = (Clock.System.now().minus(30, DateTimeUnit.DAY, TimeZone.currentSystemDefault()))
                        .toLocalDateTime(TimeZone.currentSystemDefault()),
                    dataTo = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                    replayFrom = (Clock.System.now().minus(15, DateTimeUnit.DAY, TimeZone.currentSystemDefault()))
                        .toLocalDateTime(TimeZone.currentSystemDefault()),
                ),
            )
        }

        when (screen) {
            Screen.LaunchForm -> LaunchForm(fields) {
                if (formScope.isFormValid()) screen = Screen.Chart
            }

            Screen.Chart -> Chart(fields) { screen = Screen.LaunchForm }
        }
    }

    @Composable
    private fun LaunchForm(
        fields: BarReplayFormFields,
        onLaunchReplay: () -> Unit,
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            ListSelectionField(
                items = NIFTY50,
                onSelection = fields.symbol.onSelectionChange,
                selection = fields.symbol.value,
                isError = fields.symbol.isError,
                label = { Text("Ticker") },
                placeholderText = "Select Ticker...",
            )

            ListSelectionField(
                items = listOf("5m", "1D"),
                onSelection = fields.timeframe.onSelectionChange,
                selection = fields.timeframe.value,
                isError = fields.timeframe.isError,
                label = { Text("Timeframe") },
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

            Button(onLaunchReplay) {
                Text("Launch")
            }
        }
    }

    @Composable
    private fun Chart(
        fields: BarReplayFormFields,
        onNewReplay: () -> Unit,
    ) {

        Row(Modifier.fillMaxSize()) {

            val coroutineScope = rememberCoroutineScope()
            val chart = remember {
                createChart(ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)))
            }
            val replayControls = remember { BarReplayControls() }

            ReplayControls(fields, replayControls, onNewReplay)

            ResizableChart(
                chart = chart,
                modifier = Modifier.fillMaxSize(),
            ) {

                val replayChart = BarReplayChart(this)
                val barReplay = BarReplay(
                    candleRepo = candleRepo,
                    coroutineScope = coroutineScope,
                    replayChart = replayChart,
                    symbol = fields.symbol.value ?: error("Invalid symbol!"),
                    timeframe = fields.timeframe.value ?: error("Invalid timeframe!"),
                    dataFrom = fields.dataFrom.value.toInstant(TimeZone.currentSystemDefault()),
                    dataTo = fields.dataTo.value.toInstant(TimeZone.currentSystemDefault()),
                    replayFrom = fields.replayFrom.value.toInstant(TimeZone.currentSystemDefault()),
                )
                replayControls.setBarReplay(barReplay)

                coroutineScope.launch {
                    barReplay.init()
                }
            }
        }
    }

    @Composable
    private fun ReplayControls(
        fields: BarReplayFormFields,
        replayControls: BarReplayControls,
        onNewReplay: () -> Unit,
    ) {

        Column(
            modifier = Modifier.width(250.dp).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {

            Button(
                onClick = onNewReplay,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("New Replay")
            }

            Button(
                onClick = replayControls::reset,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reset Replay")
            }

            Button(
                onClick = replayControls::next,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Next")
            }

            ListSelectionField(
                items = listOf("5m", "1D"),
                onSelection = { },
                selection = fields.timeframe.value,
                label = { Text("Timeframe") },
            )

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
                        replayControls.setIsAutoNextEnabled(it)
                        isAutoNextEnabled = it
                    },
                )
            }
        }
    }

    private enum class Screen {
        LaunchForm,
        Chart,
    }

    class Factory(private val appModule: AppModule) : Study.Factory<BarReplayStudy> {

        override val name: String = "Bar Replay"

        override fun create() = BarReplayStudy(appModule)
    }
}
