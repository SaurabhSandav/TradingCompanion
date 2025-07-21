package com.saurabhsandav.core.trading

import com.saurabhsandav.core.StartupJob
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes

internal class TradeManagementJob(
    private val excursionsGenerator: TradeExcursionsGenerator,
) : StartupJob {

    override suspend fun run() {

        // Don't start right away after app launch
        delay(1.minutes)

        // Generate excursions
        excursionsGenerator.generateExcursions()
    }
}
