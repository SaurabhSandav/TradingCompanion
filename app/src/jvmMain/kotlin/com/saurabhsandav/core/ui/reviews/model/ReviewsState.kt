package com.saurabhsandav.core.ui.reviews.model

import androidx.paging.PagingData
import com.saurabhsandav.core.trades.model.ReviewId
import kotlinx.coroutines.flow.Flow

internal data class ReviewsState(
    val reviewEntries: Flow<PagingData<ReviewEntry>>,
    val eventSink: (ReviewsEvent) -> Unit,
) {

    internal sealed class ReviewEntry {

        data class Section(
            val isPinned: Boolean,
            val count: Flow<Long>,
        ) : ReviewEntry()

        data class Item(
            val id: ReviewId,
            val title: String,
            val isPinned: Boolean,
        ) : ReviewEntry()
    }
}
