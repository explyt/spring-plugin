/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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
