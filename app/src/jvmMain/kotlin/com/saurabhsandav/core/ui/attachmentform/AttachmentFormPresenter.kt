package com.saurabhsandav.core.ui.attachmentform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.AttachmentFileId
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.ui.attachmentform.model.AttachmentFormModel
import com.saurabhsandav.core.ui.attachmentform.model.AttachmentFormState
import com.saurabhsandav.core.ui.attachmentform.model.AttachmentFormType
import com.saurabhsandav.core.ui.attachmentform.model.AttachmentFormType.Edit
import com.saurabhsandav.core.ui.attachmentform.model.AttachmentFormType.New
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.io.path.absolutePathString

internal class AttachmentFormPresenter(
    private val onCloseRequest: () -> Unit,
    private val coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
    private val formType: AttachmentFormType,
    private val tradingProfiles: TradingProfiles,
) {

    private val tradingRecord = coroutineScope.async { tradingProfiles.getRecord(profileId) }
    private var formModel by mutableStateOf<AttachmentFormModel?>(null)

    init {

        when (formType) {
            is New -> new(formType.file)
            is Edit -> edit(formType.tradeId, formType.fileId)
        }

        // Close if profile deleted
        tradingProfiles
            .getProfileOrNull(profileId)
            .filter { it == null }
            .onEach { onCloseRequest() }
            .launchIn(coroutineScope)
    }

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule AttachmentFormState(
            formModel = formModel,
        )
    }

    private suspend fun AttachmentFormModel.onSaveAttachment() {

        when (formType) {
            is New -> tradingRecord.await().attachments.add(
                tradeIds = formType.tradeIds,
                name = nameField.value,
                description = descriptionField.value,
                pathStr = path!!,
            )

            is Edit -> tradingRecord.await().attachments.update(
                tradeId = formType.tradeId,
                fileId = formType.fileId,
                name = nameField.value,
                description = descriptionField.value,
            )
        }

        // Close form
        onCloseRequest()
    }

    private fun new(file: String?) {

        formModel = AttachmentFormModel(
            coroutineScope = coroutineScope,
            initial = AttachmentFormModel.Initial(path = file),
            onSubmit = { onSaveAttachment() },
        )
    }

    private fun edit(
        tradeId: TradeId,
        fileId: AttachmentFileId,
    ) = coroutineScope.launchUnit {

        val attachmentsRepo = tradingRecord.await().attachments
        val attachment = attachmentsRepo.getByIdWithFile(tradeId, fileId).first()

        formModel = AttachmentFormModel(
            coroutineScope = coroutineScope,
            initial = AttachmentFormModel.Initial(
                name = attachment.name,
                description = attachment.description,
                path = attachment.path.absolutePathString(),
            ),
            onSubmit = { onSaveAttachment() },
        )
    }
}
