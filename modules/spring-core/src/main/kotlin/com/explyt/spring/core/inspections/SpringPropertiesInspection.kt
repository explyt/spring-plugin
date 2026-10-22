/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.completion.properties.DefinedConfigurationProperty
import com.explyt.spring.core.completion.properties.PropertiesPropertySource
import com.explyt.spring.core.inspections.quickfix.ReplacementKeyQuickFix
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.PropertyUtil.toKebabCase
import com.intellij.openapi.util.TextRange
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.properties.PropertiesQuickFixFactory
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.Property
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class SpringPropertiesInspection : SpringBasePropertyInspection() {

    override fun loadFileProperties(file: PsiFile): List<DefinedConfigurationProperty> {
        return (file as? PropertiesFile)?.let { PropertiesPropertySource(it).properties } ?: emptyList()
    }

    override fun getKeyPsiElement(property: DefinedConfigurationProperty): PsiElement? {
        return (property.psiElement as? Property)?.firstChild
    }

    override fun getRemoveKeyQuickFixes(property: DefinedConfigurationProperty): List<LocalQuickFix> {
        return (property.psiElement as? Property)
            ?.let { PropertiesQuickFixFactory.getInstance().createRemovePropertyLocalFix(it) }
            ?.let { listOf(it) } ?: emptyList()
    }

    /**
     * The whole key is one element here, so the offending segment is highlighted by narrowing the range rather than
     * by looking for another element: `test.fooBar=1` underlines `fooBar` alone.
     */
    override fun keyShouldBeKebabProblemDescriptor(
        manager: InspectionManager,
        psiKey: PsiElement,
        isOnTheFly: Boolean,
        key: String,
        nonCanonical: PropertyUtil.Segment
    ): ProblemDescriptor = manager.createProblemDescriptor(
        psiKey,
        segmentRangeIn(psiKey, nonCanonical),
        SpringCoreBundle.message("explyt.spring.inspection.properties.value.should.be.kebab"),
        // Relaxed binding makes every spelling resolve to the same property, so a non-canonical key is a style
        // deviation, not a defect. Reporting it as a warning put it next to "Cannot resolve key property".
        ProblemHighlightType.WEAK_WARNING,
        isOnTheFly,
        ReplacementKeyQuickFix(toKebabCase(key), psiKey.parent)
    )

    private fun segmentRangeIn(psiKey: PsiElement, nonCanonical: PropertyUtil.Segment): TextRange {
        val keyRange = ElementManipulators.getValueTextRange(psiKey)
        val start = keyRange.startOffset + nonCanonical.startOffset
        return if (start + nonCanonical.text.length <= keyRange.endOffset) {
            TextRange(start, start + nonCanonical.text.length)
        } else {
            keyRange
        }
    }

}