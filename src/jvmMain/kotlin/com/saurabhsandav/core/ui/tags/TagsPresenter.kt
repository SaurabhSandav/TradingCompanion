package com.saurabhsandav.core.ui.tags

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.tags.model.TagsEvent
import com.saurabhsandav.core.ui.tags.model.TagsEvent.DeleteTag
import com.saurabhsandav.core.ui.tags.model.TagsState
import com.saurabhsandav.core.ui.tags.model.TagsState.Tag
import com.saurabhsandav.core.ui.tradecontent.ProfileTagId
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@Stable
internal class TagsPresenter(
    private val coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
    private val tradingProfiles: TradingProfiles,
) {

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TagsState(
            tags = getTags().value,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: TagsEvent) {

        when (event) {
            is DeleteTag -> onDeleteTag(event.profileTagId)
        }
    }

    @Composable
    private fun getTags(): State<ImmutableList<Tag>> {
        return remember {
            flow {

                tradingProfiles
                    .getRecord(profileId)
                    .trades
                    .getAllTags()
                    .map { tags ->
                        tags.map { tag ->

                            Tag(
                                id = ProfileTagId(
                                    profileId = profileId,
                                    tagId = tag.id,
                                ),
                                name = tag.name,
                                description = tag.description,
                            )
                        }.toImmutableList()
                    }
                    .emitInto(this)
            }
        }.collectAsState(persistentListOf())
    }

    private fun onDeleteTag(profileTagId: ProfileTagId) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.getRecord(profileTagId.profileId)

        tradingRecord.trades.deleteTag(profileTagId.tagId)
    }
}
