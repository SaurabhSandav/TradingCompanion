package com.saurabhsandav.core.ui.tags.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.tags.form.model.TagFormType
import com.saurabhsandav.core.ui.tags.model.TradeTag
import com.saurabhsandav.core.ui.tags.screen.model.TagsScreenEvent
import com.saurabhsandav.core.ui.tags.screen.model.TagsScreenEvent.DeleteTag
import com.saurabhsandav.core.ui.tags.screen.model.TagsScreenEvent.EditTag
import com.saurabhsandav.core.ui.tags.screen.model.TagsScreenEvent.NewTag
import com.saurabhsandav.core.ui.tags.screen.model.TagsScreenEvent.NewTagFromExisting
import com.saurabhsandav.core.ui.tags.screen.model.TagsScreenState
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class TagsScreenPresenter(
    private val coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles,
) {

    private val tags = coroutineScope.async { tradingProfiles.getRecord(profileId).tags }

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TagsScreenState(
            tags = getTags().value,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: TagsScreenEvent) {

        when (event) {
            NewTag -> onNewTag()
            is NewTagFromExisting -> onNewTagFromExisting(event.id)
            is EditTag -> onEditTag(event.id)
            is DeleteTag -> onDeleteTag(event.id)
        }
    }

    @Composable
    private fun getTags(): State<List<TradeTag>?> {
        return remember {
            flow {

                tags
                    .await()
                    .getAll()
                    .map { tags ->
                        tags.map { tag ->

                            TradeTag(
                                id = tag.id,
                                name = tag.name,
                                description = tag.description.ifBlank { null },
                                color = tag.color?.let(::Color),
                            )
                        }
                    }
                    .emitInto(this)
            }
        }.collectAsState(null)
    }

    private fun onNewTag() {

        tradeContentLauncher.openTagForm(
            profileId = profileId,
            formType = TagFormType.New(),
        )
    }

    private fun onNewTagFromExisting(id: TradeTagId) {

        tradeContentLauncher.openTagForm(
            profileId = profileId,
            formType = TagFormType.NewFromExisting(id),
        )
    }

    private fun onEditTag(id: TradeTagId) {

        tradeContentLauncher.openTagForm(
            profileId = profileId,
            formType = TagFormType.Edit(id),
        )
    }

    private fun onDeleteTag(id: TradeTagId) = coroutineScope.launchUnit {

        tags.await().delete(id)
    }
}
