/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.test.util

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.java.library.JavaLibraryModificationTracker
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.DumbModeTestUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

fun <T> Project.computeInDumbModeWithColdLibraryCaches(compute: () -> T): T =
    DumbModeTestUtils.computeInDumbModeSynchronously(this, ThrowableComputable<T, Throwable> {
        dropLibraryCaches()
        runCatching(compute).getOrElse { throw AssertionError("Threw in dumb mode: $it", it) }
    })

fun LocalInspectionTool.isAvailableInDumbMode(file: PsiFile): Boolean =
    file.project.computeInDumbModeWithColdLibraryCaches { isAvailableForFile(file) }

fun LocalInspectionTool.assertAvailableOnlyInSmartMode(file: PsiFile) {
    assertTrue("${javaClass.name} must be available for ${file.name} in smart mode", isAvailableForFile(file))
    assertFalse("${javaClass.name} must not be available for ${file.name} in dumb mode", isAvailableInDumbMode(file))
}

@Suppress("UnstableApiUsage")
fun Project.dropLibraryCaches() {
    JavaLibraryModificationTracker.incModificationCount(this)
    WriteAction.run<Throwable> { PsiManager.getInstance(this).dropPsiCaches() }
}
