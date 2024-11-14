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

package com.explyt.spring.web.service

import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.model.OpenApiSpecificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.util.messages.MessageBusConnection

@Service(Service.Level.PROJECT)
@State(name = "OpenApiLocalState")
class OpenApiLocalSpecifications : PersistentStateComponent<OpenApiLocalState>, Disposable {

    private var connection: MessageBusConnection? = null
    private val localState: MutableMap<String, OpenApiSpecificationType> = mutableMapOf()

    init {
        connection = ApplicationManager.getApplication().messageBus.connect(this)
        connection?.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun before(events: List<VFileEvent>) {
                events.forEach { event -> processVFileEvent(event) }
            }
        })
    }

    override fun getState(): OpenApiLocalState? {
        val stateMap = if (localState.isNotEmpty()) {
            localState.mapValues { (_, value) -> serializeSpecificationType(value) }
        } else null

        return stateMap?.let { OpenApiLocalState(it) }
    }

    override fun loadState(state: OpenApiLocalState) {
        val specTypeByUrl = state.metadataByUrl.orEmpty()
        val deserializedState = specTypeByUrl.mapValues { deserializeSpecificationType(it.value) }
        localState.putAll(deserializedState)
    }

    fun getSpecificationType(fileUrl: String): OpenApiSpecificationType {
        return localState[fileUrl] ?: OpenApiSpecificationType.OpenApiUndefined
    }

    private fun processVFileEvent(event: VFileEvent) {
        when {
            event is VFilePropertyChangeEvent && isRenameEvent(event) -> updateFile(event)
            event is VFileDeleteEvent -> deleteFile(event)
            event is VFileMoveEvent -> movedFile(event)
        }
    }

    private fun isRenameEvent(event: VFilePropertyChangeEvent): Boolean {
        return event.propertyName == VirtualFile.PROP_NAME &&
                event.oldValue is String &&
                event.newValue is String &&
                event.oldValue != event.newValue
    }

    private fun deleteFile(event: VFileEvent) {
        localState.remove(event.file?.url)
    }

    private fun updateFile(event: VFilePropertyChangeEvent) {
        val parentPath = event.file.parent?.url ?: return deleteFile(event)

        val newValue = event.newValue as? String ?: return deleteFile(event)
        val oldValue = event.oldValue as? String ?: return deleteFile(event)

        val newFilePath = "$parentPath/$newValue"
        val oldFilePath = "$parentPath/$oldValue"

        localState.remove(oldFilePath)?.let { localState[newFilePath] = it }
    }

    private fun movedFile(event: VFileMoveEvent) {
        val newParentPath = event.newParent.url
        val oldParentPath = event.oldParent?.url ?: return deleteFile(event)

        val fileName = event.file.name
        val newFilePath = "$newParentPath/$fileName"
        val oldFilePath = "$oldParentPath/$fileName"

        localState.remove(oldFilePath)?.let { localState[newFilePath] = it }
    }

    override fun dispose() {
        localState.clear()
    }

    private fun deserializeSpecificationType(metadata: SpecificationMetadata): OpenApiSpecificationType {
        if (metadata.schemaExt == null || metadata.type == null) return OpenApiSpecificationType.OpenApiUndefined
        return when (metadata.type) {
            SpringWebBundle.message("explyt.openapi.3.0.schema.type") ->
                OpenApiSpecificationType.OpenAPI30SpecificationExtension(metadata.schemaExt)

            SpringWebBundle.message("explyt.openapi.3.1.schema.type") ->
                OpenApiSpecificationType.Openapi31SpecificationExtension(metadata.schemaExt)

            else -> OpenApiSpecificationType.OpenApiUndefined
        }
    }

    private fun serializeSpecificationType(type: OpenApiSpecificationType) = SpecificationMetadata(
        type = type.toString(),
        schemaExt = (type as? OpenApiSpecificationType.SpecificationExtension)?.schemaExt
    )

}

data class OpenApiLocalState(
    val metadataByUrl: Map<String, SpecificationMetadata>? = null
)

data class SpecificationMetadata(
    val type: String? = null,
    val schemaExt: String? = null
)
