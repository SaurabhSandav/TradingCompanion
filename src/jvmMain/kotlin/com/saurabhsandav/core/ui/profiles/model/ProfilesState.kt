package com.saurabhsandav.core.ui.profiles.model

import com.saurabhsandav.core.trades.model.ProfileId

internal data class ProfilesState(
    val profiles: List<Profile>,
    val currentProfile: Profile?,
    val eventSink: (ProfilesEvent) -> Unit,
) {

    data class Profile(
        val id: ProfileId,
        val name: String,
        val description: String?,
        val isTraining: Boolean,
    )
}
