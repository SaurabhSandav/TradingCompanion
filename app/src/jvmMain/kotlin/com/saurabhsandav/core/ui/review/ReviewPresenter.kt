package com.saurabhsandav.core.ui.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.Trade
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ReviewId
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.ui.common.TradeDateTimeFormat
import com.saurabhsandav.core.ui.review.model.ReviewEvent
import com.saurabhsandav.core.ui.review.model.ReviewEvent.OpenChart
import com.saurabhsandav.core.ui.review.model.ReviewEvent.OpenDetails
import com.saurabhsandav.core.ui.review.model.ReviewEvent.OpenMarkdownLink
import com.saurabhsandav.core.ui.review.model.ReviewEvent.SaveReview
import com.saurabhsandav.core.ui.review.model.ReviewEvent.SetTitle
import com.saurabhsandav.core.ui.review.model.ReviewEvent.ToggleMarkdown
import com.saurabhsandav.core.ui.review.model.ReviewState
import com.saurabhsandav.core.ui.review.model.ReviewState.TradeEntry
import com.saurabhsandav.core.ui.tradecontent.ProfileReviewId
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class ReviewPresenter(
    private val coroutineScope: CoroutineScope,
    private val profileReviewId: ProfileReviewId,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles,
) {

    private val tradingRecord = coroutineScope.async { tradingProfiles.getRecord(profileReviewId.profileId) }
    private val trades = coroutineScope.async { tradingRecord.await().trades }
    private val reviews = coroutineScope.async { tradingRecord.await().reviews }
    private val review = flow { reviews.await().getById(profileReviewId.reviewId).emitInto(this) }

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        val review = review.collectAsState(null).value ?: return@launchMolecule null

        return@launchMolecule ReviewState(
            title = review.title,
            isMarkdown = review.isMarkdown,
            review = review.review,
            trades = getTrades(),
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: ReviewEvent) {

        when (event) {
            is SetTitle -> onSaveTitle(event.title)
            ToggleMarkdown -> onToggleMarkdown()
            is SaveReview -> onSaveReview(event.review)
            is OpenMarkdownLink -> onOpenMarkdownLink(event.linkText)
            is OpenChart -> tradeContentLauncher.openTradeReview(event.profileTradeId)
            is OpenDetails -> tradeContentLauncher.openTrade(event.profileTradeId)
        }
    }

    @Composable
    private fun getTrades(): List<TradeEntry> {
        return produceState<List<TradeEntry>>(emptyList()) {

            reviews
                .await()
                .getById(profileReviewId.reviewId)
                .flatMapLatest { review ->

                    trades
                        .await()
                        .getByIds(ids = review.tradeIds)
                        .mapList { trade ->
                            trade.toTradeListEntry()
                        }
                }
                .collect { value = it }
        }.value
    }

    private fun Trade.toTradeListEntry(): TradeEntry {

        val instrumentCapitalized = instrument.strValue
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        fun formatDuration(duration: Duration): String {

            val durationSeconds = duration.inWholeSeconds

            return "%02d:%02d:%02d".format(
                durationSeconds / 3600,
                (durationSeconds % 3600) / 60,
                durationSeconds % 60,
            )
        }

        val duration = when {
            isClosed -> TradeEntry.Duration.Closed(
                str = formatDuration(exitTimestamp!! - entryTimestamp),
            )

            else -> TradeEntry.Duration.Open(
                flow = flow {
                    while (true) {
                        emit(formatDuration(Clock.System.now() - entryTimestamp))
                        delay(1.seconds)
                    }
                },
            )
        }

        return TradeEntry(
            profileTradeId = ProfileTradeId(profileId = profileReviewId.profileId, tradeId = id),
            broker = "$broker ($instrumentCapitalized)",
            ticker = ticker,
            side = side.toString().uppercase(),
            quantity = when {
                !isClosed -> "$closedQuantity / $quantity"
                else -> quantity.toPlainString()
            },
            entry = averageEntry.toPlainString(),
            exit = averageExit?.toPlainString() ?: "",
            entryTime = entryTimestamp.toLocalDateTime(TimeZone.currentSystemDefault()).format(TradeDateTimeFormat),
            duration = duration,
            pnl = pnl.toPlainString(),
            isProfitable = pnl > BigDecimal.ZERO,
            netPnl = netPnl.toPlainString(),
            isNetProfitable = netPnl > BigDecimal.ZERO,
        )
    }

    private fun onSaveTitle(title: String) = coroutineScope.launchUnit {

        reviews.await().setTitle(profileReviewId.reviewId, title)
    }

    private fun onToggleMarkdown() = coroutineScope.launchUnit {

        reviews.await().toggleIsMarkdown(profileReviewId.reviewId)
    }

    private fun onSaveReview(review: String) = coroutineScope.launchUnit {

        val ids = Regex(ReviewTradeRefRegex)
            .findAll(review)
            .mapNotNull { result -> result.groupValues[1].toLongOrNull() }
            .toList()
            .distinct()
        val tradeIds = trades.await().exists(ids).first().filterValues { it }.keys.map(::TradeId)

        reviews.await().update(
            id = profileReviewId.reviewId,
            review = review,
            tradeIds = tradeIds,
        )
    }

    private fun onOpenMarkdownLink(linkText: String) = coroutineScope.launchUnit {

        val tradeMatch = Regex(ReviewTradeRefRegex).matchEntire(linkText)
            ?: Regex(TradeRefRegex).matchEntire(linkText)

        if (tradeMatch != null) {

            val tradeId = tradeMatch.groupValues[1].toLongOrNull() ?: return@launchUnit

            val tradeExists = trades.await().exists(tradeId).first()

            if (tradeExists) {

                val profileTradeId = ProfileTradeId(
                    profileId = profileReviewId.profileId,
                    tradeId = TradeId(tradeId),
                )

                tradeContentLauncher.openTrade(profileTradeId)
            }

            return@launchUnit
        }

        val reviewMatch = Regex(ReviewRefRegex).matchEntire(linkText)

        if (reviewMatch != null) {

            val reviewId = reviewMatch.groupValues[1].toLongOrNull() ?: return@launchUnit

            val reviewExists = reviews.await().exists(reviewId).first()

            if (reviewExists) {

                val profileReviewId = ProfileReviewId(
                    profileId = profileReviewId.profileId,
                    reviewId = ReviewId(reviewId),
                )

                tradeContentLauncher.openReview(profileReviewId)
            }
        }
    }

    private companion object {

        const val ReviewTradeRefRegex = "ReviewTrade#(\\d+)"
        const val TradeRefRegex = "Trade#(\\d+)"
        const val ReviewRefRegex = "Review#(\\d+)"
    }
}
