package ui.main

import androidx.compose.runtime.Immutable
import ui.sizing.model.SizedTrade

@Immutable
internal data class MainState(
    val sizedTrades: List<SizedTrade> = emptyList(),
)
