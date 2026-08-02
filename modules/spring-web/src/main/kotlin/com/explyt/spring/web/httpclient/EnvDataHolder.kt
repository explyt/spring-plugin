/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.httpclient

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import java.nio.file.Path
import java.util.concurrent.Callable
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
        // The environment list is loaded asynchronously, so the persisted environment can only be restored
        // once it is known; passing it along keeps the previous end state of this method.
        selectFile(project, fileState.selectedEnv)
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

    fun selectFile(project: Project) = selectFile(project, envToRestore = "")

    private fun selectFile(project: Project, envToRestore: String) {
        val httpFileState = HttpFileStateService.getInstance().getOrCreateState(file)
        val selectedFileName = envFiles.selected ?: ""
        httpFileState.selectedFileName = selectedFileName

        val envFile = selectedFileName.takeIf { it.isNotEmpty() }
            ?.let { httpFileState.filesPathByName[it] }
            ?.let { VfsUtil.findFile(Path.of(it), false) }
        if (envFile == null) {
            envModel.removeAll()
            envModel.selectedItem = ""
            envFileIsJson.set(false)
            return
        }

        // Reading the environment file resolves it to JSON PSI, which is too slow for EDT: this method is called
        // both from panel initialization and from the environment-file combo listener.
        ReadAction.nonBlocking<List<String>?>(Callable { readEnvList(envFile, project) })
            .expireWhen { project.isDisposed }
            .coalesceBy(this)
            .finishOnUiThread(ModalityState.nonModal()) { envList ->
                applyEnvList(envList, selectedFileName, envToRestore)
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun applyEnvList(envList: List<String>?, fileName: String, envToRestore: String) {
        // A newer file may have been selected while this one was being parsed.
        if ((envFiles.selected ?: "") != fileName) return

        envFileIsJson.set(envList != null)
        envModel.removeAll()
        envModel.add("")
        envList.orEmpty().forEach { envModel.add(it) }
        envModel.selectedItem = envToRestore.takeIf { it in envModel.items } ?: envModel.items[0]
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

    /** Must be called under a read action. */
    private fun readEnvList(file: VirtualFile, project: Project): List<String>? {
        if (!file.isValid) return null
        val psiJsonFile = file.toPsiFile(project) as? JsonFile ?: return null
        return psiJsonFile.children.filterIsInstance<JsonObject>()
            .flatMap { jsonObject ->
                ProgressManager.checkCanceled()
                jsonObject.propertyList.map { it.name }
            }
    }

    private fun isExist(file: Path) = HttpFileStateService.getInstance().getOrCreateState(this.file).filesPathByName
        .values.any { it == file.absolutePathString() }
}