/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.externalsystem.utils.Constants
import com.intellij.openapi.externalSystem.action.DetachExternalProjectAction
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import org.junit.Assert
import org.junit.Test

class UnlinkProjectAwareTest {
    //need for check DetachExternalProjectAction.detachProject exist because used reflection for call it
    @Test
    fun simpleSupportTest() {
        Assert.assertTrue(true)
    }

    fun unlinkProject(project: Project, externalProjectPath: String) {
        val systemId = Constants.SYSTEM_ID
        val projectData = ExternalSystemApiUtil.findProjectNode(project, systemId, externalProjectPath)?.data ?: return
        DetachExternalProjectAction.detachProject(project, projectData.owner, projectData, null)
    }
}