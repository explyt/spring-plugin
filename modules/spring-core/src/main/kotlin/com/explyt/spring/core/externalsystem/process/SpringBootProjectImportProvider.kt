/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.process

import com.explyt.spring.core.externalsystem.utils.Constants
import com.explyt.spring.core.externalsystem.utils.NativeBootUtils
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class SpringBootProjectImportProvider : AbstractExternalProjectImportProvider(
    LinkSpringBootProjectImportBuilder(), Constants.SYSTEM_ID
) {

    override fun createSteps(context: WizardContext): Array<ModuleWizardStep> = ModuleWizardStep.EMPTY_ARRAY

    override fun canImport(fileOrDirectory: VirtualFile, project: Project?): Boolean {
        if (project == null || fileOrDirectory.isDirectory) return false
        val mainRootFiles = NativeBootUtils.getMainRootFiles(project)
        return mainRootFiles.contains(fileOrDirectory)
    }

    override fun getId(): String {
        return fileSample
    }

    override fun getName(): String {
        return fileSample
    }

    override fun getFileSample(): String {
        return "Spring Boot Application Main Class"
    }
}