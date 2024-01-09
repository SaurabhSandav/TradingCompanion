package com.saurabhsandav.core.ui.tags.model

import com.saurabhsandav.core.ui.tradecontent.ProfileTagId

sealed class TagsEvent {

    data class DeleteTag(val profileTagId: ProfileTagId) : TagsEvent()
}
