package com.saurabhsandav.core.ui.tags

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.tagform.model.TagFormType
import com.saurabhsandav.core.ui.tags.model.TagsEvent
import com.saurabhsandav.core.ui.tags.model.TagsEvent.*
import com.saurabhsandav.core.ui.tags.model.TagsState
import com.saurabhsandav.core.ui.tags.model.TagsState.Tag
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class TagsPresenter(
    private val coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles,
) {

    private val trades = coroutineScope.async { tradingProfiles.getRecord(profileId).trades }

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TagsState(
            tags = getTags().value,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: TagsEvent) {

        when (event) {
            NewTag -> onNewTag()
            is NewTagFromExisting -> onNewTagFromExisting(event.id)
            is EditTag -> onEditTag(event.id)
            is DeleteTag -> onDeleteTag(event.id)
        }
    }

    @Composable
    private fun getTags(): State<List<Tag>?> {
        return remember {
            flow {

                trades
                    .await()
                    .getAllTags()
                    .map { tags ->
                        tags.map { tag ->

                            Tag(
                                id = tag.id,
                                name = tag.name,
                                description = tag.description.ifBlank { null },
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
            formType = TagFormType.New,
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

        trades.await().deleteTag(id)
    }
}
