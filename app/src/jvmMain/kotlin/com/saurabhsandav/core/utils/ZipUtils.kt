package com.saurabhsandav.core.utils

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.outputStream
import kotlin.io.path.pathString

object ZipUtils {

    fun zip(
        paths: List<Path>,
        outPath: Path,
        onProgress: ((ZipProgress) -> Unit)? = null,
    ) {

        ZipArchiveOutputStream(outPath.outputStream().buffered()).use { outStream ->

            val progressBuilder = onProgress?.let(::ZipProgressBuilder)

            paths
                .flatMap { path ->
                    Files.walk(path).toList().map { pathToZip ->
                        progressBuilder?.addTotalSize(pathToZip.fileSize())
                        val pathInZip = path.parent.relativize(pathToZip)
                        pathToZip to pathInZip
                    }
                }
                .forEach { (pathToZip, pathInZip) ->

                    val fileSize = pathToZip.fileSize()
                    val entry = ZipArchiveEntry(pathToZip, pathInZip.pathString)

                    progressBuilder?.setPath(path = pathToZip.absolutePathString(), size = fileSize)

                    outStream.putArchiveEntry(entry)

                    when {
                        pathToZip.isDirectory() -> progressBuilder?.addPathCopied(copied = fileSize)
                        else -> pathToZip.inputStream().buffered().use { inStream ->

                            var totalCopied = 0L
                            inStream.copyTo(outStream) { bytesCopied ->
                                val delta = bytesCopied - totalCopied
                                totalCopied = bytesCopied
                                progressBuilder?.addPathCopied(delta)
                            }
                        }
                    }

                    outStream.closeArchiveEntry()
                }

            outStream.finish()

            progressBuilder?.finish()
        }
    }

    fun unzip(
        zipPath: Path,
        outDir: Path,
        onProgress: ((ZipProgress) -> Unit)? = null,
    ) {

        require(outDir.isDirectory()) { "${outDir.absolutePathString()} is not a directory" }

        val progressBuilder = onProgress?.let(::ZipProgressBuilder)

        ZipFile.builder().setPath(zipPath).get().use { zipFile ->

            zipFile.entries.asIterator().forEach { progressBuilder?.addTotalSize(it.size) }

            zipFile.entries.asIterator().forEach { entry ->

                progressBuilder?.setPath(entry.name, entry.size)

                val outPath = outDir.resolve(entry.name)

                when {
                    entry.isDirectory -> {

                        outPath.createDirectories()

                        progressBuilder?.addPathCopied(entry.size)
                    }

                    else -> outPath.outputStream().buffered().use { outStream ->
                        zipFile.getInputStream(entry).use { inStream ->

                            var totalCopied = 0L
                            inStream.copyTo(outStream) { bytesCopied ->
                                val delta = bytesCopied - totalCopied
                                totalCopied = bytesCopied
                                progressBuilder?.addPathCopied(delta)
                            }
                        }
                    }
                }
            }

            progressBuilder?.finish()
        }
    }
}

private class ZipProgressBuilder(
    private val onProgress: (ZipProgress) -> Unit,
) {

    private var progress = ZipProgress(
        copied = 0L,
        size = 0L,
        pathProgress = null,
    )

    fun addTotalSize(size: Long) {

        val newProgress = progress.copy(size = progress.size + size)

        emitProgress(newProgress)
    }

    fun setPath(
        path: String,
        size: Long,
    ) {

        val newProgress = progress.copy(
            pathProgress = PathZipProgress(
                path = path,
                copied = 0,
                size = size,
            ),
        )

        emitProgress(newProgress)
    }

    fun addPathCopied(copied: Long) {

        val newProgress = progress.copy(
            copied = progress.copied + copied,
            pathProgress = progress.pathProgress?.let { it ->
                it.copy(copied = it.copied + copied)
            },
        )

        emitProgress(newProgress)
    }

    fun finish() {

        val newProgress = progress.copy(pathProgress = null)

        emitProgress(newProgress)
    }

    private fun emitProgress(newProgress: ZipProgress) {
        if (newProgress == progress) return
        onProgress(newProgress)
        progress = newProgress
    }
}

data class ZipProgress(
    val copied: Long,
    val size: Long,
    val pathProgress: PathZipProgress?,
)

data class PathZipProgress(
    val path: String,
    val copied: Long,
    val size: Long,
)
