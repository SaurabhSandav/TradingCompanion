package com.saurabhsandav.core.ui.tags

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.ui.tags.model.TagsEvent
import com.saurabhsandav.core.ui.tags.model.TagsEvent.CopyTag
import com.saurabhsandav.core.ui.tags.model.TagsEvent.DeleteTag
import com.saurabhsandav.core.ui.tags.model.TagsState
import com.saurabhsandav.core.ui.tags.model.TagsState.Tag
import com.saurabhsandav.core.ui.tradecontent.ProfileTagId
import com.saurabhsandav.core.utils.getCurrentTradingProfile
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Stable
internal class TagsPresenter(
    private val coroutineScope: CoroutineScope,
    private val appPrefs: FlowSettings,
    private val tradingProfiles: TradingProfiles,
) {

    private val currentProfileId = appPrefs.getCurrentTradingProfile(tradingProfiles)
        .map { it.id }
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TagsState(
            profileId = currentProfileId.collectAsState().value,
            tags = getTags().value,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: TagsEvent) {

        when (event) {
            is CopyTag -> onCopyTag(event.profileTagId)
            is DeleteTag -> onDeleteTag(event.profileTagId)
        }
    }

    @Composable
    private fun getTags(): State<ImmutableList<Tag>> {
        return remember {
            appPrefs.getCurrentTradingProfile(tradingProfiles).flatMapLatest { profile ->

                val tradingRecord = tradingProfiles.getRecord(profile.id)

                tradingRecord.trades.getAllTags().map { tags ->
                    tags.map { tag ->

                        Tag(
                            id = ProfileTagId(
                                profileId = profile.id,
                                tagId = tag.id,
                            ),
                            name = tag.name,
                            description = tag.description,
                        )
                    }.toImmutableList()
                }
            }
        }.collectAsState(persistentListOf())
    }

    private fun onCopyTag(profileTagId: ProfileTagId) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.getRecord(profileTagId.profileId)

        tradingRecord.trades.copyTag(profileTagId.tagId)
    }

    private fun onDeleteTag(profileTagId: ProfileTagId) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.getRecord(profileTagId.profileId)

        tradingRecord.trades.deleteTag(profileTagId.tagId)
    }
}
