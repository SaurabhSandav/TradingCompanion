package com.saurabhsandav.core.ui.reviews

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.trades.model.ReviewId
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.reviews.model.ReviewsState.Review
import com.saurabhsandav.core.ui.reviews.ui.ReviewsList
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ReviewsScreen(
    reviews: ImmutableList<Review>,
    onNewReview: () -> Unit,
    onOpenReview: (ReviewId) -> Unit,
    onDeleteReview: (ReviewId) -> Unit,
) {

    // Set window title
    WindowTitle("Reviews")

    Column {

        Scaffold(
            modifier = Modifier.weight(1F),
            floatingActionButton = {

                ExtendedFloatingActionButton(onClick = onNewReview) {
                    Text(text = "New Review")
                }
            },
        ) {

            ReviewsList(
                reviews = reviews,
                onOpenReview = onOpenReview,
                onDeleteReview = onDeleteReview,
            )
        }
    }
}
