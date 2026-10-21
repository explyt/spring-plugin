/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.externalsystem.utils.Constants
import com.intellij.openapi.externalSystem.action.DetachExternalProjectAction
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.view.ProjectNode
import com.intellij.openapi.project.Project
import org.junit.Assert
import org.junit.Test
import java.lang.reflect.Modifier

class UnlinkProjectAwareTest {

    /**
     * [DetachAllProjectsAction.detachProjectNode] reaches [DetachExternalProjectAction.detachProject] by
     * reflection, because the platform class is internal API. Reflection resolves the method by name only,
     * so a platform signature change would degrade the detach action into a no-op at runtime instead of
     * failing the build. This test is the compile- and signature-level guard for that call.
     */
    @Test
    fun detachProjectSignatureIsStable() {
        val detachMethods = DetachExternalProjectAction::class.java.declaredMethods
            .filter { it.name == "detachProject" }

        Assert.assertEquals(
            "DetachExternalProjectAction must declare exactly one 'detachProject' method",
            1, detachMethods.size
        )

        val detachProject = detachMethods.single()
        Assert.assertTrue(
            "'detachProject' must stay static, it is invoked with a null receiver",
            Modifier.isStatic(detachProject.modifiers)
        )
        Assert.assertArrayEquals(
            "'detachProject' parameter types must match the reflective invocation arguments",
            arrayOf(Project::class.java, ProjectSystemId::class.java, ProjectData::class.java, ProjectNode::class.java),
            detachProject.parameterTypes
        )
    }

    fun unlinkProject(project: Project, externalProjectPath: String) {
        val systemId = Constants.SYSTEM_ID
        val projectData = ExternalSystemApiUtil.findProjectNode(project, systemId, externalProjectPath)?.data ?: return
        DetachExternalProjectAction.detachProject(project, projectData.owner, projectData, null)
    }
}
