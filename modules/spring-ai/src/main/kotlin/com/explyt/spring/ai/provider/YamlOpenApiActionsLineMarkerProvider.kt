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

package com.explyt.spring.ai.provider

import com.explyt.linemarker.SpringMarkerInfo
import com.explyt.spring.ai.SpringAiBundle.message
import com.explyt.spring.ai.SpringAiIcons
import com.explyt.spring.ai.action.ConvertOpenapiToControllerAction
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.util.SpringWebUtil.OPEN_API
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.yaml.psi.YAMLKeyValue

class YamlOpenApiActionsLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element is LeafPsiElement) return null
        if (!SpringWebUtil.isSpringWebProject(element.project)) return null
        val yamlKeyValue = element as? YAMLKeyValue ?: return null
        val psiElement = yamlKeyValue.key ?: return null
        if (yamlKeyValue.keyText != SpringWebUtil.OPEN_API) return null

        val actionGroup = DefaultActionGroup()
        actionGroup.add(ConvertOpenapiToControllerAction())
        return SpringMarkerInfo(
            psiElement,
            SpringAiIcons.aiAssistant,
            { message("explyt.spring.ai.actions") },
            actionGroup
        )
    }
}

class JsonOpenApiActionsLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (!SpringWebUtil.isSpringWebProject(element.project)) return null
        val jsonProperty = element as? JsonProperty ?: return null
        if (jsonProperty.name != OPEN_API) return null

        val actionGroup = DefaultActionGroup()
        actionGroup.add(ConvertOpenapiToControllerAction())
        return SpringMarkerInfo(
            jsonProperty,
            SpringAiIcons.aiAssistant,
            { message("explyt.spring.ai.actions") },
            actionGroup
        )
    }
}