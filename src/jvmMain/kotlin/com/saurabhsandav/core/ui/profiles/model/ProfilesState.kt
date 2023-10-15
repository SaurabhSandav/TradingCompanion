package com.saurabhsandav.core.ui.profiles.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.saurabhsandav.core.ui.common.form2.FormValidator
import com.saurabhsandav.core.ui.common.form2.validations.isRequired
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class ProfilesState(
    val profiles: ImmutableList<Profile>,
    val currentProfile: Profile?,
    val eventSink: (ProfilesEvent) -> Unit,
) {

    @Immutable
    data class Profile(
        val id: Long,
        val name: String,
        val description: String,
        val isTraining: Boolean,
    )
}

@Stable
internal class ProfileFormModel(
    val validator: FormValidator,
    name: String,
    description: String,
    isTraining: Boolean,
) {

    val nameField = validator.addField(name) { isRequired() }

    val descriptionField = validator.addField(description)

    val isTrainingField = validator.addField(isTraining)
}
