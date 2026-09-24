/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties

import junit.framework.TestCase

/**
 * A library ships one declaration in several files, and navigation must offer it once.
 *
 * The identity that decides "same artifact" is the jar's file name, not its path: Gradle caches a jar and its
 * sources jar under two different checksum directories, so anything path-based leaves the two copies in separate
 * groups and the user is asked to pick between identical entries.
 */
class MetadataDeclarationsTest : TestCase() {

    fun testGradleLayoutFoldsTheSourcesJarIntoItsBinaryTwin() {
        assertEquals(
            MetadataDeclarations.artifactOf(GRADLE_BINARY),
            MetadataDeclarations.artifactOf(GRADLE_SOURCES)
        )
    }

    fun testSourcesJarIsThePreferredCopy() {
        assertTrue(
            "The sources jar must rank before the binary jar",
            MetadataDeclarations.rankOf(GRADLE_SOURCES) < MetadataDeclarations.rankOf(GRADLE_BINARY)
        )
    }

    /** The layout that already worked before the artifact identity became name-based. */
    fun testMavenLayoutStillFolds() {
        val binary = "$MAVEN_DIR/spring-boot-3.5.16.jar$METADATA"
        val sources = "$MAVEN_DIR/spring-boot-3.5.16-sources.jar$METADATA"

        assertEquals(MetadataDeclarations.artifactOf(binary), MetadataDeclarations.artifactOf(sources))
    }

    fun testDifferentVersionsOfOneArtifactDoNotFold() {
        val older = "$MAVEN_DIR/spring-boot-3.5.15.jar$METADATA"
        val newer = "$MAVEN_DIR/spring-boot-3.5.16.jar$METADATA"

        assertFalse(
            "The version is part of the file name, so two versions are two artifacts",
            MetadataDeclarations.artifactOf(older) == MetadataDeclarations.artifactOf(newer)
        )
    }

    fun testDifferentArtifactsDoNotFold() {
        val boot = "$MAVEN_DIR/spring-boot-3.5.16.jar$METADATA"
        val autoconfigure = "$MAVEN_DIR/spring-boot-autoconfigure-3.5.16.jar$METADATA"

        assertFalse(
            MetadataDeclarations.artifactOf(boot) == MetadataDeclarations.artifactOf(autoconfigure)
        )
    }

    /** A project file belongs to no artifact, so it is grouped by its directory. */
    fun testProjectFileIsGroupedByItsDirectory() {
        val path = "/home/dev/explyt-demo/src/main/resources/META-INF/$ADDITIONAL"

        assertEquals("/home/dev/explyt-demo/src/main/resources/META-INF", MetadataDeclarations.artifactOf(path))
    }

    fun testHandWrittenMetadataIsPreferredOverGeneratedWithinOneArtifact() {
        val generated = "$MAVEN_DIR/spring-boot-3.5.16.jar$METADATA"
        val additional = "$MAVEN_DIR/spring-boot-3.5.16.jar!/META-INF/$ADDITIONAL"

        assertEquals(MetadataDeclarations.artifactOf(generated), MetadataDeclarations.artifactOf(additional))
        assertTrue(
            "The hand-written metadata file must rank before the generated one",
            MetadataDeclarations.rankOf(additional) < MetadataDeclarations.rankOf(generated)
        )
    }

    /** The whole point of the grouping: `distinct` keeps one target per declaration. */
    fun testOneTargetSurvivesForADeclarationShippedByBothJars() {
        val declarations = listOf("explyt.demo.enabled" to GRADLE_BINARY, "explyt.demo.enabled" to GRADLE_SOURCES)

        val kept = declarations
            .groupBy { it.first to MetadataDeclarations.artifactOf(it.second) }
            .values
            .mapNotNull { group -> group.minByOrNull { MetadataDeclarations.rankOf(it.second) } }

        assertEquals(1, kept.size)
        assertEquals(GRADLE_SOURCES, kept.single().second)
    }

    private companion object {
        const val ADDITIONAL = "additional-spring-configuration-metadata.json"
        const val METADATA = "!/META-INF/spring-configuration-metadata.json"
        const val GRADLE_CACHE = "/home/dev/.gradle/caches/modules-2/files-2.1/org.springframework.boot/spring-boot/3.5.16"
        const val MAVEN_DIR = "/home/dev/.m2/repository/org/springframework/boot/spring-boot/3.5.16"

        const val GRADLE_BINARY = "$GRADLE_CACHE/8e75e8d0d3cfbca088774df8d30b45bbd21e5d99/spring-boot-3.5.16.jar$METADATA"
        const val GRADLE_SOURCES =
            "$GRADLE_CACHE/efdac62e4add33a9bdd5b9e00cb1e03b0cc649e0/spring-boot-3.5.16-sources.jar$METADATA"
    }
}
