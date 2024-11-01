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
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import kotlin.jvm.internal.TypeIntrinsics

@Service(Service.Level.PROJECT)
@State(
    name = "OpenApiUserDefinedState",
)
class OpenApiUserDefinedSpecifications : PersistentStateComponent<OpenApiUserDefinedState>, Disposable {

    private val myState: MutableMap<String, OpenApiSpecificationType> = mutableMapOf()

    init {
        val connection = ApplicationManager.getApplication().messageBus.connect(this)
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun before(events: List<VFileEvent>) {
                events.forEach { event -> processVFileEvent(event) }
            }
        })
    }

    fun getSpecificationType(fileUrl: String): OpenApiSpecificationType {
        return myState[fileUrl] ?: OpenApiSpecificationType.UNKNOWN
    }

    private fun processVFileEvent(event: VFileEvent) {
        if (event is VFileDeleteEvent) {
            removeUrlForDeletedFile(event)
        } else if (event is VFileMoveEvent) {
            updateUrlForMovedFile(event)
        } else if (event is VFilePropertyChangeEvent && event.isRename) {
            updateUrlForRenamedFile(event)
        }
    }

    private fun removeUrlForDeletedFile(event: VFileEvent) {
        TypeIntrinsics.asMutableMap(myState).remove(event.file?.url)
    }


    private fun updateUrlForMovedFile(event: VFileMoveEvent) {
        val fileName = event.file.name
        val oldParentUrl = event.oldParent?.url
        val newParentUrl = event.newParent.url

        if (oldParentUrl != null) {
            val oldFileUrl = "$oldParentUrl/$fileName"
            val newFileUrl = "$newParentUrl/$fileName"

            myState.remove(oldFileUrl)?.let { oldValue ->
                myState[newFileUrl] = oldValue
            }
        } else {
            removeUrlForDeletedFile(event)
        }
    }

    private fun updateUrlForRenamedFile(event: VFilePropertyChangeEvent) {
        val parentUrl = event.file.parent?.url

        if (parentUrl != null) {
            val oldName = event.oldValue as? String
            val newName = event.newValue as? String

            if (oldName == null || newName == null) {
                removeUrlForDeletedFile(event)
                return
            }

            val oldFileUrl = "$parentUrl/$oldName"
            val newFileUrl = "$parentUrl/$newName"

            myState.remove(oldFileUrl)?.let { oldValue ->
                myState[newFileUrl] = oldValue
            }
        } else {
            removeUrlForDeletedFile(event)
        }
    }

    override fun dispose() {
        myState.clear()
    }

    override fun getState(): OpenApiUserDefinedState? {
        val stateMap = if (myState.isNotEmpty()) {
            myState.mapValues { (_, value) ->
                serializeSpecificationType(value)
            }
        } else null

        return stateMap?.let { OpenApiUserDefinedState(it) }
    }

    override fun loadState(state: OpenApiUserDefinedState) {
        val specTypeByUrl = state.specTypeByUrl ?: emptyMap()

        val deserializedState = specTypeByUrl.mapValues { entry ->
            deserializeSpecificationType(entry.value)
        }

        myState.putAll(deserializedState)
    }

    private fun deserializeSpecificationType(value: SerializedSpecificationType): OpenApiSpecificationType {
        val partId = value.partId
        val typeId = value.typeId

        return when {
            partId != null && typeId != null -> when (typeId) {
                SpringWebBundle.message("explyt.openapi.3.0.schema.type") ->
                    OpenApiSpecificationType.OpenAPI30SpecificationExtension(partId)

                SpringWebBundle.message("explyt.openapi.3.1.schema.type") ->
                    OpenApiSpecificationType.Openapi31SpecificationExtension(partId)

                else -> OpenApiSpecificationType.UNKNOWN
            }

            else -> OpenApiSpecificationType.UNKNOWN
        }
    }

    private fun serializeSpecificationType(type: OpenApiSpecificationType): SerializedSpecificationType {
        val typeId = type.toString()
        val partId = (type as? OpenApiSpecificationType.SpecificationExtension)?.partSchemaId

        return SerializedSpecificationType(typeId, partId)
    }

}

data class OpenApiUserDefinedState(
    val specTypeByUrl: Map<String, SerializedSpecificationType>? = null
)

data class SerializedSpecificationType(
    val typeId: String? = null,
    val partId: String? = null
)
