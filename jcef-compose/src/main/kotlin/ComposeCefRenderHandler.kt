package com.saurabhsandav.libs.jcefcompose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.Density
import org.cef.browser.CefBrowser
import org.cef.handler.CefRenderHandlerAdapter
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.awt.Cursor
import java.awt.IllegalComponentStateException
import java.awt.Point
import java.awt.Rectangle
import java.nio.ByteBuffer

internal class ComposeCefRenderHandler : CefRenderHandlerAdapter() {

    var density = Density(1F, 1F)
    var pointerIcon by mutableStateOf<PointerIcon?>(null)
    var image by mutableStateOf<Image?>(null)

    private val screenPoint = Point(0, 0)
    private val browserRect = Rectangle()
    private var imageInfo = ImageInfo.DEFAULT
    private var byteArray = ByteArray(0)

    fun onPositioned(coordinates: LayoutCoordinates) = with(density) {

        browserRect.setRect(
            /* x = */ 0.0,
            /* y = */ 0.0,
            /* width = */ coordinates.size.width.toDp().value.toDouble(),
            /* height = */ coordinates.size.height.toDp().value.toDouble(),
        )

        val positionOnScreen = try {
            coordinates.positionOnScreen()
        } catch (_: IllegalComponentStateException) {
            return@with
        }

        screenPoint.setLocation(positionOnScreen.x.toDouble(), positionOnScreen.y.toDouble())
    }

    override fun onPaint(
        browser: CefBrowser,
        popup: Boolean,
        dirtyRects: Array<out Rectangle>,
        buffer: ByteBuffer,
        width: Int,
        height: Int,
    ) {

        when {
            imageInfo.width != width || imageInfo.height != height -> {

                imageInfo = ImageInfo.makeS32(
                    width = width,
                    height = height,
                    alphaType = ColorAlphaType.PREMUL,
                )

                byteArray = ByteArray(buffer.remaining())

                buffer.get(byteArray)
            }

            else -> for (rect in dirtyRects) {

                fun pixelStartIndex(x: Int, y: Int) = (x + (y * imageInfo.width)) * CefPixelBytes

                val startPixelIndex = pixelStartIndex(rect.x, rect.y)
                val endPixelIndex = pixelStartIndex(rect.x + rect.width - 1, rect.y + rect.height - 1)
                // Add another pixel bytes to include last pixel bytes
                val length = endPixelIndex + CefPixelBytes - startPixelIndex

                buffer.get(startPixelIndex, byteArray, startPixelIndex, length)
            }
        }

        image = Image.makeRaster(
            imageInfo = imageInfo,
            bytes = byteArray,
            rowBytes = width * CefPixelBytes,
        )
    }

    override fun getScreenPoint(browser: CefBrowser, viewPoint: Point): Point =
        Point(screenPoint).apply { translate(viewPoint.x, viewPoint.y) }

    override fun getViewRect(browser: CefBrowser): Rectangle = browserRect

    override fun getDeviceScaleFactor(browser: CefBrowser): Double = density.density.toDouble()

    override fun onCursorChange(browser: CefBrowser, cursorType: Int): Boolean {
        pointerIcon = PointerIcon(Cursor(cursorType))
        return true
    }
}

private const val CefPixelBytes = 4
