package com.saurabhsandav.core.ui.reviews

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.PagingData
import com.saurabhsandav.core.trades.model.ReviewId
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.reviews.model.ReviewsState.ReviewEntry
import com.saurabhsandav.core.ui.reviews.ui.ReviewsList
import kotlinx.coroutines.flow.Flow

@Composable
internal fun ReviewsScreen(
    reviewEntries: Flow<PagingData<ReviewEntry>>,
    onNewReview: () -> Unit,
    onOpenReview: (ReviewId) -> Unit,
    onTogglePinReview: (ReviewId) -> Unit,
    onDeleteReview: (ReviewId) -> Unit,
) {

    // Set window title
    WindowTitle("Reviews")

    Column {

        Scaffold(
            modifier = Modifier.weight(1F),
        ) {

            ReviewsList(
                reviewEntries = reviewEntries,
                onNewReview = onNewReview,
                onOpenReview = onOpenReview,
                onTogglePinReview = onTogglePinReview,
                onDeleteReview = onDeleteReview,
            )
        }
    }
}
