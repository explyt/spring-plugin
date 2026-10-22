/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.providers

import com.explyt.spring.core.SpringProperties.PLACEHOLDER_PREFIX
import com.explyt.spring.core.SpringProperties.SPEL_PREFIX
import com.explyt.spring.core.properties.references.ExplytPropertyReference
import com.explyt.spring.core.references.SpelBeanMemberReference
import com.explyt.spring.core.references.SpelBeanReference
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
        /**
         * The default value (everything after the first `:`) may itself contain one brace-delimited
         * expression: a SpEL default such as `:#{null}` or a nested placeholder such as
         * `:$\{fallback.key}`. Such a group must be consumed as a whole, otherwise the placeholder
         * key is not extracted at all, no property reference is created, and the property looks
         * unused - producing false "Cannot resolve key property" warnings in properties and YAML
         * files.
         *
         * Only one level of nesting is supported, which covers the defaults Spring accepts here.
         * The alternatives are mutually exclusive by their first character and quantified
         * possessively on purpose: this provider runs while computing references for editable
         * annotation text, and an ambiguous alternation backtracks exponentially on input such as
         * `$\{x:` followed by many `$\{a}` groups, which would freeze highlighting.
         */
        val PROPERTIES_PATTERN =
            """\$\{([.A-z\d_-]+)(:(?:[^{}$]|\$(?!\{)|\$\{[^{}]*+}|\{[^{}]*+})*+)?\s*+}""".toPattern()

        /**
         * A SpEL block in an annotation value. Group 1 is its body, so a bean reference is searched only inside
         * `#{...}` and never in surrounding literal text — an `@` elsewhere in the value is not a bean.
         *
         * Nested braces are not matched on purpose: a SpEL default inside a placeholder
         * (`$\{key:#{null}}`) still yields its inner block, and the possessive quantifier keeps the scan linear
         * while the user edits the annotation.
         */
        val SPEL_PATTERN = """#\{([^{}]*+)}""".toPattern()

        /**
         * A bean reference inside a SpEL block: `@beanName` and, optionally, the single property access that
         * follows it. Group 1 is the bean name, group 2 the member.
         *
         * Only the first access is captured — `#{@myProps.cron}` is the shape issue #44 is about. A longer chain
         * such as `#{@a.b.c}` needs the type of `b` to resolve `c`, which the flat name index cannot supply, and
         * offering a reference that never resolves is worse than offering none.
         */
        val SPEL_BEAN_PATTERN = """@(\w++)(?:\s*+\.\s*+(\w++))?""".toPattern()
    }

    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        uExpression.getParentOfType<UAnnotation>() ?: return EMPTY_ARRAY

        val valueText = uExpression.evaluateString()?.removeDummyIdentifier() ?: return EMPTY_ARRAY
        val referenceProperties = extractReferenceProperty(valueText)

        if (referenceProperties.isEmpty() && valueText.startsWith(PLACEHOLDER_PREFIX)) {
            val startPosition =
                if (uExpression.lang == KotlinLanguage.INSTANCE) 4 else 3
            return arrayOf(
                ExplytPropertyReference(
                    host, "", TextRange.from(startPosition, 0)
                )
            )
        }

        val propertyReferences = referenceProperties
            .mapNotNull { referenceProperty ->
                val text = host.text.removeDummyIdentifier()
                val startOffset = text.indexOf(referenceProperty.key)
                // The key is extracted from the evaluated @Value string, which may not appear
                // literally in the raw host text (e.g. a template referencing a constant).
                // If it can't be located, skip it instead of building an invalid TextRange
                // such as (-1, n), which throws IllegalArgumentException (issue #236).
                if (startOffset < 0) return@mapNotNull null

                ExplytPropertyReference(
                    host, referenceProperty.key,
                    TextRange.from(startOffset, referenceProperty.key.length)
                )
            }

        return (propertyReferences + spelBeanReferences(host)).toTypedArray()
    }

    /**
     * The bean references, and the members they read, inside every SpEL block of the value (issue #44).
     *
     * Matched against the raw host text rather than the evaluated string: the offsets a reference needs are
     * offsets into the host, and taking them straight from the matcher keeps them valid by construction. The
     * placeholder path above has to search for its key instead, because a placeholder may be assembled from a
     * constant and so not appear literally in the host — and that search is what produced the invalid ranges of
     * issue #236. A bean name in a SpEL block is always written out, so the search is unnecessary here.
     */
    private fun spelBeanReferences(host: PsiLanguageInjectionHost): List<PsiReference> {
        val hostText = host.text
        if (!hostText.contains(SPEL_PREFIX)) return emptyList()

        val references = mutableListOf<PsiReference>()
        val spelMatcher = SPEL_PATTERN.matcher(hostText)
        while (spelMatcher.find()) {
            val blockStart = spelMatcher.start(1)
            val beanMatcher = SPEL_BEAN_PATTERN.matcher(spelMatcher.group(1))
            while (beanMatcher.find()) {
                val beanName = beanMatcher.group(1)
                references += SpelBeanReference(
                    host, beanName,
                    TextRange(blockStart + beanMatcher.start(1), blockStart + beanMatcher.end(1))
                )

                val memberName = beanMatcher.group(2) ?: continue
                references += SpelBeanMemberReference(
                    host, beanName, memberName,
                    TextRange(blockStart + beanMatcher.start(2), blockStart + beanMatcher.end(2))
                )
            }
        }
        return references
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

