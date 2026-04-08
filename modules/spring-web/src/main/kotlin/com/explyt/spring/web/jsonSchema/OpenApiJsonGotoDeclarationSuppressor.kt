/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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