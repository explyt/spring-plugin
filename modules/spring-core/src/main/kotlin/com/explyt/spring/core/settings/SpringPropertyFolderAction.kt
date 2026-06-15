/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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
