package main

import androidx.compose.runtime.Immutable
import sizing.SizedTrade

@Immutable
internal data class MainState(
    val sizedTrades: List<SizedTrade> = emptyList(),
)
