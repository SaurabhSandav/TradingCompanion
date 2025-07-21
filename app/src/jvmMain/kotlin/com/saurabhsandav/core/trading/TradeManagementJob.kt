package com.saurabhsandav.core.trading

import com.saurabhsandav.core.StartupJob
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes

@ContributesIntoSet(AppScope::class, binding<StartupJob>())
@Inject
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
