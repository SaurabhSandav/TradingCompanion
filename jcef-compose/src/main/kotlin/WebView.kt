package com.saurabhsandav.libs.jcefcompose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import org.cef.CefBrowserSettings
import org.cef.CefClient
import org.cef.browser.CefBrowserOsrWithHandler
import org.cef.browser.CefRequestContext
import org.jetbrains.skia.Rect
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import kotlin.math.roundToInt

fun CefClient.createComposeOffScreenBrowser(
    url: String? = null,
    context: CefRequestContext = CefRequestContext.getGlobalContext(),
    settings: CefBrowserSettings? = null,
    reverseScrollDirection: Boolean = true,
    scrollEventMultiplier: Int = 32,
): ComposeCefOSRBrowser {

    val browser = ComposeCefOSRBrowser(
        client = this,
        url = url,
        context = context,
        settings = settings,
        reverseScrollDirection = reverseScrollDirection,
        scrollEventMultiplier = scrollEventMultiplier,
    )

    browser.createImmediately()

    return browser
}

@Suppress("ktlint:standard:comment-wrapping")
@Stable
class ComposeCefOSRBrowser internal constructor(
    client: CefClient,
    url: String? = null,
    context: CefRequestContext? = null,
    settings: CefBrowserSettings? = null,
    val reverseScrollDirection: Boolean = true,
    val scrollEventMultiplier: Int = 32,
) : CefBrowserOsrWithHandler(
        /* client = */ client,
        /* url = */ url,
        /* context = */ context,
        /* renderHandler = */ ComposeCefRenderHandler(),
        /* component = */ null,
        /* settings = */ settings,
    )

@Composable
fun WebView(
    browser: ComposeCefOSRBrowser,
    modifier: Modifier = Modifier,
) {

    val renderHandler = browser.renderHandler as ComposeCefRenderHandler
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current

    val pointerIcon = renderHandler.pointerIcon
    val pointerIconModifier = if (pointerIcon == null) Modifier else Modifier.pointerHoverIcon(pointerIcon)

    LaunchedEffect(density) {
        renderHandler.density = density
        browser.notifyScreenInfoChanged()
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onFocusChanged { browser.setFocus(it.isFocused) }
            .focusRequester(focusRequester)
            .focusable()
            .onPlaced { coordinates ->
                renderHandler.onPositioned(coordinates)
                browser.wasResized(coordinates.size.width, coordinates.size.height)
            }
            .pointerInput(renderHandler) {

                awaitPointerEventScope {

                    while (true) {

                        val event = awaitPointerEvent()

                        // Focus when clicked
                        if (event.type == PointerEventType.Press) focusRequester.requestFocus()

                        // Forward mouse events to CEF
                        when (event.awtEventOrNull ?: continue) {
                            is MouseWheelEvent ->
                                browser.sendMouseWheelEvent(event.adjustedEvent(browser, density))

                            else -> browser.sendMouseEvent(event.adjustedEvent(density))
                        }
                    }
                }
            }
            .onKeyEvent { event ->
                browser.sendKeyEvent(event.awtEventOrNull ?: return@onKeyEvent false)
                true
            }
            .then(pointerIconModifier)
            .then(modifier),
    ) {

        drawIntoCanvas { canvas ->

            val image = renderHandler.image ?: return@drawIntoCanvas

            val rect = Rect.makeXYWH(
                l = 0F,
                t = 0F,
                w = size.width,
                h = size.height,
            )

            canvas.nativeCanvas.drawImageRect(image = image, dst = rect)
        }
    }
}

private fun PointerEvent.adjustedEvent(density: Density): MouseEvent {

    val offset = changes.single().position
    val awtEvent = awtEventOrNull!!

    @Suppress("ktlint:standard:comment-wrapping")
    return MouseEvent(
        /* source = */ awtEvent.component,
        /* id = */ when (type) {
            PointerEventType.Press -> MouseEvent.MOUSE_PRESSED
            PointerEventType.Release -> MouseEvent.MOUSE_RELEASED
            PointerEventType.Move -> MouseEvent.MOUSE_MOVED
            PointerEventType.Enter -> MouseEvent.MOUSE_ENTERED
            PointerEventType.Exit -> MouseEvent.MOUSE_EXITED
            else -> error("Unexpected PointerEventType")
        },
        /* when = */ awtEvent.getWhen(),
        /* modifiers = */ awtEvent.getModifiersEx(),
        /* x = */ with(density) { offset.x.toDp().value.roundToInt() },
        /* y = */ with(density) { offset.y.toDp().value.roundToInt() },
        /* xAbs = */ awtEvent.xOnScreen,
        /* yAbs = */ awtEvent.yOnScreen,
        /* clickCount = */ awtEvent.clickCount,
        /* popupTrigger = */ awtEvent.isPopupTrigger,
        /* button = */ awtEvent.button,
    )
}

private fun PointerEvent.adjustedEvent(
    browser: ComposeCefOSRBrowser,
    density: Density,
): MouseWheelEvent {

    val offset = changes.single().position
    val awtEvent = awtEventOrNull!! as MouseWheelEvent

    val scrollEventMultiplier = browser.scrollEventMultiplier
    val reverseScrollDirection = browser.reverseScrollDirection

    @Suppress("ktlint:standard:comment-wrapping")
    return MouseWheelEvent(
        /* source = */ awtEvent.component,
        /* id = */ awtEvent.id,
        /* when = */ awtEvent.getWhen(),
        /* modifiers = */ awtEvent.getModifiersEx(),
        /* x = */ with(density) { offset.x.toDp().value.roundToInt() },
        /* y = */ with(density) { offset.y.toDp().value.roundToInt() },
        /* xAbs = */ awtEvent.xOnScreen,
        /* yAbs = */ awtEvent.yOnScreen,
        /* clickCount = */ awtEvent.clickCount,
        /* popupTrigger = */ awtEvent.isPopupTrigger,
        /* scrollType = */ awtEvent.scrollType,
        /* scrollAmount = */ awtEvent.scrollAmount * scrollEventMultiplier,
        /* wheelRotation = */ awtEvent.wheelRotation * (if (reverseScrollDirection) -1 else 1),
        /* preciseWheelRotation = */ awtEvent.preciseWheelRotation * (if (reverseScrollDirection) -1 else 1),
    )
}
