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
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsSchema
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.intellij.lang.properties.PropertiesFileType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.yaml.YAMLFileType
import java.nio.file.Path
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
        val document = e.getData(CommonDataKeys.EDITOR)?.document ?: return
        val propertiesVirtualFile = virtualFiles.firstOrNull { it.fileType == PropertiesFileType.INSTANCE } ?: return

        ApplicationManager.getApplication().runWriteAction {
            FileDocumentManager.getInstance().saveDocument(document)
        }

        val yamlMap = readProperty(propertiesVirtualFile.toNioPath())

        val nioPropPath = propertiesVirtualFile.toNioPath()
        val name = nioPropPath.name.substringBeforeLast(".")
        val nioYamlPath = nioPropPath.parent.resolve("$name-converted-explyt.yml")

        mapToYaml(nioYamlPath, yamlMap)
        val toPsiFile = VfsUtil.findFile(nioYamlPath, true)?.toPsiFile(project) ?: return
        toPsiFile.navigate(true)
        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(project) {
                CodeStyleManager.getInstance(project).reformat(toPsiFile)
            }
        }
    }

    companion object {
        @TestOnly
        fun readProperty(path: Path?, content: String? = null): LinkedHashMap<String, Any?> {
            val mapper = JavaPropsMapper()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false)
            val referenceType = object : TypeReference<LinkedHashMap<String, Any?>>() {}
            val map = path?.let { mapper.readValue(path.toFile(), referenceType) }
                ?: mapper.readValue(content, referenceType)
                ?: LinkedHashMap()
            return map
        }

        @TestOnly
        fun mapToYaml(nioYamlPath: Path? = null, yamlMap: LinkedHashMap<String, Any?>): String? {
            val yamlFactory = YAMLFactory()
            yamlFactory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            yamlFactory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)

            val mapper = ObjectMapper(yamlFactory)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false)
            if (nioYamlPath != null) {
                mapper.writeValue(nioYamlPath.toFile(), yamlMap)
                return null
            }
            return mapper.writeValueAsString(yamlMap)
        }
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
        val document = e.getData(CommonDataKeys.EDITOR)?.document ?: return
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val propertiesVirtualFile = virtualFiles.firstOrNull { it.fileType == YAMLFileType.YML } ?: return
        val nioYmlPropPath = propertiesVirtualFile.toNioPath()

        ApplicationManager.getApplication().runWriteAction {
            FileDocumentManager.getInstance().saveDocument(document)
        }

        val yamlMap = readYaml(nioYmlPropPath)

        val name = nioYmlPropPath.name.substringBeforeLast(".")
        val nioPropPath = nioYmlPropPath.parent.resolve("$name-converted-explyt.properties")

        mapToProperty(nioPropPath, yamlMap)

        VfsUtil.findFile(nioPropPath, true)?.toPsiFile(project)?.navigate(true)
    }

    companion object {
        @TestOnly
        fun readYaml(path: Path?, content: String? = null): LinkedHashMap<String, Any?> {
            val yamlFactory = YAMLFactory()
            val mapper = ObjectMapper(yamlFactory)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false)
            val referenceType = object : TypeReference<LinkedHashMap<String, Any?>>() {}
            val map = path?.let { mapper.readValue(path.toFile(), referenceType) }
                ?: mapper.readValue(content, referenceType)
                ?: LinkedHashMap()
            return map
        }

        @TestOnly
        fun mapToProperty(path: Path? = null, yamlMap: LinkedHashMap<String, Any?>): String? {
            val mapper = JavaPropsMapper()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false)
                .writer(
                    JavaPropsSchema.emptySchema()
                        .withWriteIndexUsingMarkers(true)
                        .withFirstArrayOffset(0)
                )
            if (path != null) {
                mapper.writeValue(path.toFile(), yamlMap)
                return null
            }
            return mapper.writeValueAsString(yamlMap)
        }
    }
}
