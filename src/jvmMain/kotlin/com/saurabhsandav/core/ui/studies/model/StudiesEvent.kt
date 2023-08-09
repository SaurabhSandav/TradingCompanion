package com.saurabhsandav.core.ui.studies.model

import com.saurabhsandav.core.ui.studies.impl.Study

internal sealed class StudiesEvent {

    data class OpenStudy(val studyFactory: Study.Factory<*>) : StudiesEvent()
}
