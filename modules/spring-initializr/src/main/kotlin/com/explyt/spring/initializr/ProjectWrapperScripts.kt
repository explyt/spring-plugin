/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.initializr

import com.intellij.execution.wsl.WslPath
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

/**
 * start.spring.io serves the build wrappers inside a zip, and the zip entry permissions do not survive
 * [com.intellij.util.io.ZipUtil.extract]. The generated project therefore arrives with a non-executable `gradlew`,
 * and the first `./gradlew` in a terminal fails with "permission denied" until the user chmods it by hand.
 */
internal object ProjectWrapperScripts {

    /** Only the POSIX shell wrappers: `gradlew.bat` and `mvnw.cmd` are launched by cmd.exe and need no bit. */
    private val EXECUTABLE_NAMES = listOf("gradlew", "mvnw")

    private val logger = Logger.getInstance(ProjectWrapperScripts::class.java)

    fun makeRunnable(projectDirectory: Path) {
        // setPosixFilePermissions throws on a non-POSIX file system, so skip Windows unless the target is a WSL path.
        if (SystemInfo.isWindows && !WslPath.isWslUncPath(projectDirectory.absolutePathString())) return

        val permissions = PosixFilePermissions.fromString("rwxr-xr-x")
        for (name in EXECUTABLE_NAMES) {
            val script = projectDirectory.resolve(name).takeIf { it.exists() } ?: continue
            try {
                Files.setPosixFilePermissions(script, permissions)
            } catch (e: IOException) {
                // A generated project is still usable without the bit, so report and carry on rather than failing
                // project creation over a chmod.
                logger.warn("Cannot make '$name' executable in $projectDirectory", e)
            } catch (e: UnsupportedOperationException) {
                logger.warn("File system of $projectDirectory does not support POSIX permissions", e)
            }
        }
    }
}
