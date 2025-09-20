package com.saurabhsandav.core.ui.tags.selector

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.trading.TradingProfiles
import com.saurabhsandav.core.ui.tags.model.TradeTag
import com.saurabhsandav.core.ui.tags.selector.TagSelectorType.All
import com.saurabhsandav.core.ui.tags.selector.TagSelectorType.ForTrades
import com.saurabhsandav.core.utils.mapList
import com.saurabhsandav.trading.record.model.TradeId
import com.saurabhsandav.trading.record.model.TradeTagId
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.flatMapLatest

@AssistedInject
internal class TagSelectorState(
    @Assisted profileId: ProfileId,
    @Assisted private val type: TagSelectorType,
    private val tradingProfiles: TradingProfiles,
) {

    val filterQuery = TextFieldState()

    val tags = snapshotFlow { filterQuery.text.toString() }
        .flatMapLatest { filterQuery ->
            tradingProfiles.getRecord(profileId).tags.run {
                when (type) {
                    is All -> getSuggested(filterQuery, type.ignoreIds)
                    is ForTrades -> getSuggestedForTrades(type.ids, filterQuery)
                }
            }
        }
        .mapList { tag ->

            TradeTag(
                id = tag.id,
                name = tag.name,
                description = tag.description.ifBlank { null },
                color = tag.color?.let(::Color),
            )
        }

    @AssistedFactory
    fun interface Factory {

        fun create(
            profileId: ProfileId,
            type: TagSelectorType,
        ): TagSelectorState
    }
}

sealed class TagSelectorType {

    data class All(
        val ignoreIds: List<TradeTagId>,
    ) : TagSelectorType()

    data class ForTrades(
        val ids: List<TradeId>,
    ) : TagSelectorType()
}
