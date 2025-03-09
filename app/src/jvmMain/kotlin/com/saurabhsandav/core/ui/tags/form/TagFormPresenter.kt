package com.saurabhsandav.core.ui.tags.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.tags.form.model.TagFormModel
import com.saurabhsandav.core.ui.tags.form.model.TagFormState
import com.saurabhsandav.core.ui.tags.form.model.TagFormType
import com.saurabhsandav.core.ui.tags.form.model.TagFormType.Edit
import com.saurabhsandav.core.ui.tags.form.model.TagFormType.New
import com.saurabhsandav.core.ui.tags.form.model.TagFormType.NewFromExisting
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class TagFormPresenter(
    private val coroutineScope: CoroutineScope,
    profileId: ProfileId,
    private val formType: TagFormType,
    private val onCloseRequest: () -> Unit,
    private val tradingProfiles: TradingProfiles,
) {

    private val tags = coroutineScope.async { tradingProfiles.getRecord(profileId).tags }

    private var formModel by mutableStateOf<TagFormModel?>(null)

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TagFormState(
            title = remember {
                when (formType) {
                    is New, is NewFromExisting -> "New Tag"
                    is Edit -> "Edit Tag"
                }
            },
            formModel = formModel,
        )
    }

    init {

        coroutineScope.launch {

            formModel = TagFormModel(
                coroutineScope = coroutineScope,
                isTagNameUnique = ::isTagNameUnique,
                initial = when (formType) {
                    is New -> TagFormModel.Initial(name = formType.name ?: "")
                    is NewFromExisting -> {

                        val tag = tags.await().getById(formType.id).first()

                        TagFormModel.Initial(
                            name = tag.name,
                            description = tag.description,
                            color = tag.color?.let(::Color),
                        )
                    }

                    is Edit -> {

                        val tag = tags.await().getById(formType.id).first()

                        TagFormModel.Initial(
                            name = tag.name,
                            description = tag.description,
                            color = tag.color?.let(::Color),
                        )
                    }
                },
                onSubmit = { save() },
            )
        }
    }

    fun onDelete() = coroutineScope.launchUnit {
        tags.await().delete((formType as Edit).id)
    }

    private suspend fun TagFormModel.save() {

        when (formType) {
            is New, is NewFromExisting -> tags.await().create(
                name = nameField.value,
                description = descriptionField.value,
                color = colorField.value?.toArgb(),
            )

            is Edit -> tags.await().update(
                id = formType.id,
                name = nameField.value,
                description = descriptionField.value,
                color = colorField.value?.toArgb(),
            )
        }

        onCloseRequest()
    }

    private suspend fun isTagNameUnique(name: String): Boolean {
        return tags.await().isNameUnique(name, (formType as? Edit)?.id)
    }
}
