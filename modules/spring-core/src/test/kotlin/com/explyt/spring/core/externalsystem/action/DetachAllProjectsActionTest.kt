/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.externalsystem.setting.NativeProjectSettings
import com.explyt.spring.core.externalsystem.utils.Constants.DEBUG_SESSION_NAME
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class DetachAllProjectsActionTest : LightJavaCodeInsightFixtureTestCase() {

    override fun setUp() {
        super.setUp()
        unlinkAll()
    }

    override fun tearDown() {
        try {
            unlinkAll()
        } catch (e: Throwable) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
    }

    private fun unlinkAll() {
        val settings = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
        settings.linkedProjectsSettings.mapNotNull { it.externalProjectPath }
            .forEach { settings.unlinkExternalProject(it) }
    }

    private fun linkProject(externalProjectPath: String) {
        val settings = NativeProjectSettings()
        settings.externalProjectPath = externalProjectPath
        ExternalSystemApiUtil.getSettings(project, SYSTEM_ID).linkProject(settings)
    }

    fun testDetachAllUnlinksProjectWithoutImportData() {
        // A link whose resolve never succeeded (or whose cached structure was dropped) has no
        // externalProjectStructure, so enumerating the import-data cache never sees it.
        linkProject("${project.basePath}/billing/src/main/kotlin/com/explyt/BillingApplication.kt")

        DetachAllProjectsAction.detachAllProjects(project)

        assertTrue(
            "Detach All must drop links that have no import data",
            ExternalSystemApiUtil.getSettings(project, SYSTEM_ID).linkedProjectsSettings.isEmpty()
        )
    }

    fun testDetachAllUnlinksDebugSession() {
        // The debug session entry can never have import data: resolveProjectInfo returns null
        // for DEBUG_SESSION_NAME, so it is invisible to the import-data cache by construction.
        linkProject(DEBUG_SESSION_NAME)

        DetachAllProjectsAction.detachAllProjects(project)

        assertTrue(
            "Detach All must drop the debug session link",
            ExternalSystemApiUtil.getSettings(project, SYSTEM_ID).linkedProjectsSettings.isEmpty()
        )
    }
}
