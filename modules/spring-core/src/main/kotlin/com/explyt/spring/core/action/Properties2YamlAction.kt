/*
 * Copyright © 2025 Explyt Ltd
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

package com.explyt.spring.core.action

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.util.ActionUtil
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.intellij.lang.properties.PropertiesFileType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.yaml.YAMLFileType
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import kotlin.io.path.name


class Properties2YamlAction : AnAction(SpringCoreBundle.message("explyt.spring.properties.action.yaml")) {
    override fun update(e: AnActionEvent) {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (virtualFiles == null || virtualFiles.size > 1) {
            ActionUtil.isEnabledAndVisible(e, false)
            return
        }
        e.presentation.isEnabledAndVisible = virtualFiles.any { it.fileType == PropertiesFileType.INSTANCE }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val propertiesVirtualFile = virtualFiles.firstOrNull { it.fileType == PropertiesFileType.INSTANCE } ?: return

        val documentManager = PsiDocumentManager.getInstance(project)
        val psiFile = propertiesVirtualFile.toPsiFile(project) ?: return
        val document = documentManager.getDocument(psiFile) ?: return
        PsiDocumentManager.getInstance(project).commitDocument(document)
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)

        val properties = Properties()
        val nioPropPath = propertiesVirtualFile.toNioPath()
        FileInputStream(nioPropPath.toFile()).use { properties.load(it) }

        val toNestedMap = toNestedMap(properties)
        val name = nioPropPath.name.substringBeforeLast(".")
        val nioYamlPath = nioPropPath.parent.resolve("$name-converted-explyt.yml")


        val yamlFactory = YAMLFactory()
        yamlFactory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
        yamlFactory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)

        val mapper = ObjectMapper(yamlFactory)
        mapper.writeValue(nioYamlPath.toFile(), toNestedMap)
        VfsUtil.findFile(nioYamlPath, true)?.toPsiFile(project)?.navigate(true)
    }

    private fun toNestedMap(properties: Properties): Map<String, Any> {
        val resultMap = mutableMapOf<String, Any>()

        for (key in properties.stringPropertyNames()) {
            val value = properties.getProperty(key, "")
            val parts = key.split(".")
            if (parts.isEmpty()) continue
            var currentMap = resultMap
            if (parts.size > 1) {
                for ((index, part) in parts.withIndex()) {
                    if (index == parts.size - 1) continue

                    if (!currentMap.containsKey(part)) {
                        currentMap[part] = mutableMapOf<String, Any>()
                    }
                    currentMap = (currentMap[part] as? MutableMap<String, Any>) ?: continue

                }
            }
            currentMap[parts.last()] = getValue(value)
        }
        return resultMap
    }

    private fun getValue(value: String): Any {
        if (value.isEmpty()) return value
        val intOrNull = value.toIntOrNull()
        if (intOrNull != null) {
            return intOrNull
        }
        val booleanOrNull = value.lowercase().toBooleanStrictOrNull()
        if (booleanOrNull != null) {
            return booleanOrNull
        }
        return value
    }
}


class Yaml2PropertiesAction : AnAction(SpringCoreBundle.message("explyt.spring.properties.action.prop")) {
    override fun update(e: AnActionEvent) {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (virtualFiles == null || virtualFiles.size > 1) {
            ActionUtil.isEnabledAndVisible(e, false)
            return
        }
        e.presentation.isEnabledAndVisible = virtualFiles.any { it.fileType == YAMLFileType.YML }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val propertiesVirtualFile = virtualFiles.firstOrNull { it.fileType == YAMLFileType.YML } ?: return
        val nioYmlPropPath = propertiesVirtualFile.toNioPath()

        val yamlFactory = YAMLFactory()
        val mapper = ObjectMapper(yamlFactory)
        val referenceType = object : TypeReference<LinkedHashMap<String, Any?>>() {}
        val yamlMap = mapper.readValue(nioYmlPropPath.toFile(), referenceType) ?: return

        val flattenedMap = LinkedHashMap<String, Any?>()
        toFlattenMap(yamlMap, "", flattenedMap)

        val properties = Properties()
        flattenedMap.forEach { (key, value) -> properties.setProperty(key, value as? String ?: "") }

        val name = nioYmlPropPath.name.substringBeforeLast(".")
        val nioPropPath = nioYmlPropPath.parent.resolve("$name-converted-explyt.properties")
        FileOutputStream(nioPropPath.toFile()).use { properties.store(it, null) }

        val virtualFile = VfsUtil.findFile(nioPropPath, true)
        virtualFile?.toPsiFile(project)?.navigate(true)
    }

    private fun toFlattenMap(nestedMap: Map<String, Any?>, prefix: String, result: LinkedHashMap<String, Any?>) {
        nestedMap.forEach { (key: Any?, value: Any?) ->
            val fullKey = (if (prefix.isEmpty()) key else "$prefix.$key") as String
            if (value is Map<*, *>) {
                toFlattenMap(value as Map<String, Any?>, fullKey, result)
            } else {
                result[fullKey] = value.toString()
            }
        }
    }
}
