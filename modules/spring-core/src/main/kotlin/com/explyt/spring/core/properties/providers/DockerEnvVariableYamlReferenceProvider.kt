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