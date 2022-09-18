package main

import androidx.compose.runtime.Stable
import sizing.SizedTrade

@Stable
internal data class MainState(
    val sizedTrades: List<SizedTrade>,
)
