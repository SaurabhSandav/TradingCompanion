package com.saurabhsandav.core.ui.profiles.model

internal sealed class ProfilesEvent {

    data class SetCurrentProfile(val id: Long) : ProfilesEvent()

    data class DeleteProfile(val id: Long) : ProfilesEvent()

    data class CopyProfile(val id: Long) : ProfilesEvent()
}
