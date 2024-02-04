package com.saurabhsandav.core.ui.autotrader

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.facebook.ktfmt.format.Formatter
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.AutoTraderScriptId
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.autotrader.model.AutoTraderEvent
import com.saurabhsandav.core.ui.autotrader.model.AutoTraderEvent.*
import com.saurabhsandav.core.ui.autotrader.model.AutoTraderState
import com.saurabhsandav.core.ui.autotrader.model.AutoTraderState.Script
import com.saurabhsandav.core.ui.autotrader.model.ConfigFormModel
import com.saurabhsandav.core.ui.autotrader.model.ScriptFormModel
import com.saurabhsandav.core.utils.NIFTY50
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic

@Stable
internal class AutoTraderPresenter(
    private val coroutineScope: CoroutineScope,
    private val appDB: AppDB,
    private val appPrefs: FlowSettings,
    private val candleRepo: CandleRepository,
    private val tradingProfiles: TradingProfiles,
) {

    private val host = AutoTraderScriptHost()
    private val configFormModel = ConfigFormModel(coroutineScope, ConfigFormModel.Initial())
    private var scriptFormModel by mutableStateOf<ScriptFormModel?>(null)
    private var isScriptRunning by mutableStateOf(false)

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule AutoTraderState(
            configFormModel = configFormModel,
            scripts = getScripts(),
            scriptFormModel = scriptFormModel,
            isScriptRunning = isScriptRunning,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: AutoTraderEvent) {

        when (event) {
            Run -> onRun()
            NewScript -> onNewScript()
            is SelectScript -> onSelectScript(event.id)
            is CopyScript -> onCopyScript(event.id)
            is DeleteScript -> onDeleteScript(event.id)
            FormatScript -> onFormatScript()
            SaveScript -> onSaveScript()
        }
    }

    init {

        coroutineScope.launch {

            val key = "autotrader_selected_script"

            val selectedScriptId = appPrefs.getLongOrNullFlow(key).first()

            if (selectedScriptId != null) {
                onSelectScript(selectedScriptId.let(::AutoTraderScriptId))
            }

            snapshotFlow { scriptFormModel?.id }
                .flowOn(Dispatchers.IO)
                .collect { id ->
                    when (id) {
                        null -> appPrefs.remove(key)
                        else -> appPrefs.putLong(key, id.value)
                    }
                }
        }

        coroutineScope.launch {

            snapshotFlow {
                with(configFormModel) { tickerField.value to intervalField.value }
            }.collect { (ticker, interval) ->
                configFormModel.titleField.value = "${ticker}_${interval.start}_${interval.endInclusive}"
            }
        }
    }

    @Composable
    private fun getScripts(): ImmutableList<Script> {
        return produceState(persistentListOf()) {

            appDB.autoTraderScriptQueries
                .getAll()
                .asFlow()
                .mapToList(Dispatchers.IO)
                .mapList { script ->

                    Script(
                        id = script.id,
                        title = script.title,
                        description = script.description,
                    )
                }
                .collect { value = it.toImmutableList() }
        }.value
    }

    private fun onRun() = coroutineScope.launchUnit(Dispatchers.IO) {

        if (!configFormModel.validator.validate()) return@launchUnit

        val scriptFormModel = scriptFormModel ?: return@launchUnit
        val scriptTitle = scriptFormModel.titleField.value
        val script = scriptFormModel.scriptField.value

        isScriptRunning = true

        val tz = TimeZone.currentSystemDefault()
        val ticker = configFormModel.tickerField.value!!
        val from = configFormModel.intervalField.value.start.atStartOfDayIn(tz)
        // Include the entire to day
        val to = configFormModel.intervalField.value.endInclusive.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz)

        val scriptStrategyGenerator = object : ScriptStrategyGenerator {

            val strategyBlocks = mutableListOf<suspend (TradingStrategy.Environment, Candle) -> Unit>()

            override fun strategy(block: suspend (TradingStrategy.Environment, Candle) -> Unit) {
                strategyBlocks += block
            }
        }

        val compilationResult = host.execStr(scriptStrategyGenerator, script)

        scriptFormModel.consoleText = compilationResult
            .reports
            .filter { it.severity > ScriptDiagnostic.Severity.WARNING }
            .joinToString("\n") { it.toString() }

        if (compilationResult is ResultWithDiagnostics.Success) {

            val strategyBuilder = object : TradingStrategy.Builder {

                override val title: String = scriptTitle

                override fun build(env: TradingStrategy.Environment): TradingStrategy {
                    return TradingStrategy { candle ->
                        scriptStrategyGenerator.strategyBlocks.forEach { strategyBlock -> strategyBlock(env, candle) }
                    }
                }
            }

            ReplayAutoTrader(candleRepo, tradingProfiles).trade(
                tickers = if (ticker == "All") NIFTY50 else listOf(ticker),
                from = from,
                to = to,
                strategyBuilder = strategyBuilder,
            )
        }

        isScriptRunning = false
    }

    private fun onNewScript() = coroutineScope.launchUnit(Dispatchers.IO) {

        var title = "New Script"
        var count = 0

        while (!isScriptTitleUnique(title)) {
            title = "New Script (${++count})"
        }

        val id = appDB.transactionWithResult {

            appDB.autoTraderScriptQueries.insert(
                title = title,
                description = "",
                script = "",
                created = Clock.System.now(),
                lastUpdated = Clock.System.now(),
            )

            appDB.appDBUtilsQueries.lastInsertedRowId().executeAsOne().let(::AutoTraderScriptId)
        }

        val formModel = ScriptFormModel(
            coroutineScope = coroutineScope,
            isTitleUnique = { isScriptTitleUnique(it, id) },
            initial = ScriptFormModel.Initial(
                id = id,
                title = title,
            ),
        )

        scriptFormModel = formModel
    }

    private fun onSelectScript(id: AutoTraderScriptId) = coroutineScope.launchUnit(Dispatchers.IO) {

        val queries = appDB.autoTraderScriptQueries
        val script = queries.getById(id).executeAsOne()

        val formModel = ScriptFormModel(
            coroutineScope = coroutineScope,
            isTitleUnique = { title -> isScriptTitleUnique(title, id) },
            initial = ScriptFormModel.Initial(
                id = id,
                title = script.title,
                description = script.description,
                script = script.script,
            ),
        )

        scriptFormModel = formModel
    }

    private fun onCopyScript(id: AutoTraderScriptId) = coroutineScope.launchUnit(Dispatchers.IO) {

        val queries = appDB.autoTraderScriptQueries
        val script = queries.getById(id).executeAsOne()

        queries.insert(
            title = "Copy of ${script.title}",
            description = script.description,
            script = script.script,
            created = Clock.System.now(),
            lastUpdated = Clock.System.now(),
        )
    }

    private fun onDeleteScript(id: AutoTraderScriptId) = coroutineScope.launchUnit(Dispatchers.IO) {

        appDB.autoTraderScriptQueries.delete(id)

        scriptFormModel = null
    }

    private fun onFormatScript() = coroutineScope.launchUnit(Dispatchers.IO) {

        val formModel = scriptFormModel ?: return@launchUnit

        val script = formModel.scriptField.value
        val formattedScript = Formatter.format(
            options = Formatter.KOTLINLANG_FORMAT,
            code = script,
        )

        formModel.scriptField.value = formattedScript
    }

    private fun onSaveScript() = coroutineScope.launchUnit(Dispatchers.IO) {

        val formModel = scriptFormModel ?: return@launchUnit

        if (!formModel.validator.validate()) return@launchUnit

        appDB.autoTraderScriptQueries.update(
            id = formModel.id,
            title = formModel.titleField.value,
            description = formModel.descriptionField.value,
            script = formModel.scriptField.value,
            lastUpdated = Clock.System.now(),
        )
    }

    private suspend fun isScriptTitleUnique(
        title: String,
        ignoreId: AutoTraderScriptId? = null,
    ): Boolean = withContext(Dispatchers.IO) {
        appDB.autoTraderScriptQueries.isTitleUnique(title, ignoreId).executeAsOne()
    }
}
