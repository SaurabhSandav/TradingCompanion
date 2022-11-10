package trading.barreplay

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class BarReplay {

    private var offset = 0

    private val sessionList = mutableListOf<ReplaySession>()

    fun newSession(session: (currentOffset: Int) -> ReplaySession): ReplaySession {
        contract { callsInPlace(session, InvocationKind.EXACTLY_ONCE) }
        return session(offset).also(sessionList::add)
    }

    fun removeSession(session: ReplaySession) {
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
