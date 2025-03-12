package com.saurabhsandav.core.utils

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

object ZipUtils {

    fun createZip(
        paths: List<Path>,
        outPath: Path,
    ) {

        ZipArchiveOutputStream(outPath.outputStream().buffered()).use { outStream ->

            paths
                .flatMap { path ->
                    Files.walk(path).toList().map { pathToZip ->
                        val pathInZip = path.parent.relativize(pathToZip)
                        pathToZip to pathInZip
                    }
                }
                .forEach { (pathToZip, pathInZip) ->

                    val entry = ZipArchiveEntry(pathToZip, pathInZip.pathString)

                    outStream.putArchiveEntry(entry)

                    if (!pathToZip.isDirectory()) {
                        pathToZip.inputStream().buffered().use { it.copyTo(outStream) }
                    }

                    outStream.closeArchiveEntry()
                }

            outStream.finish()
        }
    }

    fun extractZip(
        zipPath: Path,
        outDir: Path,
    ) {

        require(outDir.isDirectory()) { "${outDir.absolutePathString()} is not a directory" }

        ZipFile.builder().setPath(zipPath).get().use { zipFile ->

            zipFile.entries.asIterator().forEach { entry ->

                val outPath = outDir.resolve(entry.name)

                when {
                    entry.isDirectory -> outPath.createDirectories()
                    else -> outPath.outputStream().buffered().use { outStream ->
                        zipFile.getInputStream(entry).use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                }
            }
        }
    }
}
