/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.providers

import com.explyt.spring.core.completion.doker.isDockerEnvCandidate
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLScalar

class DockerEnvVariableYamlReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (!isDockerEnvCandidate(element)) return emptyArray()

        val yamlScalar = element as? YAMLScalar ?: return PsiReference.EMPTY_ARRAY
        val envValueText = yamlScalar.text?.substringBefore("=") ?: return emptyArray()
        val propertyKey = envValueText.replace("_", ".").lowercase()
        val module = ModuleUtilCore.findModuleForPsiElement(yamlScalar) ?: return emptyArray()
        val textRange = TextRange(0, envValueText.length)
        val reference = ConfigurationPropertyKeyReference(yamlScalar, module, propertyKey, textRange)
        return arrayOf(reference)
    }
}