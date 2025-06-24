package com.saurabhsandav.core.ui.tags.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trading.record.TradingProfiles
import com.saurabhsandav.core.trading.record.model.ProfileId
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
            onSubmit = ::onSubmit,
        )
    }

    init {

        coroutineScope.launch {

            formModel = when (formType) {
                is New -> TagFormModel(
                    isTagNameUnique = ::isTagNameUnique,
                    name = formType.name ?: "",
                )

                is NewFromExisting -> {

                    val tag = tags.await().getById(formType.id).first()

                    TagFormModel(
                        isTagNameUnique = ::isTagNameUnique,
                        name = tag.name,
                        description = tag.description,
                        color = tag.color?.let(::Color),
                    )
                }

                is Edit -> {

                    val tag = tags.await().getById(formType.id).first()

                    TagFormModel(
                        isTagNameUnique = ::isTagNameUnique,
                        name = tag.name,
                        description = tag.description,
                        color = tag.color?.let(::Color),
                    )
                }
            }
        }
    }

    private fun onSubmit() = coroutineScope.launchUnit {

        val formModel = formModel!!

        when (formType) {
            is New, is NewFromExisting -> tags.await().create(
                name = formModel.nameField.value,
                description = formModel.descriptionField.value,
                color = formModel.colorField.value?.toArgb(),
            )

            is Edit -> tags.await().update(
                id = formType.id,
                name = formModel.nameField.value,
                description = formModel.descriptionField.value,
                color = formModel.colorField.value?.toArgb(),
            )
        }
        onCloseRequest()
    }

    fun onDelete() = coroutineScope.launchUnit {
        tags.await().delete((formType as Edit).id)
    }

    private suspend fun isTagNameUnique(name: String): Boolean {
        return tags.await().isNameUnique(name, (formType as? Edit)?.id)
    }
}
