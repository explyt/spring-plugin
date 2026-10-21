/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections.quickfix

import com.explyt.spring.core.inspections.utils.ExplytJsonUtil.addPropertyToObject
import com.explyt.spring.core.inspections.utils.ExplytJsonUtil.iterateWithComma
import com.explyt.spring.core.statistic.StatisticActionId.GUTTER_OPENAPI_GENERATE_ENDPOINT
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.builder.openapi.OpenApiBuilderFactory
import com.explyt.spring.web.builder.openapi.json.OpenApiJsonPathBuilder
import com.explyt.spring.web.builder.openapi.json.OpenApiJsonPathHttpTypeBuilder
import com.explyt.spring.web.builder.openapi.yaml.OpenApiYamlPathBuilder
import com.explyt.spring.web.builder.openapi.yaml.OpenApiYamlPathHttpTypeBuilder
import com.explyt.spring.web.loader.EndpointVerbs
import com.explyt.spring.web.providers.EndpointUsageSearcher.findOpenApiJsonData
import com.explyt.spring.web.providers.EndpointUsageSearcher.findOpenApiYamlData
import com.explyt.spring.web.providers.EndpointUsageSearcher.findUndocumentedOpenApiMethods
import com.explyt.spring.web.providers.EndpointUsageSearcher.openApiMethodsOf
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.findOrCreateDirectory
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.impl.YAMLHashImpl


class AddEndpointToOpenApiIntention(private val endpoint: EndpointInfo) : BaseIntentionAction() {

    override fun getFamilyName(): String {
        return "Create endpoint description"
    }

    override fun getText() = SpringWebBundle.message("explyt.openapi.gutter.method")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        val psiElement = endpoint.psiElement
        if (!psiElement.isValid) return false

        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return false
        return findUndocumentedOpenApiMethods(endpoint.path, endpoint.requestMethods, module).isNotEmpty()
    }

    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
        return IntentionPreviewInfo.EMPTY
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val module = ModuleUtilCore.findModuleForPsiElement(endpoint.psiElement) ?: return

        ReadAction.nonBlocking<Pair<List<PsiFile>, EndpointInfo>> {
            val openApiFiles = (findOpenApiYamlData(project) + findOpenApiJsonData(project))
                .map { it.psiFile }
                .filter { ModuleUtilCore.findModuleForFile(it) == module }
            openApiFiles to endpoint.copy(requestMethods = openApiMethodsOf(endpoint.path, endpoint.requestMethods, module))
        }
            .finishOnUiThread(ModalityState.current()) { (openApiFiles, documentedEndpoint) ->
                proceedFiles(openApiFiles, documentedEndpoint, module)
            }
            .inSmartMode(project)
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun proceedFiles(openApiFiles: List<PsiFile>, endpoint: EndpointInfo, module: Module) {
        StatisticService.getInstance().addActionUsage(GUTTER_OPENAPI_GENERATE_ENDPOINT)

        val project = module.project

        ApplicationManager.getApplication().invokeLater {
            if (openApiFiles.isEmpty()) {
                val defaultFileType = YAMLFileType.YML

                val rootManager = ModuleRootManager.getInstance(module)
                val contentRoot = rootManager.contentRoots.firstOrNull() ?: return@invokeLater

                val openApiFile = OpenApiBuilderFactory.getOpenApiFileBuilder(defaultFileType)
                    .addEndpoint(endpoint)
                    .toFile("openapi", project)

                WriteCommandAction.writeCommandAction(project).run<RuntimeException> {
                    val resourcesPath = "${contentRoot.path}/src/main/resources"
                    val resourcesDir = VfsUtil.createDirectoryIfMissing(resourcesPath) ?: return@run

                    val staticDir = resourcesDir.findOrCreateDirectory("static")
                    staticDir.toPsiDirectory(project)?.let { psiDirectory ->
                        ExplytPsiUtil.navigate(psiDirectory.add(openApiFile))
                    }
                }
            } else if (openApiFiles.size == 1) {
                addDescription(endpoint, openApiFiles.first(), project)
            } else {
                chooseFile(openApiFiles, project)?.let { openApiFile ->
                    addDescription(endpoint, openApiFile, project)
                }
            }
        }
    }

    private fun chooseFile(
        openApiFiles: List<PsiFile>,
        project: Project
    ): PsiFile? {
        val virtualFiles = openApiFiles.map { it.virtualFile }
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
        descriptor.roots = virtualFiles

        val openApiVf = FileChooser.chooseFile(descriptor, project, null)
        if (openApiVf != null) {
            return openApiFiles.firstOrNull {
                it.virtualFile == openApiVf
            }
        }
        return null
    }

    private fun addDescription(endpoint: EndpointInfo, openApiFile: PsiFile, project: Project) {
        (openApiFile as? JsonFile)?.let { jsonFile ->
            addDescription(endpoint, jsonFile, project)
        }

        (openApiFile as? YAMLFile)?.let { yamlFile ->
            addDescription(endpoint, yamlFile, project)
        }
    }

    private fun addDescription(endpointInfo: EndpointInfo, openApiFile: JsonFile, project: Project) {
        WriteCommandAction.writeCommandAction(project).run<RuntimeException> {
            commitDocumentOf(openApiFile)
            val topLevelValue = openApiFile.topLevelValue as? JsonObject ?: return@run
            val pathsProperty = topLevelValue.findProperty("paths")
            if (pathsProperty != null && pathsProperty.value !is JsonObject) {
                navigateLater(pathsProperty)
                return@run
            }

            val parentObject = pathsProperty?.value as? JsonObject ?: topLevelValue
            val documentedItem = parentObject.documentedPathItemProperty(endpointInfo.path)
            val endpoint = documentedItem?.let { endpointInfo.withPathVariablesOf(it.name) } ?: endpointInfo
            val documentedOperations = documentedItem?.value as? JsonObject
            val writtenMethods = EndpointVerbs.openApiMethodsOf(endpoint.requestMethods)
                .filter { documentedOperations?.findProperty(it.lowercase()) == null }
            val generator = JsonElementGenerator(project)

            if (parentObject == topLevelValue) {
                val pathsElement = OpenApiJsonPathBuilder(endpoint.path)
                    .addEndpoint(endpoint)
                    .toJsonValue("paths", project)
                addPropertyToObject(pathsElement, parentObject, generator)
            } else { //parentObject = "paths"
                val endpointPath = endpoint.path.ifBlank { "/" }
                val builder = StringBuilder()

                builder.iterateWithComma(EndpointVerbs.openApiMethodsOf(endpoint.requestMethods)) { httpType ->
                    OpenApiJsonPathHttpTypeBuilder(endpoint, httpType, "", builder)
                        .build()
                }

                val elementGenerator = JsonElementGenerator(project)
                val newElement = elementGenerator.createProperty(endpointPath, "{${builder}}")
                parentObject.putPathItem(newElement, generator)
            }
            navigateLater(writtenJsonOperation(openApiFile, endpoint.path, writtenMethods))
        }
    }

    private fun JsonObject.putPathItem(pathItem: JsonProperty, generator: JsonElementGenerator) {
        val documentedItem = documentedPathItemProperty(pathItem.name)?.value as? JsonObject
        val operations = pathItem.value as? JsonObject
        if (documentedItem == null || operations == null) {
            addPropertyToObject(pathItem, this, generator)
            return
        }

        operations.propertyList
            .filter { documentedItem.findProperty(it.name) == null }
            .forEach { addPropertyToObject(it, documentedItem, generator) }
    }

    private fun JsonObject.documentedPathItemProperty(path: String): JsonProperty? =
        propertyList.firstOrNull { it.value is JsonObject && SpringWebUtil.isSamePathTemplate(it.name, path) }

    private fun writtenJsonOperation(openApiFile: JsonFile, path: String, writtenMethods: List<String>): PsiElement? {
        val paths = (openApiFile.topLevelValue as? JsonObject)?.findProperty("paths")?.value as? JsonObject
            ?: return null
        val pathItem = paths.documentedPathItemProperty(path) ?: return null
        val operation = writtenMethods.firstNotNullOfOrNull {
            (pathItem.value as? JsonObject)?.findProperty(it.lowercase())
        }
        return operation ?: pathItem
    }

    private fun addDescription(endpointInfo: EndpointInfo, openApiFile: YAMLFile, project: Project) {
        WriteCommandAction.writeCommandAction(project).run<RuntimeException> {
            commitDocumentOf(openApiFile)
            val topLevelValue = openApiFile.documents.firstOrNull()?.topLevelValue as? YAMLMapping ?: return@run
            val parentKeyValue = topLevelValue.getKeyValueByKey("paths")?.value ?: topLevelValue
            val parentMapping = parentKeyValue as? YAMLMapping ?: return@run
            val flowStyleTarget = flowStyleTargetOf(parentMapping, endpointInfo.path)
            if (flowStyleTarget != null) {
                navigateLater(flowStyleTarget)
                return@run
            }

            val documentedItem = parentMapping.documentedPathItemKeyValue(endpointInfo.path)
            val endpoint = documentedItem?.let { endpointInfo.withPathVariablesOf(it.keyText) } ?: endpointInfo
            val documentedOperations = documentedItem?.value as? YAMLMapping
            val writtenMethods = EndpointVerbs.openApiMethodsOf(endpoint.requestMethods)
                .filter { documentedOperations?.getKeyValueByKey(it.lowercase()) == null }
            if (parentKeyValue == topLevelValue) {
                val pathsElement = OpenApiYamlPathBuilder(endpoint.path)
                    .addEndpoint(endpoint)
                    .toYamlKeyValue("paths", project)
                parentMapping.putKeyValue(pathsElement)
            } else { //parentKeyValue = "paths"
                val endpointPath = endpoint.path.ifBlank { "/" }
                val builder = StringBuilder()

                for (httpType in EndpointVerbs.openApiMethodsOf(endpoint.requestMethods)) {
                    OpenApiYamlPathHttpTypeBuilder(endpoint, httpType, "", builder)
                        .build()
                }

                val keyValueGenerator = YAMLElementGenerator.getInstance(project)
                val valueString = builder.toString().removePrefix("\n")
                val pathElement = keyValueGenerator.createYamlKeyValue(endpointPath, valueString)
                parentMapping.putPathItem(pathElement)
            }
            navigateLater(writtenYamlOperation(openApiFile, endpoint.path, writtenMethods))
        }
    }

    private fun flowStyleTargetOf(parentMapping: YAMLMapping, path: String): PsiElement? {
        if (parentMapping is YAMLHashImpl) return parentMapping
        return parentMapping.documentedPathItemKeyValue(path)?.takeIf { it.value is YAMLHashImpl }
    }

    private fun commitDocumentOf(file: PsiFile) {
        val documentManager = PsiDocumentManager.getInstance(file.project)
        documentManager.getDocument(file)?.let { documentManager.commitDocument(it) }
    }

    private fun YAMLMapping.putPathItem(pathItem: YAMLKeyValue) {
        val documentedItem = documentedPathItemKeyValue(pathItem.keyText)?.value as? YAMLMapping
        val operations = pathItem.value as? YAMLMapping
        if (documentedItem == null || operations == null) {
            putKeyValue(pathItem)
            return
        }

        val missingOperations = operations.keyValues.filter { documentedItem.getKeyValueByKey(it.keyText) == null }
        val lastKeyValue = documentedItem.keyValues.lastOrNull()
        if (lastKeyValue == null) {
            missingOperations.forEach { documentedItem.putKeyValue(it) }
            return
        }

        val indent = YAMLUtil.getIndentToThisElement(lastKeyValue)
        val inserted = missingOperations.joinToString("") { "\n" + " ".repeat(indent) + it.textIndentedTo(indent) }
        val file = documentedItem.containingFile
        val documentManager = PsiDocumentManager.getInstance(file.project)
        val document = documentManager.getDocument(file) ?: return
        documentManager.doPostponedOperationsAndUnblockDocument(document)
        document.insertString(lastKeyValue.textRange.endOffset, inserted)
        documentManager.commitDocument(document)
    }

    private fun YAMLKeyValue.textIndentedTo(indent: Int): String {
        val ownIndent = YAMLUtil.getIndentToThisElement(this)
        return text.lines().mapIndexed { index, line ->
            if (index == 0 || line.isBlank()) line else " ".repeat(indent) + line.drop(ownIndent)
        }.joinToString("\n")
    }

    private fun YAMLMapping.documentedPathItemKeyValue(path: String): YAMLKeyValue? =
        keyValues.firstOrNull { it.value is YAMLMapping && SpringWebUtil.isSamePathTemplate(it.keyText, path) }

    private fun writtenYamlOperation(openApiFile: YAMLFile, path: String, writtenMethods: List<String>): PsiElement? {
        val topLevelValue = openApiFile.documents.firstOrNull()?.topLevelValue as? YAMLMapping ?: return null
        val paths = topLevelValue.getKeyValueByKey("paths")?.value as? YAMLMapping ?: return null
        val pathItem = paths.documentedPathItemKeyValue(path) ?: return null
        val operation = writtenMethods.firstNotNullOfOrNull {
            (pathItem.value as? YAMLMapping)?.getKeyValueByKey(it.lowercase())
        }
        return operation ?: pathItem
    }

    private fun navigateLater(target: PsiElement?) {
        if (target == null || !target.isValid || IntentionPreviewUtils.isIntentionPreviewActive()) return
        val pointer = SmartPointerManager.createPointer(target)
        ApplicationManager.getApplication().invokeLater {
            (pointer.element?.takeIf { it.isValid } as? Navigatable)?.navigate(false)
        }
    }

    private fun EndpointInfo.withPathVariablesOf(documentedPath: String): EndpointInfo {
        val renames = SpringWebUtil.pathTemplateVariables(path)
            .zip(SpringWebUtil.pathTemplateVariables(documentedPath))
            .toMap()
        if (renames.all { (name, documentedName) -> name == documentedName }) return this
        return copy(pathVariables = pathVariables.map { variable ->
            renames[variable.name]?.let { variable.copy(name = it) } ?: variable
        })
    }

    data class EndpointInfo(
        val path: String,
        val requestMethods: List<String>,
        val psiElement: PsiElement,
        val methodName: String,
        val tag: String,
        val description: String,
        val returnTypeFqn: String,
        val pathVariables: Collection<SpringWebUtil.PathArgumentInfo> = emptyList(),
        val requestParameters: Collection<SpringWebUtil.PathArgumentInfo> = emptyList(),
        val requestBodyInfo: SpringWebUtil.PathArgumentInfo? = null,
        val requestHeaders: Collection<SpringWebUtil.PathArgumentInfo> = emptyList(),
        val produces: Collection<String> = emptyList(),
        val consumes: Collection<String> = emptyList(),
    )

}