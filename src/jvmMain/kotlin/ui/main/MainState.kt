package ui.main

import androidx.compose.runtime.Immutable
import ui.sizing.SizedTrade

@Immutable
internal data class MainState(
    val sizedTrades: List<SizedTrade> = emptyList(),
)
