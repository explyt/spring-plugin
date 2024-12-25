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
import com.explyt.spring.web.providers.EndpointUsageSearcher.findOpenApiJsonEndpoints
import com.explyt.spring.web.providers.EndpointUsageSearcher.findOpenApiJsonFiles
import com.explyt.spring.web.providers.EndpointUsageSearcher.findOpenApiYamlEndpoints
import com.explyt.spring.web.providers.EndpointUsageSearcher.findOpenApiYamlFiles
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.SourcesUtils
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
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
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping


class AddEndpointToOpenApiIntention(private val endpoint: EndpointInfo) : BaseIntentionAction() {

    override fun getFamilyName(): String {
        return "Create endpoint description"
    }

    override fun getText() = SpringWebBundle.message("explyt.openapi.gutter.method")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        val psiElement = endpoint.psiElement
        if (!psiElement.isValid) return false

        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return false
        return findOpenApiYamlEndpoints(endpoint.path, endpoint.requestMethods, module).isEmpty() &&
                findOpenApiJsonEndpoints(endpoint.path, endpoint.requestMethods, module).isEmpty()
    }

    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
        return IntentionPreviewInfo.EMPTY
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val module = ModuleUtilCore.findModuleForPsiElement(endpoint.psiElement) ?: return

        ReadAction.nonBlocking<List<PsiFile>> {
            findOpenApiYamlFiles(module) + findOpenApiJsonFiles(module)
        }
            .finishOnUiThread(ModalityState.current()) { openApiFiles ->
                proceedFiles(openApiFiles, module)
            }
            .inSmartMode(project)
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun proceedFiles(openApiFiles: List<PsiFile>, module: Module) {
        StatisticService.getInstance().addActionUsage(GUTTER_OPENAPI_GENERATE_ENDPOINT)

        val project = module.project

        ApplicationManager.getApplication().invokeLater {
            if (openApiFiles.isEmpty()) {
                val defaultFileType = YAMLFileType.YML

                val resourceRoot = SourcesUtils.getResourceRoots(module, false).firstOrNull()?.toPsiDirectory(project)

                if (resourceRoot != null) {
                    val openApiFile = OpenApiBuilderFactory.getOpenApiFileBuilder(defaultFileType)
                        .addEndpoint(endpoint)
                        .toFile("openapi", project)

                    WriteCommandAction.writeCommandAction(project).run<RuntimeException> {
                        val staticDir =
                            resourceRoot.findSubdirectory("static") ?: resourceRoot.createSubdirectory("static")
                        navigate(staticDir.add(openApiFile))
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

    private fun addDescription(endpoint: EndpointInfo, openApiFile: JsonFile, project: Project) {
        val topLevelValue = openApiFile.topLevelValue as? JsonObject ?: return

        val parentObject = topLevelValue.findProperty("paths")?.value as? JsonObject ?: topLevelValue

        WriteCommandAction.writeCommandAction(project).run<RuntimeException> {
            val generator = JsonElementGenerator(project)

            val propertyElement = if (parentObject == topLevelValue) {
                val pathsElement = OpenApiJsonPathBuilder(endpoint.path)
                    .addEndpoint(endpoint)
                    .toJsonValue("paths", project)
                addPropertyToObject(pathsElement, parentObject, generator)
            } else { //parentObject = "paths"
                val endpointPath = endpoint.path.ifBlank { "/" }
                val builder = StringBuilder()

                builder.iterateWithComma(endpoint.requestMethods) { httpType ->
                    OpenApiJsonPathHttpTypeBuilder(endpoint, httpType, "", builder)
                        .build()
                }

                val elementGenerator = JsonElementGenerator(project)
                val newElement = elementGenerator.createProperty(endpointPath, "{${builder}}")
                addPropertyToObject(newElement, parentObject, generator)
            }
            navigate(propertyElement)
        }
    }

    private fun addDescription(endpoint: EndpointInfo, openApiFile: YAMLFile, project: Project) {
        val topLevelValue = openApiFile.documents.firstOrNull()?.topLevelValue as? YAMLMapping ?: return
        val parentKeyValue = topLevelValue.getKeyValueByKey("paths")?.value ?: topLevelValue
        val parentMapping = parentKeyValue as? YAMLMapping ?: return

        WriteCommandAction.writeCommandAction(project).run<RuntimeException> {
            if (parentKeyValue == topLevelValue) {
                val pathsElement = OpenApiYamlPathBuilder(endpoint.path)
                    .addEndpoint(endpoint)
                    .toYamlKeyValue("paths", project)
                parentMapping.putKeyValue(pathsElement)
            } else { //parentKeyValue = "paths"
                val endpointPath = endpoint.path.ifBlank { "/" }
                val builder = StringBuilder()

                for (httpType in endpoint.requestMethods) {
                    OpenApiYamlPathHttpTypeBuilder(endpoint, httpType, "", builder)
                        .build()
                }

                val keyValueGenerator = YAMLElementGenerator.getInstance(project)
                val valueString = builder.toString().removePrefix("\n")
                val pathElement = keyValueGenerator.createYamlKeyValue(endpointPath, valueString)
                parentMapping.putKeyValue(pathElement)
            }
            navigate(parentMapping.children.lastOrNull())
        }
    }

    private fun navigate(psiElement: PsiElement?) {
        val navigatable = psiElement as? Navigatable ?: return

        ApplicationManager.getApplication().invokeLater {
            navigatable.navigate(false)
        }
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