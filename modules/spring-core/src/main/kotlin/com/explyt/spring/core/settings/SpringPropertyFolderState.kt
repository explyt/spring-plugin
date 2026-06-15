/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

@Service(Service.Level.PROJECT)
@State(name = "ExplytSpringPropertyFolderState", storages = [Storage("explyt-spring-property-folder.xml")])
class SpringPropertyFolderState : SimplePersistentStateComponent<PropertyFolders>(PropertyFolders()) {

    companion object {
        fun getInstance(project: Project): SpringPropertyFolderState = project.service()

        fun isUserPropertyFolder(project: Project, virtualFile: VirtualFile): Boolean {
            if (!virtualFile.isDirectory) return false
            val canonicalPath = virtualFile.canonicalPath ?: return false
            val folders = getInstance(project).state.folders
                .takeIf { it.isNotEmpty() } ?: return false
            return folders.any { canonicalPath.startsWith(it) }
        }

        fun isUserPropertyFolder(currentPsiFile: PsiFile): Boolean {
            val virtualFile = currentPsiFile.virtualFile?.parent ?: return false
            return isUserPropertyFolder(currentPsiFile.project, virtualFile)
        }

        fun addUserPropertyFolder(project: Project, virtualFile: VirtualFile) {
            if (!virtualFile.isDirectory) return
            val canonicalPath = virtualFile.canonicalPath ?: return
            if (isUserPropertyFolder(project, virtualFile)) return
            getInstance(project).state.folders.add(canonicalPath)
        }

        fun removeUserPropertyFolder(project: Project, virtualFile: VirtualFile) {
            if (!virtualFile.isDirectory) return
            val canonicalPath = virtualFile.canonicalPath ?: return
            getInstance(project).state.folders.remove(canonicalPath)
        }
    }
}

class PropertyFolders : BaseState() {
    var folders by list<String>()
}