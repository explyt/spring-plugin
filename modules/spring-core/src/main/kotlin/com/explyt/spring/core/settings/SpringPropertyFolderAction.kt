/*
 * Copyright © 2025 Explyt Ltd
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

package com.explyt.spring.core.settings

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class SpringPropertyFolderAction : AnAction() {
    init {
        getTemplatePresentation().text = SpringCoreBundle.message("explyt.spring.folder.mark.property")
        getTemplatePresentation().icon = SpringIcons.SpringBoot
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        if (SpringPropertyFolderState.isUserPropertyFolder(project, file)) {
            SpringPropertyFolderState.removeUserPropertyFolder(project, file)
        } else {
            SpringPropertyFolderState.addUserPropertyFolder(project, file)
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.isDirectory ?: false
        file ?: return
        val propertyFolder = SpringPropertyFolderState.isUserPropertyFolder(project, file)
        if (propertyFolder) {
            e.presentation.text = SpringCoreBundle.message("explyt.spring.folder.unmark.property")
        } else {
            e.presentation.text = SpringCoreBundle.message("explyt.spring.folder.mark.property")
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
