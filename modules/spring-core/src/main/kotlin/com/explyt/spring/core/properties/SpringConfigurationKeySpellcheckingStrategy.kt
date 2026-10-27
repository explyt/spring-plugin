/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties

import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.spellchecker.inspections.PlainTextSplitter
import com.intellij.spellchecker.inspections.PropertiesSplitter
import com.intellij.spellchecker.inspections.Splitter
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.TokenConsumer
import com.intellij.spellchecker.tokenizer.Tokenizer
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * Spellchecks a configuration key in a Spring configuration file without its declared part.
 *
 * `spring.security.oauth2.resourceserver` is spelled the way Spring declares it, so a typo report there can only be
 * fixed by breaking the binding. The segments the configuration model declares are skipped; the rest — keys the
 * application invents, misspelled ones — are checked the way the language's own strategy checks a key.
 */
class SpringConfigurationKeySpellcheckingStrategy : SpellcheckingStrategy() {

    override fun isMyContext(element: PsiElement): Boolean =
        KeySyntax.of(element) != null && SpringCoreUtil.isConfigurationPropertyFile(element.containingFile)

    override fun getTokenizer(element: PsiElement): Tokenizer<*> = UndeclaredKeySegmentsTokenizer

    private object UndeclaredKeySegmentsTokenizer : Tokenizer<PsiElement>() {
        override fun tokenize(element: PsiElement, consumer: TokenConsumer) {
            val syntax = KeySyntax.of(element) ?: return
            val text = element.text
            val undeclared = undeclaredRange(element, syntax.fullKey(element), text)
            if (undeclared.isEmpty) return
            consumer.consumeToken(element, text, syntax.renamesOnFix, 0, undeclared, syntax.splitter)
        }

        private fun undeclaredRange(element: PsiElement, fullKey: String, text: String): TextRange {
            val declared = ModuleUtilCore.findModuleForPsiElement(element)
                ?.let { DeclaredKeyPrefix.segmentCount(it, fullKey) } ?: 0
            val segments = PropertyUtil.keySegments(text)
            val segmentsAboveElement = PropertyUtil.keySegments(fullKey).size - segments.size
            val declaredInElement = (declared - segmentsAboveElement).coerceIn(0, segments.size)
            val start = segments.take(declaredInElement).sumOf { it.length + 1 }.coerceAtMost(text.length)
            return TextRange(start, text.length)
        }
    }

    private enum class KeySyntax(val renamesOnFix: Boolean, val splitter: Splitter) {
        PROPERTIES(renamesOnFix = true, PropertiesSplitter.getInstance()) {
            override fun fullKey(element: PsiElement): String = element.text
        },
        YAML(renamesOnFix = false, PlainTextSplitter.getInstance()) {
            override fun fullKey(element: PsiElement): String = YAMLUtil.getConfigFullName(element.parent as YAMLKeyValue)
        };

        abstract fun fullKey(element: PsiElement): String

        companion object {
            fun of(element: PsiElement): KeySyntax? = when {
                element is PropertyKeyImpl -> PROPERTIES
                element.elementType == YAMLTokenTypes.SCALAR_KEY && element.parent is YAMLKeyValue -> YAML
                else -> null
            }
        }
    }
}
