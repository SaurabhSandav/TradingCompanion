package com.saurabhsandav.core.ui.closetradeform

import androidx.compose.runtime.*
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.TradeTag
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.closetradeform.CloseTradeFormWindowParams.OperationType.CloseOpenTrade
import com.saurabhsandav.core.ui.closetradeform.CloseTradeFormWindowParams.OperationType.EditExistingTrade
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.utils.launchUnit
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun rememberCloseTradeFormWindowState(
    params: CloseTradeFormWindowParams,
): CloseTradeFormWindowState {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current

    return remember {
        CloseTradeFormWindowState(
            params = params,
            coroutineScope = scope,
            appModule = appModule,
        )
    }
}

@Stable
internal class CloseTradeFormWindowState(
    val params: CloseTradeFormWindowParams,
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val appDB: AppDB = appModule.appDB,
    private val candleRepo: CandleRepository = CandleRepository(appModule),
) {

    private val formValidator = FormValidator()

    var isReady by mutableStateOf(false)
        private set

    var showDetails by mutableStateOf(false)
        private set

    val model = CloseTradeFormModel(
        validator = formValidator,
        ticker = null,
        quantity = "",
        isLong = true,
        entry = "",
        stop = "",
        entryDateTime = run {
            val currentTime = Clock.System.now()
            val currentTimeWithoutNanoseconds = currentTime - currentTime.nanosecondsOfSecond.nanoseconds
            currentTimeWithoutNanoseconds.toLocalDateTime(TimeZone.currentSystemDefault())
        },
        target = "",
        exit = "",
        exitDateTime = run {
            val currentTime = Clock.System.now()
            val currentTimeWithoutNanoseconds = currentTime - currentTime.nanosecondsOfSecond.nanoseconds
            currentTimeWithoutNanoseconds.toLocalDateTime(TimeZone.currentSystemDefault())
        },
    )

    var detailModel: CloseTradeDetailFormModel? = null

    init {

        coroutineScope.launch {

            when (params.operationType) {
                is EditExistingTrade -> editExistingTrade(params.operationType.id)
                is CloseOpenTrade -> closeOpenTrade(params.operationType.openTradeId)
            }

            isReady = true
        }
    }

    fun onSaveTrade() = coroutineScope.launchUnit {

        if (!formValidator.isValid()) return@launchUnit

        withContext(Dispatchers.IO) {

            appDB.transaction {

                val id = when (params.operationType) {
                    is CloseOpenTrade -> null
                    is EditExistingTrade -> params.operationType.id
                }

                appDB.closedTradeQueries.insert(
                    id = id,
                    broker = "Finvasia",
                    ticker = model.ticker.value!!,
                    instrument = "equity",
                    quantity = model.quantity.value,
                    lots = null,
                    side = (if (model.isLong.value) TradeSide.Long else TradeSide.Short).strValue,
                    entry = model.entry.value,
                    stop = model.stop.value.ifBlank { null },
                    entryDate = model.entryDateTime.value.toString(),
                    target = model.target.value.ifBlank { null },
                    exit = model.exit.value,
                    exitDate = model.exitDateTime.value.toString(),
                )

                if (id != null) {

                    val detailModel = requireNotNull(detailModel)

                    appDB.closedTradeDetailQueries.insert(
                        closedTradeId = id,
                        maxFavorableExcursion = detailModel.maxFavorableExcursion.value.ifBlank { null },
                        maxAdverseExcursion = detailModel.maxAdverseExcursion.value.ifBlank { null },
                        persisted = detailModel.persisted.toString(),
                        persistenceResult = null,
                        notes = detailModel.notes.value.ifBlank { null },
                    )
                }

                if (params.operationType is CloseOpenTrade) {
                    appDB.openTradeQueries.delete(params.operationType.openTradeId)
                }
            }
        }

        params.onCloseRequest()
    }

    fun onShowDetails() {
        showDetails = true
    }

    fun onCalculateMFE() = coroutineScope.launchUnit {

        val dependencyFields = listOf(
            model.ticker,
            model.entry,
            model.entryDateTime,
            model.exitDateTime,
        )

        if (!dependencyFields.onEach { it.validate() }.all { it.isValid }) return@launchUnit

        val candles = getCandlesInSession()

        var mfe = model.entry.value.toBigDecimal()

        for (candle in candles) {
            when {
                model.isLong.value -> {
                    mfe = maxOf(mfe, candle.high)
                    if (candle.low <= model.stop.value.toBigDecimal()) break
                }

                else -> {
                    mfe = minOf(mfe, candle.low)
                    if (candle.high >= model.stop.value.toBigDecimal()) break
                }
            }
        }

        detailModel!!.maxFavorableExcursion.value = mfe.toPlainString()
    }

    fun onCalculateMAE() = coroutineScope.launchUnit {

        val dependencyFields = listOf(
            model.ticker,
            model.entry,
            model.entryDateTime,
            model.exitDateTime,
        )

        if (!dependencyFields.onEach { it.validate() }.all { it.isValid }) return@launchUnit

        val candles = getCandlesInSession()

        var mae = model.entry.value.toBigDecimal()

        for (candle in candles) {
            when {
                model.isLong.value -> {
                    mae = minOf(mae, candle.low)
                    if (candle.high >= model.target.value.toBigDecimal()) break
                }

                else -> {
                    mae = maxOf(mae, candle.high)
                    if (candle.low <= model.target.value.toBigDecimal()) break
                }
            }
        }

        detailModel!!.maxAdverseExcursion.value = mae.toPlainString()
    }

    suspend fun getSuggestedTags(text: String): List<TradeTag> = withContext(Dispatchers.IO) {
        return@withContext when {
            text.isBlank() -> emptyList()
            else -> appDB.tradeTagQueries.getAllLike("%$text%").executeAsList()
        }
    }

    fun onAddTag(tag: String) = coroutineScope.launchUnit(Dispatchers.IO) {
        appDB.transaction {

            appDB.tradeTagQueries.insert(tag)

            val tagId = appDB.tradeTagQueries.getByName(tag).executeAsOne().id

            appDB.closedTradeTagQueries.insert(
                tradeId = (params.operationType as EditExistingTrade).id,
                tagId = tagId,
            )
        }
    }

    fun onSelectTag(id: Long) = coroutineScope.launchUnit(Dispatchers.IO) {
        appDB.closedTradeTagQueries.insert(
            tradeId = (params.operationType as EditExistingTrade).id,
            tagId = id,
        )
    }

    fun onRemoveTag(id: Long) = coroutineScope.launchUnit(Dispatchers.IO) {
        appDB.closedTradeTagQueries.delete(
            tradeId = (params.operationType as EditExistingTrade).id,
            tagId = id,
        )
    }

    private suspend fun editExistingTrade(id: Long) {

        val closedTrade = withContext(Dispatchers.IO) {
            appDB.closedTradeQueries.getClosedTradesDetailedById(id).executeAsOne()
        }

        detailModel = CloseTradeDetailFormModel(
            validator = formValidator,
            closeTradeFormModel = model,
            maxFavorableExcursion = closedTrade.maxFavorableExcursion.orEmpty(),
            maxAdverseExcursion = closedTrade.maxAdverseExcursion.orEmpty(),
            persisted = closedTrade.persisted.toBoolean(),
            notes = closedTrade.notes.orEmpty(),
        )

        // Tags
        appDB.closedTradeTagQueries.getTagsByTrade(id)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .onEach { detailModel!!.tags = it }
            .launchIn(coroutineScope)

        model.ticker.value = closedTrade.ticker
        model.quantity.value = closedTrade.quantity
        model.isLong.value = TradeSide.fromString(closedTrade.side) == TradeSide.Long
        model.entry.value = closedTrade.entry
        model.stop.value = closedTrade.stop.orEmpty()
        model.entryDateTime.value = LocalDateTime.parse(closedTrade.entryDate)
        model.target.value = closedTrade.target.orEmpty()
        model.exit.value = closedTrade.exit
        model.exitDateTime.value = LocalDateTime.parse(closedTrade.exitDate)
    }

    private suspend fun closeOpenTrade(openTradeId: Long) {

        val openTrade = withContext(Dispatchers.IO) {
            appModule.appDB.openTradeQueries.getById(openTradeId).executeAsOne()
        }

        val currentTime = Clock.System.now()
        val currentTimeWithoutNanoseconds = currentTime - currentTime.nanosecondsOfSecond.nanoseconds

        // TODO Fetch current price
        // val accessToken = appPrefs.getString(PrefKeys.FyersAccessToken)
        // val response = fyersApi.getQuotes(accessToken, listOf("NSE:${openTrade.ticker}-EQ"))
        // val currentPrice = response.result?.quote?.first()?.quoteData?.cmd?.close?.toString() ?: "0"

        model.ticker.value = openTrade.ticker
        model.quantity.value = openTrade.quantity
        model.isLong.value = TradeSide.fromString(openTrade.side) == TradeSide.Long
        model.entry.value = openTrade.entry
        model.stop.value = openTrade.stop.orEmpty()
        model.entryDateTime.value = LocalDateTime.parse(openTrade.entryDate)
        model.target.value = openTrade.target.orEmpty()
        model.exit.value = ""
        model.exitDateTime.value = currentTimeWithoutNanoseconds.toLocalDateTime(TimeZone.currentSystemDefault())
    }

    private suspend fun getCandlesInSession(): List<Candle> {

        val timeframe = Timeframe.M5

        val entryInstant = model.entryDateTime.value.toInstant(TimeZone.currentSystemDefault())

        val candlesResult = candleRepo.getCandles(
            ticker = model.ticker.value!!,
            timeframe = timeframe,
            from = entryInstant - timeframe.seconds.seconds,
            // Up to start of next day. Assume session only lasts the same day
            to = (model.exitDateTime.value.date + DatePeriod(days = 1)).atStartOfDayIn(TimeZone.currentSystemDefault()),
        )

        val candles = when (candlesResult) {
            is Ok -> candlesResult.value
            is Err -> when (val error = candlesResult.error) {
                is CandleRepository.Error.AuthError -> error("AuthError")
                is CandleRepository.Error.UnknownError -> error(error.message)
            }
        }

        val firstCandleOpenInstant = candles.first().openInstant

        // Make sure first candle is actually the entry candle
        return when (entryInstant) {
            in firstCandleOpenInstant..firstCandleOpenInstant + timeframe.seconds.seconds -> candles
            else -> candles.drop(1)
        }.dropLast(2) // Square-off 10 minutes before market close
    }
}

internal class CloseTradeFormWindowParams(
    val operationType: OperationType,
    val onCloseRequest: () -> Unit,
) {

    sealed class OperationType {

        data class CloseOpenTrade(val openTradeId: Long) : OperationType()

        data class EditExistingTrade(val id: Long) : OperationType()
    }
}
