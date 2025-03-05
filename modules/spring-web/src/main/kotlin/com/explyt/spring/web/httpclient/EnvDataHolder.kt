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

package com.explyt.spring.web.httpclient

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CollectionComboBoxModel
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.name


class EnvDataHolder(
    val file: Path,
    val envFiles: CollectionComboBoxModel<String> = CollectionComboBoxModel(mutableListOf<String>()),
    val envModel: CollectionComboBoxModel<String> = CollectionComboBoxModel(mutableListOf<String>()),
    val envPanelVisible: AtomicBooleanProperty = AtomicBooleanProperty(false),
    val envFileIsJson: AtomicBooleanProperty = AtomicBooleanProperty(false),
    val additionalArgsBind: GraphProperty<String> = PropertyGraph().property("")
) {

    fun init(project: Project) {
        val fileState = HttpFileStateService.getInstance().getOrCreateState(file)
        val keysForRemove = fileState.filesPathByName.filter { !Path.of(it.value).exists() }.map { it.key }
        keysForRemove.forEach { fileState.filesPathByName.remove(it) }
        envPanelVisible.set(fileState.filesPathByName.isNotEmpty())
        envFiles.removeAll()
        envFiles.add("")
        fileState.filesPathByName.keys.forEach { envFiles.add(it) }
        envFiles.selectedItem = if (fileState.filesPathByName.contains(fileState.selectedFileName))
            fileState.selectedFileName else ""
        selectFile(project)
        envModel.selectedItem = fileState.selectedEnv
        additionalArgsBind.set(fileState.additionalArgs)
    }

    fun addFile(file: Path) {
        if (isExist(file)) return
        addFileToModel(file)
    }

    fun addFile(file: VirtualFile, project: Project?) {
        val nioPath = file.toNioPath()
        if (isExist(nioPath)) return
        val modelName = addFileToModel(nioPath)

        envFiles.selectedItem = modelName
        project ?: return
        selectFile(project)
    }

    fun removeFile(project: Project) {
        val currentFileName = envFiles.selected?.takeIf { it.isNotEmpty() } ?: return
        val httpFileState = HttpFileStateService.getInstance().getOrCreateState(file)
        httpFileState.filesPathByName.remove(currentFileName)
        httpFileState.selectedFileName = ""
        init(project)
    }

    fun selectFile(project: Project) {
        envModel.removeAll()
        envModel.selectedItem = ""
        envFileIsJson.set(false)
        val httpFileState = HttpFileStateService.getInstance().getOrCreateState(file)
        httpFileState.selectedFileName = envFiles.selected ?: ""
        val file = envFiles.selected?.let { httpFileState.filesPathByName[it] }
            ?.let { VfsUtil.findFile(Path.of(it), false) } ?: return

        val psiJsonFile = getPsiJsonFile(file, project)
        envFileIsJson.set(psiJsonFile != null)
        val obj = psiJsonFile?.children?.filterIsInstance<JsonObject>() ?: emptyList()
        val envList = obj.flatMap { it.propertyList }.map { it.name }
        envModel.removeAll()
        envModel.add("")
        envList.forEach { envModel.add(it) }
        envModel.selectedItem = envModel.items[0]
    }

    fun selectEnv() {
        HttpFileStateService.getInstance().getOrCreateState(file).selectedEnv = envModel.selected ?: ""
    }

    fun setArgs() {
        HttpFileStateService.getInstance().getOrCreateState(file).additionalArgs = additionalArgsBind.get()
    }

    fun getFilePath(): Path? {
        val filesPathByName = HttpFileStateService.getInstance().getOrCreateState(file).filesPathByName
        return envFiles.selected?.takeIf { it.isNotEmpty() }?.let { filesPathByName[it] }?.let { Path.of(it) }
    }

    private fun addFileToModel(file: Path): String {
        if (envFiles.isEmpty) envFiles.add("")
        val modelName = getModelName(file)
        envFiles.add(modelName)
        envPanelVisible.set(true)
        HttpFileStateService.getInstance().getOrCreateState(this.file).filesPathByName[modelName] =
            file.absolutePathString()
        return modelName
    }

    private fun getModelName(file: Path): String {
        val filesPathByName = HttpFileStateService.getInstance().getOrCreateState(this.file).filesPathByName
        var tmpFile = file
        var fileName = tmpFile.name
        while (filesPathByName.contains(fileName)) {
            tmpFile = tmpFile.parent
            fileName = tmpFile.name + "/" + fileName
        }
        return fileName
    }

    private fun getPsiJsonFile(file: VirtualFile, project: Project?): JsonFile? {
        return project?.let { file.toPsiFile(it) as? JsonFile }
    }

    private fun isExist(file: Path) = HttpFileStateService.getInstance().getOrCreateState(this.file).filesPathByName
        .values.any { it == file.absolutePathString() }
}