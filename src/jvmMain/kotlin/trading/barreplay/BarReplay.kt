package trading.barreplay

import trading.CandleSeries

class BarReplay {

    private var offset = 0

    private val sessionList = mutableListOf<Session>()

    fun addReplaySession(candleSeries: CandleSeries, initialReplayIndex: Int): Session {

        val replayCandleSeries = CandleSeries(
            initial = candleSeries.subList(0, initialReplayIndex + offset),
            timeframe = candleSeries.timeframe,
        )

        val session = Session(
            candleSeries,
            replayCandleSeries,
            initialReplayIndex,
        )

        sessionList.add(session)

        return session
    }

    fun removeSession(session: Session) {
        sessionList.remove(session)
    }

    fun next() {

        sessionList.forEach {
            val currentIndex = it.initialIndex + offset
            it.replayCandleSeries.addCandle(it.inputCandleSeries[currentIndex])
        }

        offset++
    }

    fun reset() {
        sessionList.forEach { it.replayCandleSeries.removeLast(offset) }
        offset = 0
    }

    class Session(
        val inputCandleSeries: CandleSeries,
        val replayCandleSeries: CandleSeries,
        val initialIndex: Int,
    )
}
