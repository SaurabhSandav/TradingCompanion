package com.saurabhsandav.core.ui.common

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import kotlin.math.roundToInt

@Composable
fun SideSheetHost(
    sheetState: SideSheetState,
    sheet: @Composable () -> Unit,
    onDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
    scrimColor: Color = DrawerDefaults.scrimColor,
    content: @Composable () -> Unit,
) {

    val transition = updateTransition(sheetState, label = "Side Sheet transition")

    val alpha by transition.animateFloat(label = "alpha") { state ->
        when (state) {
            SideSheetState.Open -> 1F
            SideSheetState.Closed -> 0F
        }
    }

    val offset by transition.animateFloat(label = "Sheet offset") { state ->
        when (state) {
            SideSheetState.Open -> 1F
            SideSheetState.Closed -> 0F
        }
    }

    Layout(
        modifier = modifier,
        contents = buildList {

            add(content)

            if (
                transition.currentState == transition.targetState &&
                sheetState != SideSheetState.Open
            ) {
                return@buildList
            }

            add {
                Scrim(
                    scrimColor = scrimColor,
                    onDismissSheet = onDismissSheet,
                    alpha = { alpha },
                )
            }

            add(sheet)
        },
    ) { measurables, constraints ->

        // Main content
        val contentPlaceables = measurables[0].map { it.measure(constraints) }

        // Scrim
        val scrimPlaceable = measurables.getOrNull(1)?.single()?.measure(constraints)

        // Sheet content
        val sheetPlaceable = measurables
            .getOrNull(2)
            ?.singleOrNull()
            ?.measure(constraints.copy(minHeight = constraints.maxHeight))

        layout(constraints.maxWidth, constraints.maxHeight) {

            // Main content
            contentPlaceables.forEach { it.place(0, 0) }

            // Scrim
            scrimPlaceable?.place(0, 0)

            // Sheet content
            sheetPlaceable?.place(constraints.maxWidth - (sheetPlaceable.width * offset).roundToInt(), 0)
        }
    }
}

@Composable
fun SideSheet(content: @Composable () -> Unit) {

    Surface(
        content = content,
    )
}

@Composable
private fun Scrim(
    scrimColor: Color,
    onDismissSheet: () -> Unit,
    alpha: () -> Float,
) {

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(onDismissSheet) { detectTapGestures { onDismissSheet() } },
    ) {
        drawRect(scrimColor, alpha = alpha())
    }
}

enum class SideSheetState {
    Open,
    Closed,
}
