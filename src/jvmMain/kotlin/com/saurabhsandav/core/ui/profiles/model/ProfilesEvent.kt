package com.saurabhsandav.core.ui.profiles.model

internal sealed class ProfilesEvent {

    data class CreateNewProfile(val profileModel: ProfileModel) : ProfilesEvent()

    data class SetCurrentProfile(val id: Long) : ProfilesEvent()

    data class DeleteProfile(val id: Long) : ProfilesEvent()

    data class UpdateProfile(
        val id: Long,
        val profileModel: ProfileModel,
    ) : ProfilesEvent()

    data class CopyProfile(val id: Long) : ProfilesEvent()
}
