/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.tracker

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.ProjectScope

/**
 * Moves when a library jar is reloaded in place, the one library change [JavaLibraryModificationTracker]
 * notices besides project roots - without also counting dumb mode as that tracker does.
 */
class LibraryReloadModificationTracker(private val project: Project, parent: Disposable) : SimpleModificationTracker() {

    init {
        project.messageBus.connect(parent).subscribe(FileDocumentManagerListener.TOPIC, object : FileDocumentManagerListener {
            private val libraries by lazy { ProjectScope.getLibrariesScope(project) }

            override fun fileWithNoDocumentChanged(file: VirtualFile) {
                if (project.isInitialized && libraries.contains(file)) incModificationCount()
            }
        })
    }
}
