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

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.externalsystem.utils.Constants
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.projectImport.ProjectOpenProcessor

class SpringBootProjectOpenProcessor : ProjectOpenProcessor() {
    private val importProvider = SpringBootOpenProjectProvider()

    override fun canOpenProject(file: VirtualFile): Boolean = importProvider.canOpenProject(file)

    override fun doOpenProject(
        virtualFile: VirtualFile,
        projectToClose: Project?,
        forceOpenInNewFrame: Boolean
    ): Project? {
        return runWithModalProgressBlocking(ModalTaskOwner.guess(), "") {
            importProvider.openProject(virtualFile, projectToClose, forceOpenInNewFrame)
        }
    }

    override val name = Constants.SYSTEM_ID.readableName

    override val icon = SpringIcons.Spring

    override fun canImportProjectAfterwards(): Boolean = true

    @Deprecated("use async method instead")
    override fun importProjectAfterwards(project: Project, file: VirtualFile) {
        importProvider.linkToExistingProject(file, project)
    }

    override suspend fun importProjectAfterwardsAsync(project: Project, file: VirtualFile) {
        importProvider.linkToExistingProjectAsync(file, project)
    }
}