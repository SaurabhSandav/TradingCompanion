package com.saurabhsandav.core.ui.autotrader.model

import com.saurabhsandav.core.trades.model.AutoTraderScriptId

internal sealed class AutoTraderEvent {

    data object Run : AutoTraderEvent()

    data object NewScript : AutoTraderEvent()

    data class SelectScript(val id: AutoTraderScriptId) : AutoTraderEvent()

    data class CopyScript(val id: AutoTraderScriptId) : AutoTraderEvent()

    data class DeleteScript(val id: AutoTraderScriptId) : AutoTraderEvent()

    data object FormatScript : AutoTraderEvent()

    data object SaveScript : AutoTraderEvent()
}
