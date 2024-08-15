package com.saurabhsandav.core.ui.tagform

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
import com.saurabhsandav.core.ui.tagform.model.TagFormModel
import com.saurabhsandav.core.ui.tagform.model.TagFormState
import com.saurabhsandav.core.ui.tagform.model.TagFormType
import com.saurabhsandav.core.ui.tagform.model.TagFormType.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class TagFormPresenter(
    coroutineScope: CoroutineScope,
    profileId: ProfileId,
    private val formType: TagFormType,
    private val onCloseRequest: () -> Unit,
    private val tradingProfiles: TradingProfiles,
) {

    private val trades = coroutineScope.async { tradingProfiles.getRecord(profileId).trades }

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

            val trades = trades.await()

            formModel = TagFormModel(
                coroutineScope = coroutineScope,
                isTagNameUnique = ::isTagNameUnique,
                initial = when (formType) {
                    is New -> TagFormModel.Initial()
                    is NewFromExisting -> {

                        val tag = trades.getTagById(formType.id).first()

                        TagFormModel.Initial(
                            name = tag.name,
                            description = tag.description,
                            color = tag.color?.let(::Color),
                        )
                    }

                    is Edit -> {

                        val tag = trades.getTagById(formType.id).first()

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

    private suspend fun TagFormModel.save() {

        val trades = trades.await()

        when (formType) {
            is New, is NewFromExisting -> trades.createTag(
                name = nameField.value,
                description = descriptionField.value,
                color = colorField.value?.toArgb(),
            )

            is Edit -> trades.updateTag(
                id = formType.id,
                name = nameField.value,
                description = descriptionField.value,
                color = colorField.value?.toArgb(),
            )
        }

        onCloseRequest()
    }

    private suspend fun isTagNameUnique(name: String): Boolean {
        return trades.await().isTagNameUnique(name, (formType as? Edit)?.id)
    }
}
