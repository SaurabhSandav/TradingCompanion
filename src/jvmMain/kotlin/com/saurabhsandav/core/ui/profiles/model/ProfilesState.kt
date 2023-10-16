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
    initial: Initial,
) {

    val nameField = validator.addField(initial.name) { isRequired() }

    val descriptionField = validator.addField(initial.description)

    val isTrainingField = validator.addField(initial.isTraining)

    class Initial(
        val name: String = "",
        val description: String = "",
        val isTraining: Boolean = false,
    )
}
