package ui.pnlcalculator

import AppModule
import LocalAppModule
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.Side
import ui.common.form.FormValidator
import ui.pnlcalculator.PNLCalculatorWindowParams.OperationType.*
import utils.Brokerage
import utils.brokerage
import java.math.BigDecimal

@Composable
internal fun rememberPNLCalculatorWindowState(
    params: PNLCalculatorWindowParams,
): PNLCalculatorWindowState {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current

    return remember {
        PNLCalculatorWindowState(
            params = params,
            coroutineScope = scope,
            appModule = appModule,
        )
    }
}

internal class PNLCalculatorWindowState(
    val params: PNLCalculatorWindowParams,
    coroutineScope: CoroutineScope,
    private val appModule: AppModule,
) {

    private val formValidator = FormValidator()
    private var maxId = 0

    var isReady by mutableStateOf(false)
        private set

    val model = PNLCalculatorModel(
        validator = formValidator,
        quantity = "1",
        isLong = true,
        entry = "100",
        exit = "110",
    )

    init {

        coroutineScope.launch {

            when (params.operationType) {
                New -> Unit
                is FromOpenTrade -> openFromOpenTrade(params.operationType.id)
                is FromClosedTrade -> openFromClosedTrade(params.operationType.id)
            }

            isReady = true
        }
    }

    fun onCalculate() {

        val side = if (model.isLong.value) "LONG" else "SHORT"

        if (!formValidator.isValid() || model.pnlEntries.any {
                it.quantity == model.quantity.value &&
                        it.side == side &&
                        it.entry == model.entry.value &&
                        it.exit == model.exit.value
            }) return

        val brokerage = brokerage(
            quantity = model.quantity.value.toBigDecimal(),
            entry = model.entry.value.toBigDecimal(),
            exit = model.exit.value.toBigDecimal(),
            side = if (model.isLong.value) Side.Long else Side.Short,
        )

        model.pnlEntries += PNLEntry(
            id = maxId++,
            side = side,
            quantity = model.quantity.value,
            entry = model.entry.value,
            exit = model.exit.value,
            breakeven = brokerage.breakeven.toPlainString(),
            pnl = brokerage.pnl.toPlainString(),
            isProfitable = brokerage.pnl > BigDecimal.ZERO,
            netPNL = brokerage.netPNL.toPlainString(),
            charges = brokerage.totalCharges.toPlainString(),
            isNetProfitable = brokerage.netPNL > BigDecimal.ZERO,
            isRemovable = true,
        )
    }

    fun onRemoveCalculation(id: Int) {
        model.pnlEntries.removeIf { it.id == id }
    }

    private suspend fun openFromOpenTrade(id: Long) {

        val openTrade = withContext(Dispatchers.IO) {
            appModule.appDB.openTradeQueries.getById(id).executeAsOne()
        }

        model.quantity.value = openTrade.quantity
        model.isLong.value = Side.fromString(openTrade.side) == Side.Long
        model.entry.value = openTrade.entry
        model.exit.value = openTrade.target ?: openTrade.stop ?: openTrade.entry

        model.enableModification = false

        openTrade.stop?.let {

            val brokerage = brokerage(
                quantity = openTrade.quantity.toBigDecimal(),
                entry = openTrade.entry.toBigDecimal(),
                exit = it.toBigDecimal(),
                side = if (model.isLong.value) Side.Long else Side.Short,
            )

            model.pnlEntries += PNLEntry(
                id = maxId++,
                side = openTrade.side.uppercase(),
                quantity = openTrade.quantity,
                entry = openTrade.entry,
                exit = "$it\n(Stop)",
                breakeven = brokerage.breakeven.toPlainString(),
                pnl = brokerage.pnl.toPlainString(),
                isProfitable = brokerage.pnl > BigDecimal.ZERO,
                charges = brokerage.totalCharges.toPlainString(),
                netPNL = brokerage.netPNL.toPlainString(),
                isNetProfitable = brokerage.netPNL > BigDecimal.ZERO,
            )
        }

        openTrade.target?.let {

            val brokerage = brokerage(
                quantity = openTrade.quantity.toBigDecimal(),
                entry = openTrade.entry.toBigDecimal(),
                exit = it.toBigDecimal(),
                side = if (model.isLong.value) Side.Long else Side.Short,
            )

            model.pnlEntries += PNLEntry(
                id = maxId++,
                side = openTrade.side.uppercase(),
                quantity = openTrade.quantity,
                entry = openTrade.entry,
                exit = "$it (Target)",
                breakeven = brokerage.breakeven.toPlainString(),
                pnl = brokerage.pnl.toPlainString(),
                isProfitable = brokerage.pnl > BigDecimal.ZERO,
                charges = brokerage.totalCharges.toPlainString(),
                netPNL = brokerage.netPNL.toPlainString(),
                isNetProfitable = brokerage.netPNL > BigDecimal.ZERO,
            )
        }
    }

    private suspend fun openFromClosedTrade(id: Long) {

        val closedTrade = withContext(Dispatchers.IO) {
            appModule.appDB.closedTradeQueries.getById(id).executeAsOne()
        }

        model.quantity.value = closedTrade.quantity
        model.isLong.value = Side.fromString(closedTrade.side) == Side.Long
        model.entry.value = closedTrade.entry
        model.exit.value = closedTrade.exit

        model.enableModification = false

        closedTrade.stop?.let {

            val brokerage = brokerage(
                quantity = closedTrade.quantity.toBigDecimal(),
                entry = closedTrade.entry.toBigDecimal(),
                exit = it.toBigDecimal(),
                side = if (model.isLong.value) Side.Long else Side.Short,
            )

            model.pnlEntries += PNLEntry(
                id = maxId++,
                side = closedTrade.side.uppercase(),
                quantity = closedTrade.quantity,
                entry = closedTrade.entry,
                exit = "$it\n(Stop)",
                breakeven = brokerage.breakeven.toPlainString(),
                pnl = brokerage.pnl.toPlainString(),
                isProfitable = brokerage.pnl > BigDecimal.ZERO,
                charges = brokerage.totalCharges.toPlainString(),
                netPNL = brokerage.netPNL.toPlainString(),
                isNetProfitable = brokerage.netPNL > BigDecimal.ZERO,
            )
        }

        closedTrade.target?.let {

            val brokerage = brokerage(
                quantity = closedTrade.quantity.toBigDecimal(),
                entry = closedTrade.entry.toBigDecimal(),
                exit = it.toBigDecimal(),
                side = if (model.isLong.value) Side.Long else Side.Short,
            )

            model.pnlEntries += PNLEntry(
                id = maxId++,
                side = closedTrade.side.uppercase(),
                quantity = closedTrade.quantity,
                entry = closedTrade.entry,
                exit = "$it\n(Target)",
                breakeven = brokerage.breakeven.toPlainString(),
                pnl = brokerage.pnl.toPlainString(),
                isProfitable = brokerage.pnl > BigDecimal.ZERO,
                charges = brokerage.totalCharges.toPlainString(),
                netPNL = brokerage.netPNL.toPlainString(),
                isNetProfitable = brokerage.netPNL > BigDecimal.ZERO,
            )
        }

        closedTrade.exit.let {

            val brokerage = brokerage(
                quantity = closedTrade.quantity.toBigDecimal(),
                entry = closedTrade.entry.toBigDecimal(),
                exit = it.toBigDecimal(),
                side = if (model.isLong.value) Side.Long else Side.Short,
            )

            model.pnlEntries += PNLEntry(
                id = maxId++,
                side = closedTrade.side.uppercase(),
                quantity = closedTrade.quantity,
                entry = closedTrade.entry,
                exit = "$it\n(Exit)",
                breakeven = brokerage.breakeven.toPlainString(),
                pnl = brokerage.pnl.toPlainString(),
                isProfitable = brokerage.pnl > BigDecimal.ZERO,
                charges = brokerage.totalCharges.toPlainString(),
                netPNL = brokerage.netPNL.toPlainString(),
                isNetProfitable = brokerage.netPNL > BigDecimal.ZERO,
            )
        }
    }

    private fun brokerage(
        quantity: BigDecimal,
        entry: BigDecimal,
        exit: BigDecimal,
        side: Side,
    ): Brokerage = brokerage(
        broker = "Finvasia",
        instrument = "equity",
        entry = entry,
        exit = exit,
        quantity = quantity,
        side = side,
    )
}

internal class PNLCalculatorWindowParams(
    val operationType: OperationType,
    val onCloseRequest: () -> Unit,
) {

    sealed class OperationType {

        object New : OperationType()

        data class FromOpenTrade(val id: Long) : OperationType()

        data class FromClosedTrade(val id: Long) : OperationType()
    }
}
