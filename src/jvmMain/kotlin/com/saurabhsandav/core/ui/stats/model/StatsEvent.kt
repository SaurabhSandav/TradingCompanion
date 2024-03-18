package com.saurabhsandav.core.ui.stats.model

import com.saurabhsandav.core.ui.stats.studies.Study

internal sealed class StatsEvent {

    data class OpenStudy(val studyFactory: Study.Factory<*>) : StatsEvent()
}
