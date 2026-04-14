/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.jsonSchema

import com.explyt.spring.web.util.SpringWebUtil.OPENAPI_COMPONENTS
import com.explyt.spring.web.util.SpringWebUtil.OPENAPI_PARAMETERS
import com.explyt.spring.web.util.SpringWebUtil.OPENAPI_RESPONSES
import com.explyt.spring.web.util.SpringWebUtil.OPENAPI_SCHEMAS
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.jsonSchema.extension.JsonSchemaGotoDeclarationSuppressor
import org.jetbrains.yaml.psi.YAMLKeyValue

class OpenApiYamlGotoDeclarationSuppressor : JsonSchemaGotoDeclarationSuppressor {

    override fun shouldSuppressGtd(psiElement: PsiElement?): Boolean {
        val keyElement = psiElement as? LeafPsiElement ?: return false
        val parentElement = keyElement.parent as? YAMLKeyValue ?: return false
        val grandParentElement = parentKeyValue(parentElement) ?: return false
        val grandGrandParentElement = parentKeyValue(grandParentElement) ?: return false

        return grandGrandParentElement.name == OPENAPI_COMPONENTS && grandParentElement.name in setOf(
            OPENAPI_RESPONSES, OPENAPI_SCHEMAS, OPENAPI_PARAMETERS
        )
    }

    private fun parentKeyValue(kvElement: YAMLKeyValue): YAMLKeyValue? {
        return kvElement.parent.parent as? YAMLKeyValue
    }

}