/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.process

import com.explyt.spring.core.externalsystem.SpringBootNativeManager
import com.explyt.spring.core.externalsystem.setting.NativeProjectSettings
import com.explyt.spring.core.externalsystem.utils.Constants.DEBUG_SESSION_NAME
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class SpringBootOpenProjectProviderTest : LightJavaCodeInsightFixtureTestCase() {

    override fun setUp() {
        super.setUp()
        ExternalSystemApiUtil.getSettings(project, SYSTEM_ID).unlinkExternalProject(DEBUG_SESSION_NAME)
    }

    fun testManagerRegistersWithExternalSystemAutoImportTracker() {
        val manager: ExternalSystemAutoImportAware = SpringBootNativeManager()
        assertNull(manager.getAffectedExternalProjectPath("/tmp/changed-file", project))
        assertEmpty(manager.getAffectedExternalProjectFiles("/tmp/project", project))
    }

    fun testFirstDebugSessionRun() {
        val provider = SpringBootOpenProjectProvider()
        val runConfigurationId = "runConfigName"
        val settings = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
            .getLinkedProjectSettings(DEBUG_SESSION_NAME)
        assertNull(settings)
        provider.linkDebugProject(project, DEBUG_SESSION_NAME, runConfigurationId)
        val projectSettings = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
            .getLinkedProjectSettings(DEBUG_SESSION_NAME) as? NativeProjectSettings
        assertNotNull(projectSettings)
        assertEquals(runConfigurationId, projectSettings?.runConfigurationId)
    }

    fun testTwoDebugSessionRuns() {
        val provider = SpringBootOpenProjectProvider()
        val runConfigurationId1 = "runConfigName1"
        val settings1 = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
            .getLinkedProjectSettings(DEBUG_SESSION_NAME)
        assertNull(settings1)
        provider.linkDebugProject(project, DEBUG_SESSION_NAME, runConfigurationId1)
        val projectSettings1 = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
            .getLinkedProjectSettings(DEBUG_SESSION_NAME) as? NativeProjectSettings
        assertNotNull(projectSettings1)
        assertEquals(runConfigurationId1, projectSettings1?.runConfigurationId)

        val runConfigurationId2 = "runConfigName2"
        val settings2 = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
            .getLinkedProjectSettings(DEBUG_SESSION_NAME)
        assertNotNull(settings2)
        provider.linkDebugProject(project, DEBUG_SESSION_NAME, runConfigurationId2)
        val projectSettings2 = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
            .getLinkedProjectSettings(DEBUG_SESSION_NAME) as? NativeProjectSettings
        assertNotNull(projectSettings2)
        assertEquals(runConfigurationId2, projectSettings2?.runConfigurationId)
    }
}