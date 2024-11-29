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

package com.explyt.spring.web.jsonSchema

import com.explyt.spring.web.util.SpringWebUtil.OPENAPI_COMPONENTS
import com.explyt.spring.web.util.SpringWebUtil.OPENAPI_PARAMETERS
import com.explyt.spring.web.util.SpringWebUtil.OPENAPI_RESPONSES
import com.explyt.spring.web.util.SpringWebUtil.OPENAPI_SCHEMAS
import com.intellij.json.psi.JsonProperty
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.jsonSchema.extension.JsonSchemaGotoDeclarationSuppressor

class OpenApiJsonGotoDeclarationSuppressor : JsonSchemaGotoDeclarationSuppressor {

    override fun shouldSuppressGtd(psiElement: PsiElement?): Boolean {
        val leafElement = psiElement as? LeafPsiElement ?: return false
        val parentElement = parentProperty(leafElement) ?: return false
        val grandParentElement = parentProperty(parentElement) ?: return false
        val grandGrandParentElement = parentProperty(grandParentElement) ?: return false

        return grandGrandParentElement.name == OPENAPI_COMPONENTS && grandParentElement.name in setOf(
            OPENAPI_RESPONSES, OPENAPI_SCHEMAS, OPENAPI_PARAMETERS
        )
    }

    private fun parentProperty(psiElement: PsiElement): JsonProperty? {
        return psiElement.parent.parent as? JsonProperty
    }

}