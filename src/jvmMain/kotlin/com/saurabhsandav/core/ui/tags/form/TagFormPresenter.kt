package com.saurabhsandav.core.ui.tags.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.tags.form.TagFormType.*
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class TagFormPresenter(
    private val coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
    private val formType: TagFormType,
    private val onCloseRequest: () -> Unit,
    private val tradingProfiles: TradingProfiles,
) {

    private val formValidator = FormValidator(coroutineScope)

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
                validator = formValidator,
                isTagNameUnique = ::isTagNameUnique,
                initial = when (formType) {
                    is New -> TagFormModel.Initial()
                    is NewFromExisting -> {

                        val tradingRecord = tradingProfiles.getRecord(profileId)
                        val tag = tradingRecord.trades.getTagById(formType.id).first()

                        TagFormModel.Initial(
                            name = tag.name,
                            description = tag.description,
                        )
                    }

                    is Edit -> {

                        val tradingRecord = tradingProfiles.getRecord(profileId)
                        val tag = tradingRecord.trades.getTagById(formType.id).first()

                        TagFormModel.Initial(
                            name = tag.name,
                            description = tag.description,
                        )
                    }
                },
            )
        }
    }

    fun save() = coroutineScope.launchUnit {

        if (!formValidator.validate()) return@launchUnit

        val trades = tradingProfiles.getRecord(profileId).trades

        val formModel = checkNotNull(formModel)

        when (formType) {
            is New, is NewFromExisting -> trades.createTag(
                name = formModel.nameField.value,
                description = formModel.descriptionField.value,
            )

            is Edit -> trades.updateTag(
                id = formType.id,
                name = formModel.nameField.value,
                description = formModel.descriptionField.value,
            )
        }

        onCloseRequest()
    }

    private suspend fun isTagNameUnique(name: String): Boolean {
        val trades = tradingProfiles.getRecord(profileId).trades
        return trades.isTagNameUnique(name, (formType as? Edit)?.id)
    }
}
