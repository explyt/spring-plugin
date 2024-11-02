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