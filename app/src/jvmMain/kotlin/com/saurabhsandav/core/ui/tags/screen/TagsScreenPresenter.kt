package com.saurabhsandav.core.ui.tags.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trading.record.TradingProfiles
import com.saurabhsandav.core.trading.record.model.ProfileId
import com.saurabhsandav.core.trading.record.model.TradeTagId
import com.saurabhsandav.core.ui.tags.model.TradeTag
import com.saurabhsandav.core.ui.tags.screen.model.TagsScreenEvent
import com.saurabhsandav.core.ui.tags.screen.model.TagsScreenEvent.DeleteTag
import com.saurabhsandav.core.ui.tags.screen.model.TagsScreenState
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class TagsScreenPresenter(
    private val coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
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

    private fun onDeleteTag(id: TradeTagId) = coroutineScope.launchUnit {

        tags.await().delete(id)
    }
}
