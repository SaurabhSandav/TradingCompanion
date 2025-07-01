package com.saurabhsandav.core.ui.reviews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.PagingData
import com.saurabhsandav.core.ui.common.PrimaryOptionsBar
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.reviews.model.ReviewsState.ReviewEntry
import com.saurabhsandav.core.ui.reviews.ui.ReviewsList
import com.saurabhsandav.trading.record.model.ReviewId
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
        ) { paddingValues ->

            Column(Modifier.padding(paddingValues)) {

                PrimaryOptionsBar {

                    Button(
                        onClick = onNewReview,
                        shape = MaterialTheme.shapes.small,
                        content = { Text("New Review") },
                    )
                }

                HorizontalDivider()

                ReviewsList(
                    reviewEntries = reviewEntries,
                    onOpenReview = onOpenReview,
                    onTogglePinReview = onTogglePinReview,
                    onDeleteReview = onDeleteReview,
                )
            }
        }
    }
}
