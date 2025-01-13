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

package com.explyt.spring.web.editor.openapi

import com.explyt.spring.web.util.SpringWebUtil.OPEN_API
import com.explyt.spring.web.util.SpringWebUtil.arrayTypes
import com.intellij.icons.AllIcons
import com.intellij.json.psi.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.childrenOfType
import com.jetbrains.rd.util.concurrentMapOf
import org.jetbrains.ide.BuiltInServerManager
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLSequence
import java.nio.file.Path
import java.util.*

object OpenApiUtils {
    private val cache = concurrentMapOf<UUID, String>()

    fun cacheFile(uuid: UUID, file: VirtualFile) {
        file.canonicalPath?.let {
            cache[uuid] = it
        }
    }

    fun getFile(uuid: UUID): VirtualFile? {
        return cache.getOrDefault(uuid, null)?.let {
            return VfsUtil.findFile(Path.of(it), true)
        }
    }

    fun isOpenApiFile(project: Project, file: VirtualFile): Boolean {
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return false

        return when (psiFile) {
            is YAMLFile -> isOpenApi(psiFile)
            is JsonFile -> isOpenApi(psiFile)
            else -> false
        }
    }

    fun isOpenApi(yamlFile: YAMLFile): Boolean {
        return YAMLUtil
            .getTopLevelKeys(yamlFile)
            .any { it.keyText == OPEN_API }
    }

    fun isOpenApi(jsonFile: JsonFile): Boolean {
        return jsonFile
            .topLevelValue
            ?.childrenOfType<JsonProperty>()
            ?.any { it.name == OPEN_API }
            ?: false
    }

    fun resourceUrl(specKey: UUID): String =
        "http://localhost:${BuiltInServerManager.getInstance().port}/${EXPLYT_OPENAPI}?key=${specKey}&resource"

    fun resourceUrl(specKey: UUID, resource: String): String =
        "${resourceUrl(specKey)}=$resource"

    fun proxyUrl(): String =
        "http://localhost:${BuiltInServerManager.getInstance().port}$OPENAPI_INTERNAL_CORS"

    fun createPreviewAction(tag: String, operationId: String): AnAction {
        return object : AnAction({ "Open in UI" }, AllIcons.RunConfigurations.TestState.Run) {
            override fun actionPerformed(e: AnActionEvent) {
                val editor = PlatformDataKeys.FILE_EDITOR.getData(e.dataContext)
                    ?.let { OpenApiUIEditor.from(it) } ?: return
                editor.showPreviewFor(tag, operationId)
            }

        }
    }

    fun getTagAndOperationIdFor(psiElement: PsiElement): TagAndOperationId? {
        return when (psiElement) {
            is JsonProperty -> getTagAndOperationIdFor(psiElement)
            is YAMLKeyValue -> getTagAndOperationIdFor(psiElement)
            else -> null
        }

    }

    fun isAbsolutePath(path: String) = ABSOLUTE_PATH_REGEX.matches(path)

    fun unwrapType(typeCanonical: String): ComponentSchemaInfo {
        val typeSplit = typeCanonical.split("<", ">")
        if (typeSplit.size == 3 && typeSplit.last()
                .isBlank()
        ) {
            val itemTypeCanonical = typeSplit[1]

            return ComponentSchemaInfo(itemTypeCanonical, typeSplit.first() in arrayTypes)
        }

        return ComponentSchemaInfo(typeCanonical, false)
    }

    private fun getTagAndOperationIdFor(jsonProperty: JsonProperty): TagAndOperationId? {
        val properties = (jsonProperty.value as? JsonObject)?.propertyList ?: return null

        val tag = properties.asSequence()
            .filter { it.name == "tags" }
            .mapNotNull { it.value as? JsonArray }
            .mapNotNull { it.valueList.firstOrNull() }
            .mapNotNull { it as? JsonStringLiteral }
            .map { it.unquotedValue() }
            .firstOrNull() ?: return null

        val operationId = properties.asSequence()
            .filter { it.name == "operationId" }
            .mapNotNull { it.value as? JsonStringLiteral }
            .map { it.unquotedValue() }
            .firstOrNull() ?: return null

        return TagAndOperationId(tag, operationId)
    }

    private fun JsonStringLiteral.unquotedValue(): String {
        return if (isQuotedString) JsonPsiUtil.stripQuotes(value) else value
    }

    private fun getTagAndOperationIdFor(yamlKeyValue: YAMLKeyValue): TagAndOperationId? {
        val yamlMapping = yamlKeyValue.value as? YAMLMapping ?: return null

        val tag = (yamlMapping.getKeyValueByKey("tags")?.value as? YAMLSequence)
            ?.items
            ?.firstOrNull()
            ?.value
            ?.text ?: return null

        val operationId = (yamlMapping.getKeyValueByKey("operationId"))
            ?.value
            ?.text ?: return null

        return TagAndOperationId(tag, operationId)
    }

    data class TagAndOperationId(
        val tag: String,
        val operationId: String
    )

    data class ComponentSchemaInfo(val typeQN: String, val isCollection: Boolean) {

        companion object {
            fun of(typeCanonical: String) = unwrapType(typeCanonical)
        }

    }

    const val EXPLYT_OPENAPI = "explyt-openapi"
    const val OPENAPI_INTERNAL_CORS = "/${EXPLYT_OPENAPI}_internal-cors"
    const val OPENAPI_ORIGINAL_URL = "${EXPLYT_OPENAPI}_original-url"
    const val OPENAPI_AUTH_HEADER = "${EXPLYT_OPENAPI}_auth-header"
    const val OPENAPI_EDITOR_TYPE_ID = "explyt.web.openapi.ui.editor"

    val headersToExclude = setOf(
        "host",
        "connection",
        "content-length",
        "accept-encoding",
        "origin",
        OPENAPI_AUTH_HEADER
    )

    private val ABSOLUTE_PATH_REGEX = Regex("^/?https?:.*")
}
