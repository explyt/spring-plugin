/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.childrenOfType
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping

object YamlUtil {
    fun getAllProperties(document: YAMLDocument): Set<String> = document.findPropertiesFromDocument().keys

    private fun YAMLDocument.findPropertiesFromDocument(): Map<String, YAMLKeyValue> {
        val yamlKeyValues = mutableListOf<YAMLKeyValue>()
        PsiTreeUtil.processElements(this, YAMLKeyValue::class.java) { keyValue ->
            if (keyValue.value != null) {
                yamlKeyValues.add(keyValue)
            }
            true
        }
        return yamlKeyValues.associateBy { keyValue ->
            // We filtered out key values with `null` values before, so it can be cast to non-null type
            YAMLUtil.getConfigFullName(keyValue.value!!)
        }
    }

    fun createYamlMapping(project: Project, text: String): YAMLMapping {
        return YAMLElementGenerator.getInstance(project)
            .createDummyYamlWithText(text)
            .documents
            .first()
            .childrenOfType<YAMLMapping>()
            .first()
    }
}