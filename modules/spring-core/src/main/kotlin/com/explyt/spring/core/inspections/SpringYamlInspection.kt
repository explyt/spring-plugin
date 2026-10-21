/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.completion.properties.DefinedConfigurationProperty
import com.explyt.spring.core.completion.properties.YamlPropertySource
import com.explyt.spring.core.inspections.quickfix.YamlKeyToKebabQuickFix
import com.explyt.spring.core.util.PropertyUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.YAMLBundle
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue

class SpringYamlInspection : SpringBasePropertyInspection() {

    override fun loadFileProperties(file: PsiFile): List<DefinedConfigurationProperty> {
        return (file as? YAMLFile)?.let { YamlPropertySource(it).properties } ?: emptyList()
    }

    override fun getKeyPsiElement(property: DefinedConfigurationProperty): PsiElement? {
        return (property.psiElement as? YAMLKeyValue)?.key
    }

    override fun getRemoveKeyQuickFixes(property: DefinedConfigurationProperty): List<LocalQuickFix> {
        return listOf(RemoveDuplicatedKeyQuickFix())
    }

    /**
     * Each dotted segment is usually its own `YAMLKeyValue`, so the offending segment normally lives on an ancestor
     * of the inspected leaf: `explyt.camel.camelWritten.items[0].name` deviates in `camelWritten`, several levels up
     * from `name`.
     *
     * One element can still hold several segments (`explyt.camel:`), so the range is narrowed inside the element that
     * owns the segment.
     *
     * The quick fix keeps receiving the **leaf**, not the element the problem is reported on. `applyFix` of
     * `LocalQuickFixOnPsiElement` resolves its own stored pointer and ignores the descriptor, so the reported range
     * and the fix target are independent. That separation matters: the fix derives the key to rewrite from
     * `YAMLUtil.getConfigFullName(startElement)`, so handing it the ancestor would shorten that key to
     * `explyt.rate-limit` and `ReferencesSearch` would no longer find the `${explyt.rateLimit.defaultRpm}`
     * placeholders that reference the leaf, leaving them stale. From the leaf it renames every segment on the way up,
     * the offending ancestor included.
     */
    override fun keyShouldBeKebabProblemDescriptor(
        manager: InspectionManager,
        psiKey: PsiElement,
        isOnTheFly: Boolean,
        key: String,
        nonCanonical: PropertyUtil.Segment
    ): ProblemDescriptor {
        val owner = owningKeyValue(psiKey, nonCanonical) ?: psiKey.parent as? YAMLKeyValue
        val reportOn = owner?.key ?: psiKey
        return manager.createProblemDescriptor(
            reportOn,
            segmentRangeIn(reportOn, nonCanonical),
            SpringCoreBundle.message("explyt.spring.inspection.properties.value.should.be.kebab"),
            // Relaxed binding makes every spelling resolve to the same property, so a non-canonical key is a style
            // deviation, not a defect. Reporting it as a warning put it next to "Cannot resolve key property".
            ProblemHighlightType.WEAK_WARNING,
            isOnTheFly,
            YamlKeyToKebabQuickFix(psiKey.parent)
        )
    }

    /**
     * The ancestor key-value whose own key text contains [nonCanonical].
     *
     * `YAMLUtil.getConfigFullNameParts` already walks leaf to root to build the key, so the segment is located by
     * replaying that same walk over the ancestors rather than by searching the tree again.
     */
    private fun owningKeyValue(psiKey: PsiElement, nonCanonical: PropertyUtil.Segment): YAMLKeyValue? {
        var current = psiKey.parent as? YAMLKeyValue
        var owner: YAMLKeyValue? = null
        while (current != null) {
            if (current.keyText.split('.').any { it.substringBefore('[') == nonCanonical.text }) {
                // Keep walking: the outermost match is the one whose position in the full key the offset refers to.
                owner = current
            }
            current = PsiTreeUtil.getParentOfType(current, YAMLKeyValue::class.java)
        }
        return owner
    }

    /** The range of [nonCanonical] inside [reportOn], which may itself hold several dotted segments. */
    private fun segmentRangeIn(reportOn: PsiElement, nonCanonical: PropertyUtil.Segment): TextRange {
        val elementRange = ElementManipulators.getValueTextRange(reportOn)
        val offsetInElement = reportOn.text.indexOf(nonCanonical.text)
        if (offsetInElement < 0) return elementRange
        val start = offsetInElement
        val end = start + nonCanonical.text.length
        return if (end <= reportOn.textLength) TextRange(start, end) else elementRange
    }
}

private class RemoveDuplicatedKeyQuickFix : LocalQuickFix {
    override fun getFamilyName(): String {
        return YAMLBundle.message("YAMLDuplicatedKeysInspection.remove.key.quickfix.name")
    }

    override fun availableInBatchMode() = false

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val keyVal = descriptor.psiElement.parent as? YAMLKeyValue ?: return
        keyVal.parentMapping?.deleteKeyValue(keyVal)
    }
}