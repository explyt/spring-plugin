/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
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