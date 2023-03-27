package com.saurabhsandav.core.ui.profiles.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.fields.switch
import com.saurabhsandav.core.ui.common.form.fields.textField
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class ProfilesState(
    val profiles: ImmutableList<Profile>,
) {

    @Immutable
    data class Profile(
        val id: Long,
        val name: String,
        val description: String,
        val isTraining: Boolean,
        val isCurrent: Boolean,
    )
}

@Stable
internal class ProfileModel(
    validator: FormValidator,
    name: String,
    description: String,
    isTraining: Boolean,
) {

    val name = validator.textField(initial = name)

    val description = validator.textField(
        initial = description,
        isRequired = false,
    )

    val isTraining = validator.switch(isTraining)
}
