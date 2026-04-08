/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.providers

import com.explyt.spring.core.properties.references.ExplytPropertyReference
import com.explyt.spring.core.util.SpringCoreUtil.removeDummyIdentifier
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReference.EMPTY_ARRAY
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.getParentOfType

class ValueConfigurationPropertyReferenceProvider : UastInjectionHostReferenceProvider() {

    companion object {
        val PROPERTIES_PATTERN = """\$\{([.A-z\d_-]+)(:[^{]*)?\s*\}""".toPattern()
    }

    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        uExpression.getParentOfType<UAnnotation>() ?: return EMPTY_ARRAY

        val valueText = uExpression.evaluateString()?.removeDummyIdentifier() ?: return EMPTY_ARRAY
        val referenceProperties = extractReferenceProperty(valueText)

        if (referenceProperties.isEmpty() && valueText.startsWith("\${")) {
            val startPosition =
                if (uExpression.lang == KotlinLanguage.INSTANCE) 4 else 3
            return arrayOf(
                ExplytPropertyReference(
                    host, "", TextRange.from(startPosition, 0)
                )
            )
        }

        return referenceProperties
            .map { referenceProperty ->
                val text = host.text.removeDummyIdentifier()
                val startOffset = text.indexOf(referenceProperty.key)

                ExplytPropertyReference(
                    host, referenceProperty.key,
                    TextRange.from(startOffset, referenceProperty.key.length)
                )
            }.toTypedArray()
    }

    private fun extractReferenceProperty(text: String): List<ReferenceProperty> {
        val matcher = PROPERTIES_PATTERN.matcher(text)
        val properties = mutableListOf<ReferenceProperty>()
        while (matcher.find()) {
            val propertyKey = matcher.group(1)

            val keyValue = ReferenceProperty(
                key = propertyKey,
                textRange = TextRange(
                    matcher.start(1),
                    matcher.end(1)
                )
            )
            properties.add(keyValue)
        }
        return properties
    }
}

data class ReferenceProperty(
    val key: String,
    val textRange: TextRange,
)

