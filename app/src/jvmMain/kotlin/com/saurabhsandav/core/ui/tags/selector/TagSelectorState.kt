package com.saurabhsandav.core.ui.tags.selector

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.tags.model.TradeTag
import com.saurabhsandav.core.ui.tags.selector.TagSelectorType.All
import com.saurabhsandav.core.ui.tags.selector.TagSelectorType.ForTrades
import com.saurabhsandav.core.utils.mapList
import kotlinx.coroutines.flow.flatMapLatest

internal class TagSelectorState(
    profileId: ProfileId,
    private val type: TagSelectorType,
    private val tradingProfiles: TradingProfiles,
) {

    var filterQuery: String by mutableStateOf("")

    val tags = snapshotFlow { filterQuery }
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
}

sealed class TagSelectorType {

    data class All(
        val ignoreIds: List<TradeTagId>,
    ) : TagSelectorType()

    data class ForTrades(
        val ids: List<TradeId>,
    ) : TagSelectorType()
}

internal class TagSelectorStateFactory(
    private val tradingProfiles: TradingProfiles,
) {

    fun build(
        profileId: ProfileId,
        type: TagSelectorType,
    ) = TagSelectorState(profileId, type, tradingProfiles)
}
