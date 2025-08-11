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