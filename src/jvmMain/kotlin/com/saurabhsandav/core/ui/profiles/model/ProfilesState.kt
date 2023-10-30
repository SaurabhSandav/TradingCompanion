package com.saurabhsandav.core.ui.profiles.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.trades.model.ProfileId
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class ProfilesState(
    val profiles: ImmutableList<Profile>,
    val currentProfile: Profile?,
    val eventSink: (ProfilesEvent) -> Unit,
) {

    @Immutable
    data class Profile(
        val id: ProfileId,
        val name: String,
        val description: String,
        val isTraining: Boolean,
    )
}
