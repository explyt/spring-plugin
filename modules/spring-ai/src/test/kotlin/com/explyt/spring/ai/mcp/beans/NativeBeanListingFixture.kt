/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.explyt.spring.core.externalsystem.model.BeanSearch
import com.explyt.spring.core.externalsystem.model.SpringBeanData
import com.explyt.spring.core.externalsystem.model.SpringBeanType
import com.explyt.spring.core.externalsystem.setting.NativeProjectSettings
import com.explyt.spring.core.externalsystem.setting.NativeSettings
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.internal.InternalExternalProjectInfo
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsDataStorage
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.psi.PsiClass
import com.intellij.openapi.project.Project

internal class NativeBeanListingFixture(private val project: Project) {
    private val linkedPaths = mutableListOf<String>()

    fun clear() {
        val settings = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
        linkedPaths.forEach { settings.unlinkExternalProject(it) }
        linkedPaths.clear()
    }

    fun install(application: PsiClass, beans: List<Pair<String, String>>, suffix: String = "") {
        val base = application.navigationElement.containingFile.virtualFile.canonicalPath!!
        val path = base + suffix
        project.getService(NativeSettings::class.java).linkProject(NativeProjectSettings().apply {
            externalProjectPath = path
            qualifiedMainClassName = application.qualifiedName
        })
        linkedPaths += path

        val root = DataNode(
            ProjectKeys.PROJECT,
            ProjectData(SYSTEM_ID, application.name!! + suffix, project.basePath!!, path),
            null
        )
        root.createChild(BeanSearch.KEY, BeanSearch(true, path))
        beans.forEach { (beanName, type) ->
            root.createChild(SpringBeanData.KEY,
                SpringBeanData(beanName, type, "singleton", null, null, SpringBeanType.OTHER, true, true, false))
        }
        ExternalProjectsDataStorage.getInstance(project)
            .update(InternalExternalProjectInfo(SYSTEM_ID, path, root))
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }

}
