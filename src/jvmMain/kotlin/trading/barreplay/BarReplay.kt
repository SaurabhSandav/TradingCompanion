package trading.barreplay

import trading.Timeframe
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class BarReplay(
    private val timeframe: Timeframe,
    private val candleUpdateType: CandleUpdateType = CandleUpdateType.FullBar,
) {

    private var offset = 0
    private var candleState = CandleState.Close

    private val sessionList = mutableListOf<BarReplaySession>()

    fun newSession(
        session: (currentOffset: Int, currentCandleState: CandleState) -> BarReplaySession,
    ): BarReplaySession {

        contract { callsInPlace(session, InvocationKind.EXACTLY_ONCE) }

        return session(offset, candleState).also {
            check(it.inputSeries.timeframe == timeframe) { "BarReplay: Session timeframe is invalid" }
            sessionList.add(it)
        }
    }

    fun removeSession(session: BarReplaySession) {
        sessionList.remove(session)
    }

    fun next() {

        when (candleUpdateType) {
            CandleUpdateType.FullBar -> {
                sessionList.forEach { it.addCandle(offset) }
                offset++
            }

            CandleUpdateType.OHLC -> {

                // Move to next candle state
                candleState = candleState.next()

                // Add/Update bar
                sessionList.forEach { it.addCandle(offset, candleState) }

                // If candle has closed, increment offset
                if (candleState == CandleState.Close) offset++
            }
        }
    }

    fun reset() {
        sessionList.forEach { it.reset() }
        offset = 0
        candleState = CandleState.Close
    }

    enum class CandleState {
        Open,
        Extreme1,
        Extreme2,
        Close;

        fun next(): CandleState {
            return vals[(ordinal + 1) % vals.size]
        }

        private companion object {
            private val vals = values()
        }
    }
}
