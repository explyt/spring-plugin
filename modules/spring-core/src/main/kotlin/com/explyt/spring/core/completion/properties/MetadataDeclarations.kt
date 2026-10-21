/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties

import com.explyt.spring.core.SpringProperties.ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME
import com.intellij.psi.PsiFile
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.annotations.VisibleForTesting

/**
 * One navigation target per metadata declaration.
 *
 * A library ships the same declaration up to three times: the hand-written `additional-spring-configuration-metadata.json`
 * and the generated `spring-configuration-metadata.json` of the binary jar, plus the `additional-…` copy inside the
 * `-sources` jar. They are one declaration, so offering three targets for one key only asks the user to pick at random.
 */
object MetadataDeclarations {

    /** One target per declaration name and artifact, keeping the preferred copy of each. */
    fun <T> distinct(targets: Iterable<T>, name: (T) -> String, file: (T) -> PsiFile): List<T> {
        return targets
            .groupBy { name(it) to artifactOf(file(it)) }
            .values
            .mapNotNull { group -> group.minByOrNull { rank(pathOf(file(it))) } }
    }

    /** The single preferred target among [targets], which are assumed to be the same declaration. */
    fun <T> preferred(targets: Iterable<T>, file: (T) -> PsiFile): T? {
        return targets.minByOrNull { rank(pathOf(file(it))) }
    }

    /**
     * The lower the better: the sources jar first, matching what navigation offers everywhere else in the IDE, and the
     * hand-written metadata file before the generated one, because it is what the library actually maintains.
     */
    private fun rank(path: String): Int {
        val sourcesRank = if (jarOf(path).endsWith(SOURCES_JAR_SUFFIX)) 0 else 2
        val fileRank = if (path.endsWith(ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME)) 0 else 1
        return sourcesRank + fileRank
    }

    private fun artifactOf(file: PsiFile): String = artifactOf(pathOf(file))

    /**
     * The artifact [path] belongs to: the jar's file name, with its `-sources` twin folded into it, or the
     * containing directory for a file that is not in a jar.
     *
     * The *name* rather than the whole path, because Gradle caches a jar and its sources jar under two different
     * checksum directories (`.../spring-boot/3.5.16/efdac62e.../spring-boot-3.5.16-sources.jar` next to
     * `.../spring-boot/3.5.16/8e75e8d0.../spring-boot-3.5.16.jar`). Folding the suffix while keeping those
     * directories left the two copies in separate groups, so a key declared by a library offered the same
     * declaration twice. The version is part of the file name, so distinct versions of one artifact stay distinct.
     */
    @VisibleForTesting
    fun artifactOf(path: String): String {
        if (!path.contains(JAR_SEPARATOR)) return path.substringBeforeLast('/')
        val jarName = jarOf(path).substringAfterLast('/')
        return if (jarName.endsWith(SOURCES_JAR_SUFFIX)) {
            jarName.removeSuffix(SOURCES_JAR_SUFFIX) + JAR_SUFFIX
        } else {
            jarName
        }
    }

    /** The preference of [path] among the copies of one declaration; see [rank]. */
    @VisibleForTesting
    fun rankOf(path: String): Int = rank(path)

    private fun jarOf(path: String): String = path.substringBefore(JAR_SEPARATOR)

    private fun pathOf(file: PsiFile): String = file.viewProvider.virtualFile.path

    private const val JAR_SUFFIX = ".jar"
    private const val SOURCES_JAR_SUFFIX = "-sources$JAR_SUFFIX"
}
