/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties

import com.explyt.spring.core.SpringProperties.ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME
import com.intellij.psi.PsiFile
import com.intellij.util.io.URLUtil.JAR_SEPARATOR

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

    /**
     * The artifact [file] belongs to: the jar, with its `-sources` twin folded into it, or the containing directory
     * for a file that is not in a jar.
     */
    private fun artifactOf(file: PsiFile): String {
        val path = pathOf(file)
        if (!path.contains(JAR_SEPARATOR)) return path.substringBeforeLast('/')
        val jar = jarOf(path)
        return if (jar.endsWith(SOURCES_JAR_SUFFIX)) jar.removeSuffix(SOURCES_JAR_SUFFIX) + JAR_SUFFIX else jar
    }

    private fun jarOf(path: String): String = path.substringBefore(JAR_SEPARATOR)

    private fun pathOf(file: PsiFile): String = file.viewProvider.virtualFile.path

    private const val JAR_SUFFIX = ".jar"
    private const val SOURCES_JAR_SUFFIX = "-sources$JAR_SUFFIX"
}
