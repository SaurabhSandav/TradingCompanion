package trading.barreplay

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class BarReplay {

    private var offset = 0

    private val sessionList = mutableListOf<BarReplaySession>()

    fun newSession(session: (currentOffset: Int) -> BarReplaySession): BarReplaySession {
        contract { callsInPlace(session, InvocationKind.EXACTLY_ONCE) }
        return session(offset).also(sessionList::add)
    }

    fun removeSession(session: BarReplaySession) {
        sessionList.remove(session)
    }

    fun next() {
        sessionList.forEach { it.addCandle(offset) }
        offset++
    }

    fun reset() {
        sessionList.forEach { it.reset(offset) }
        offset = 0
    }
}
