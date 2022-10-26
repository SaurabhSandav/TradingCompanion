package studies

import AppModule
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import chart.*
import chart.data.CandlestickData
import chart.data.HistogramData
import chart.data.LineData
import chart.data.Time
import chart.options.*
import chart.options.common.LineWidth
import chart.options.common.PriceFormat
import fyers_api.model.CandleResolution
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import trading.CandleSeries
import trading.indicator.ClosePriceIndicator
import trading.indicator.EMAIndicator
import trading.indicator.VWAPIndicator
import ui.common.ResizableChart
import ui.common.controls.DateTimeField
import ui.common.controls.ListSelectionField
import ui.common.form.FormScope
import ui.common.form.dateTimeFieldState
import ui.common.form.rememberFormScope
import ui.common.form.singleSelectionState
import ui.common.state
import utils.CandleRepo
import utils.NIFTY50
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.seconds

internal class BarReplayStudy(
    private val appModule: AppModule,
    private val candleRepo: CandleRepo = CandleRepo(appModule),
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
            val barReplay = remember { BarReplay(candleRepo, coroutineScope) }
            val chart = remember {
                createChart(ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)))
            }

            ReplayControls(barReplay, onNewReplay)

            ResizableChart(
                chart = chart,
                modifier = Modifier.fillMaxSize(),
            ) {

                barReplay.setupChart(this)

                coroutineScope.launch {
                    snapshotFlow { fields.symbol.value }.collectLatest { symbol ->

                        barReplay.init(
                            symbol = fields.symbol.value ?: error("Invalid symbol!"),
                            timeframe = fields.timeframe.value ?: error("Invalid timeframe!"),
                            dataFrom = fields.dataFrom.value.toInstant(TimeZone.currentSystemDefault()),
                            dataTo = fields.dataTo.value.toInstant(TimeZone.currentSystemDefault()),
                            replayFrom = fields.replayFrom.value.toInstant(TimeZone.currentSystemDefault()),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ReplayControls(
        barReplay: BarReplay,
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
                onClick = barReplay::reset,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reset Replay")
            }

            Button(
                onClick = barReplay::next,
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

                Switch(
                    checked = barReplay.isAutoNextEnabled,
                    onCheckedChange = { barReplay.isAutoNextEnabled = it },
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

private class BarReplayFormFields(
    formScope: FormScope,
    initial: Model,
) {

    val symbol = formScope.singleSelectionState(initial.symbol)

    val timeframe = formScope.singleSelectionState(initial.timeframe)

    val dataFrom = formScope.dateTimeFieldState(initial.dataFrom)

    val dataTo = formScope.dateTimeFieldState(initial.dataTo)

    val replayFrom = formScope.dateTimeFieldState(initial.replayFrom)

    class Model(
        val symbol: String?,
        val timeframe: String?,
        val dataFrom: LocalDateTime,
        val dataTo: LocalDateTime,
        val replayFrom: LocalDateTime,
    )
}

private class BarReplay(
    private val candleRepo: CandleRepo,
    coroutineScope: CoroutineScope,
) {

    private lateinit var chart: IChartApi
    private lateinit var symbol: String
    private lateinit var timeframe: String
    private lateinit var dataFrom: Instant
    private lateinit var dataTo: Instant
    private lateinit var replayFrom: Instant

    private lateinit var candlestickSeries: ISeriesApi<CandlestickData>
    private lateinit var volumeSeries: ISeriesApi<HistogramData>
    private lateinit var ema9Series: ISeriesApi<LineData>
    private lateinit var vwapSeries: ISeriesApi<LineData>

    private val sessionStartTime = LocalTime(hour = 9, minute = 15)

    private lateinit var candleSeries: CandleSeries
    private lateinit var ema9Indicator: EMAIndicator
    private lateinit var vwapIndicator: VWAPIndicator

    private var initialCandleIndex by Delegates.notNull<Int>()
    private var currentCandleIndex by Delegates.notNull<Int>()

    var isAutoNextEnabled by mutableStateOf(false)

    init {

        coroutineScope.launch {
            snapshotFlow { isAutoNextEnabled }.collectLatest {
                while (it) {
                    delay(1.seconds)
                    next()
                }
            }
        }
    }

    fun setupChart(chart: IChartApi) {

        this.chart = chart

        candlestickSeries = chart.addCandlestickSeries(
            name = "candlestickSeries",
            options = CandlestickStyleOptions(
                lastValueVisible = false,
            ),
        )

        volumeSeries = chart.addHistogramSeries(
            name = "volumeSeries",
            options = HistogramStyleOptions(
                lastValueVisible = false,
                priceFormat = PriceFormat.BuiltIn(
                    type = PriceFormat.Type.Volume,
                ),
                priceScaleId = "",
                priceLineVisible = false,
            )
        )

        ema9Series = chart.addLineSeries(
            name = "ema9Series",
            options = LineStyleOptions(
                lineWidth = LineWidth.One,
                crosshairMarkerVisible = false,
                lastValueVisible = false,
                priceLineVisible = false,
            ),
        )

        vwapSeries = chart.addLineSeries(
            name = "vwapSeries",
            options = LineStyleOptions(
                color = Color.Yellow,
                lineWidth = LineWidth.One,
                crosshairMarkerVisible = false,
                lastValueVisible = false,
                priceLineVisible = false,
            ),
        )

        volumeSeries.priceScale.applyOptions(
            PriceScaleOptions(
                scaleMargins = PriceScaleMargins(
                    top = 0.8,
                    bottom = 0,
                )
            )
        )

        chart.timeScale.applyOptions(
            TimeScaleOptions(timeVisible = true)
        )
    }

    suspend fun init(
        symbol: String,
        timeframe: String,
        dataFrom: Instant,
        dataTo: Instant,
        replayFrom: Instant,
    ) {

        this.symbol = symbol
        this.timeframe = timeframe
        this.dataFrom = dataFrom
        this.dataTo = dataTo
        this.replayFrom = replayFrom

        candleSeries = candleRepo.getCandles(
            symbol = symbol,
            resolution = when (timeframe) {
                "1D" -> CandleResolution.D1
                else -> CandleResolution.M5
            },
            from = dataFrom,
            to = dataTo,
        ).let { (it as CandleRepo.CandleResult.Success).candles }

        initialCandleIndex = candleSeries.list.indexOfFirst { it.openInstant >= replayFrom }
        currentCandleIndex = initialCandleIndex
        ema9Indicator = EMAIndicator(ClosePriceIndicator(candleSeries), length = 9)
        vwapIndicator = VWAPIndicator(candleSeries) { candle ->
            candle.openInstant.toLocalDateTime(TimeZone.currentSystemDefault()).time == sessionStartTime
        }

        val candleData = mutableListOf<CandlestickData>()
        val volumeData = mutableListOf<HistogramData>()
        val ema9Data = mutableListOf<LineData>()
        val vwapData = mutableListOf<LineData>()

        candleSeries.list.slice(0 until currentCandleIndex).forEachIndexed { index, candle ->

            val epochTime = candle.openInstant.epochSeconds
            val timeZoneOffset = candle.openInstant.offsetIn(TimeZone.currentSystemDefault()).totalSeconds
            val offsetTime = epochTime + timeZoneOffset

            candleData += CandlestickData(
                time = Time.UTCTimestamp(offsetTime),
                open = candle.open,
                high = candle.high,
                low = candle.low,
                close = candle.close,
            )

            volumeData += HistogramData(
                time = Time.UTCTimestamp(offsetTime),
                value = candle.volume,
                color = when {
                    candle.close < candle.open -> Color(255, 82, 82)
                    else -> Color(0, 150, 136)
                },
            )

            ema9Data += LineData(
                time = Time.UTCTimestamp(offsetTime),
                value = ema9Indicator[index],
            )

            vwapData += LineData(
                time = Time.UTCTimestamp(offsetTime),
                value = vwapIndicator[index],
            )
        }

        candlestickSeries.setData(candleData)
        volumeSeries.setData(volumeData)
        ema9Series.setData(ema9Data)
        vwapSeries.setData(vwapData)

        chart.timeScale.scrollToPosition(40, false)
    }

    fun reset() {

        currentCandleIndex = initialCandleIndex
        isAutoNextEnabled = false

        val candleData = mutableListOf<CandlestickData>()
        val volumeData = mutableListOf<HistogramData>()
        val ema9Data = mutableListOf<LineData>()
        val vwapData = mutableListOf<LineData>()

        candleSeries.list.slice(0 until currentCandleIndex).forEachIndexed { index, candle ->

            val epochTime = candle.openInstant.epochSeconds
            val timeZoneOffset = candle.openInstant.offsetIn(TimeZone.currentSystemDefault()).totalSeconds
            val offsetTime = epochTime + timeZoneOffset

            candleData += CandlestickData(
                time = Time.UTCTimestamp(offsetTime),
                open = candle.open,
                high = candle.high,
                low = candle.low,
                close = candle.close,
            )

            volumeData += HistogramData(
                time = Time.UTCTimestamp(offsetTime),
                value = candle.volume,
                color = when {
                    candle.close < candle.open -> Color(255, 82, 82)
                    else -> Color(0, 150, 136)
                },
            )

            ema9Data += LineData(
                time = Time.UTCTimestamp(offsetTime),
                value = ema9Indicator[index],
            )

            vwapData += LineData(
                time = Time.UTCTimestamp(offsetTime),
                value = vwapIndicator[index],
            )
        }

        candlestickSeries.setData(candleData)
        volumeSeries.setData(volumeData)
        ema9Series.setData(ema9Data)
        vwapSeries.setData(vwapData)

        chart.timeScale.scrollToPosition(40, false)
    }

    fun next() {

        val candle = candleSeries.list[currentCandleIndex++]

        val epochTime = candle.openInstant.epochSeconds
        val timeZoneOffset = candle.openInstant.offsetIn(TimeZone.currentSystemDefault()).totalSeconds
        val offsetTime = epochTime + timeZoneOffset

        candlestickSeries.update(
            CandlestickData(
                time = Time.UTCTimestamp(offsetTime),
                open = candle.open,
                high = candle.high,
                low = candle.low,
                close = candle.close,
            )
        )

        volumeSeries.update(
            HistogramData(
                time = Time.UTCTimestamp(offsetTime),
                value = candle.volume,
                color = when {
                    candle.close < candle.open -> Color(255, 82, 82)
                    else -> Color(0, 150, 136)
                },
            )
        )

        ema9Series.update(
            LineData(
                time = Time.UTCTimestamp(offsetTime),
                value = ema9Indicator[currentCandleIndex],
            )
        )

        vwapSeries.update(
            LineData(
                time = Time.UTCTimestamp(offsetTime),
                value = vwapIndicator[currentCandleIndex],
            )
        )
    }
}
