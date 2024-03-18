package com.saurabhsandav.core.ui.stats.model

import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.stats.studies.Study

internal data class StatsState(
    val statsCategories: List<StatsCategory>?,
    val studyFactories: List<Study.Factory<*>>,
    val studyWindowsManager: AppWindowsManager<Study.Factory<*>>,
    val eventSink: (StatsEvent) -> Unit,
) {

    data class StatsCategory(
        val label: String,
        val entries: List<StatEntry>,
    )

    data class StatEntry(
        val label: String,
        val value: String,
    )
}
