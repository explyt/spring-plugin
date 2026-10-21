/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.initializr

import com.intellij.openapi.util.SystemInfo
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.isExecutable
import kotlin.io.path.writeText

class ProjectWrapperScriptsTest {

    private lateinit var projectDirectory: Path

    @Before
    fun setUp() {
        // Every assertion is about a POSIX permission bit, which a Windows file system does not carry.
        Assume.assumeFalse(SystemInfo.isWindows)
        projectDirectory = Files.createTempDirectory("initializr-project")
    }

    private fun file(name: String): Path =
        projectDirectory.resolve(name).also { it.writeText("#!/bin/sh\n") }

    @Test
    fun testGradleWrapperBecomesExecutable() {
        val gradlew = file("gradlew")
        Assert.assertFalse("precondition: an extracted wrapper is not executable", gradlew.isExecutable())

        ProjectWrapperScripts.makeRunnable(projectDirectory)

        Assert.assertTrue("gradlew must be runnable from a terminal", gradlew.isExecutable())
    }

    @Test
    fun testMavenWrapperBecomesExecutable() {
        val mvnw = file("mvnw")

        ProjectWrapperScripts.makeRunnable(projectDirectory)

        Assert.assertTrue("mvnw must be runnable from a terminal", mvnw.isExecutable())
    }

    @Test
    fun testWindowsWrappersAndUnrelatedFilesAreLeftAlone() {
        val batch = file("gradlew.bat")
        val cmd = file("mvnw.cmd")
        val settings = file("settings.gradle.kts")

        ProjectWrapperScripts.makeRunnable(projectDirectory)

        Assert.assertFalse("gradlew.bat is launched by cmd.exe and needs no bit", batch.isExecutable())
        Assert.assertFalse("mvnw.cmd is launched by cmd.exe and needs no bit", cmd.isExecutable())
        Assert.assertFalse("an unrelated project file must not be touched", settings.isExecutable())
    }

    @Test
    fun testMissingWrapperIsNotAnError() {
        // A Maven project has no gradlew and vice versa, so the absent one must simply be skipped.
        val mvnw = file("mvnw")

        ProjectWrapperScripts.makeRunnable(projectDirectory)

        Assert.assertTrue(mvnw.isExecutable())
    }

    @Test
    fun testDirectoryNamedLikeAWrapperIsNotFollowed() {
        // Guards against a zip that contains a `gradlew/` entry: chmodding it would be harmless but the call must
        // not fail project creation either way.
        projectDirectory.resolve("gradlew").createDirectory()

        ProjectWrapperScripts.makeRunnable(projectDirectory)
    }
}
