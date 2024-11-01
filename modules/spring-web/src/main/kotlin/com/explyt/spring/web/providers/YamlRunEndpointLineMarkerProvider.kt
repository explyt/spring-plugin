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

package com.explyt.spring.web.providers

import com.explyt.spring.web.editor.openapi.OpenApiUtils
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl

class YamlRunEndpointLineMarkerProvider : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        if (element !is LeafPsiElement) return null
        if (!SpringWebUtil.isSpringWebProject(element.project)) return null
        val yamlKeyValue = element.parent as? YAMLKeyValue ?: return null
        if (yamlKeyValue.key != element) return null
        val yamlFile = yamlKeyValue.containingFile as? YAMLFile ?: return null
        if (!OpenApiUtils.isOpenApi(yamlFile)) return null

        if (yamlKeyValue.keyText !in SpringWebUtil.REQUEST_METHODS) return null
        val path = YAMLUtil.getConfigFullName(yamlKeyValue).split('.')

        if (path.size != 3) return null
        if (path[0] != "paths") return null

        val firstTag = ((YAMLUtil.getQualifiedKeyInFile(
            yamlFile,
            path + "tags"
        )
            ?.value as? YAMLSequence)
            ?.items
            ?.firstOrNull()?.value as? YAMLPlainTextImpl)
            ?.text ?: return null
        val operationId = (YAMLUtil.getQualifiedKeyInFile(
            yamlFile, path + "operationId"
        )
            ?.value as? YAMLPlainTextImpl)
            ?.text ?: return null

        return Info(
            OpenApiUtils.createPreviewAction(firstTag, operationId)
        )
    }

}