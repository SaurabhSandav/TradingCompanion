package com.saurabhsandav.core.ui.review.model

import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId

internal sealed class ReviewEvent {

    data class SetTitle(
        val title: String,
    ) : ReviewEvent()

    data object ToggleMarkdown : ReviewEvent()

    data class SaveReview(
        val review: String,
    ) : ReviewEvent()

    data class OpenMarkdownLink(
        val linkText: String,
    ) : ReviewEvent()

    data class OpenChart(
        val profileTradeId: ProfileTradeId,
    ) : ReviewEvent()

    data class OpenDetails(
        val profileTradeId: ProfileTradeId,
    ) : ReviewEvent()
}
