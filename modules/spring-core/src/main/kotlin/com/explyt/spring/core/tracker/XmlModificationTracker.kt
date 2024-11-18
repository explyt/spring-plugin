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

package com.explyt.spring.core.tracker

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter

@Service(Service.Level.PROJECT)
class XmlModificationTracker(project: Project) : SimpleModificationTracker(), Disposable {

    init {
        project.messageBus
            .connect(this)
            .subscribe(
                VirtualFileManager.VFS_CHANGES,
                BulkVirtualFileListenerAdapter(XmlVirtualFileListener(project))
            )
    }

    override fun dispose() {}

    companion object {
        fun getInstance(project: Project): XmlModificationTracker = project.service()
    }

}

private class XmlVirtualFileListener(val project: Project) : VirtualFileListener {
    override fun fileCreated(event: VirtualFileEvent) {
        incModificationCountIfXml(event)
    }

    override fun fileDeleted(event: VirtualFileEvent) {
        incModificationCountIfXml(event)
    }

    override fun contentsChanged(event: VirtualFileEvent) {
        incModificationCountIfXml(event)
    }

    private fun incModificationCountIfXml(event: VirtualFileEvent) {
        if (event.fileName.endsWith(".xml")) {
            XmlModificationTracker.getInstance(project)
                .incModificationCount()
        }
    }
}
