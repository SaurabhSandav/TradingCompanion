package com.saurabhsandav.core.ui.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.Trade
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ReviewId
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.ui.common.TradeDateTimeFormatter
import com.saurabhsandav.core.ui.review.model.ReviewEvent
import com.saurabhsandav.core.ui.review.model.ReviewEvent.*
import com.saurabhsandav.core.ui.review.model.ReviewState
import com.saurabhsandav.core.ui.review.model.ReviewState.TradeEntry
import com.saurabhsandav.core.ui.tradecontent.ProfileReviewId
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.format
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Stable
internal class ReviewPresenter(
    private val coroutineScope: CoroutineScope,
    private val profileReviewId: ProfileReviewId,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles,
) {

    private val review = flow {
        tradingProfiles.getRecord(profileReviewId.profileId)
            .trades
            .getReviewById(profileReviewId.reviewId)
            .emitInto(this)
    }

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
    private fun getTrades(): ImmutableList<TradeEntry> {
        return produceState<ImmutableList<TradeEntry>>(persistentListOf()) {

            val tradingRecord = tradingProfiles.getRecord(profileReviewId.profileId)

            tradingRecord
                .trades
                .getReviewById(profileReviewId.reviewId)
                .flatMapLatest { review ->

                    tradingRecord.trades
                        .getByIds(ids = review.tradeIds)
                        .mapList { trade ->
                            trade.toTradeListEntry()
                        }
                        .map { it.toImmutableList() }
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

        val durationStr = when {
            isClosed -> flowOf(formatDuration(exitTimestamp!! - entryTimestamp))
            else -> flow {
                while (true) {
                    emit(formatDuration(Clock.System.now() - entryTimestamp))
                    delay(1.seconds)
                }
            }
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
            entryTime = TradeDateTimeFormatter.format(
                ldt = entryTimestamp.toLocalDateTime(TimeZone.currentSystemDefault())
            ),
            duration = durationStr,
            pnl = pnl.toPlainString(),
            isProfitable = pnl > BigDecimal.ZERO,
            netPnl = netPnl.toPlainString(),
            isNetProfitable = netPnl > BigDecimal.ZERO,
        )
    }

    private fun onSaveTitle(title: String) = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileReviewId.profileId)
            .trades
            .setReviewTitle(profileReviewId.reviewId, title)
    }

    private fun onToggleMarkdown() = coroutineScope.launchUnit {

        tradingProfiles.getRecord(profileReviewId.profileId)
            .trades
            .toggleReviewIsMarkdown(profileReviewId.reviewId)
    }

    private fun onSaveReview(review: String) = coroutineScope.launchUnit {

        val record = tradingProfiles.getRecord(profileReviewId.profileId)

        val ids = Regex(ReviewTradeRefRegex)
            .findAll(review)
            .mapNotNull { result -> result.groupValues[1].toLongOrNull() }
            .toList()
            .distinct()
        val tradeIds = record.trades.exists(ids).first().filterValues { it }.keys.map(::TradeId)

        record.trades.updateReview(
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

            val tradingRecord = tradingProfiles.getRecord(profileReviewId.profileId)
            val tradeExists = tradingRecord.trades.exists(tradeId).first()

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

            val tradingRecord = tradingProfiles.getRecord(profileReviewId.profileId)
            val reviewExists = tradingRecord.trades.reviewExists(reviewId).first()

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
