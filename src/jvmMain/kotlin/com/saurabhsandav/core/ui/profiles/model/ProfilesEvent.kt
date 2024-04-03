package com.saurabhsandav.core.ui.profiles.model

import com.saurabhsandav.core.trades.model.ProfileId

internal sealed class ProfilesEvent {

    data class SetCurrentProfile(val id: ProfileId?) : ProfilesEvent()

    data class DeleteProfile(val id: ProfileId) : ProfilesEvent()

    data class CopyProfile(val id: ProfileId) : ProfilesEvent()
}
