package com.saurabhsandav.core.utils

import java.awt.Desktop
import java.io.File
import java.io.InputStream
import java.io.OutputStream

fun File.openExternally(onOpenNotSupported: () -> Unit = { error("Opening file externally is not supported") }) {

    if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
        onOpenNotSupported()
    }

    Desktop.getDesktop().open(this)
}

fun InputStream.copyTo(
    out: OutputStream,
    progress: (bytesCopied: Long) -> Unit,
): Long {

    var bytesCopied: Long = 0
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var bytes = read(buffer)

    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        progress(bytesCopied)
        bytes = read(buffer)
    }

    return bytesCopied
}
