/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.IdeaTestUtil

fun Module.useMockJdk(level: LanguageLevel, disposable: Disposable) {
    ModuleRootModificationUtil.setModuleSdk(this, registeredMockJdk(level, disposable))
    IdeaTestUtil.setModuleLanguageLevel(this, level, disposable)
}

private fun registeredMockJdk(level: LanguageLevel, disposable: Disposable): Sdk {
    val sdk = IdeaTestUtil.getMockJdk(level)
    val jdkTable = ProjectJdkTable.getInstance()
    return jdkTable.findJdk(sdk.name)
        ?: sdk.also { WriteAction.runAndWait<RuntimeException> { jdkTable.addJdk(it, disposable) } }
}
