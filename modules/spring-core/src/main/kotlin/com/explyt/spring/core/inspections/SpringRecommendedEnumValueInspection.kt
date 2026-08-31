/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.SpringProperties.PLACEHOLDER_PREFIX
import com.explyt.spring.core.SpringProperties.PLACEHOLDER_SUFFIX
import com.explyt.spring.core.completion.properties.ConfigurationProperty
import com.explyt.spring.core.completion.properties.DefinedConfigurationProperty
import com.explyt.spring.core.completion.properties.PropertiesPropertySource
import com.explyt.spring.core.completion.properties.PropertyHint
import com.explyt.spring.core.completion.properties.YamlPropertySource
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.PropertyUtil.propertyValuePsiElement
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.Property
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * Reports a configuration value that is not written in the spelling Spring itself recommends, and offers a quick-fix
 * that rewrites it. Relaxed binding accepts every spelling, so this is a style report, not an error.
 *
 * Two sources say what the recommended spelling is:
 *
 * - **Metadata hints.** A hint states the accepted literals verbatim, and every literal Spring ships is lower case
 *   (`trace`, `info`, `read-only`), so a value that differs from a literal only by case is written against the
 *   metadata that declares it. `logging.level.*=INFO` is the everyday case.
 * - **Enum constants.** Spring's own metadata never ships the declared `SCREAMING_SNAKE` name as a default value:
 *   measured on `spring-boot-autoconfigure-3.5.13`, all 64 enum-typed properties with a string `defaultValue` use
 *   lower case (`never`, `graceful`) or kebab-case (`read-uncommitted`, `path-pattern-parser`).
 *
 * A value that matches nothing is never reported here: it is either free-form (a hint whose provider is `any`) or a
 * genuine error, which [SpringBasePropertyInspection] owns.
 */
class SpringRecommendedEnumValueInspection : SpringBaseLocalInspectionTool() {

    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): Array<ProblemDescriptor>? {
        if (!SpringCoreUtil.isConfigurationPropertyFile(file)) return ProblemDescriptor.EMPTY_ARRAY

        val module = ModuleUtilCore.findModuleForFile(file) ?: return ProblemDescriptor.EMPTY_ARRAY
        val problems = mutableListOf<ProblemDescriptor>()
        for (property in loadFileProperties(file)) {
            ProgressManager.checkCanceled()
            problems += checkProperty(module, property, manager, isOnTheFly)
        }
        return problems.toTypedArray()
    }

    private fun loadFileProperties(file: PsiFile): List<DefinedConfigurationProperty> = when (file) {
        is YAMLFile -> YamlPropertySource(file).properties
        is PropertiesFile -> PropertiesPropertySource(file).properties
        else -> emptyList()
    }

    private fun checkProperty(
        module: Module,
        property: DefinedConfigurationProperty,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): List<ProblemDescriptor> {
        val psiElement = property.psiElement ?: return emptyList()
        val value = property.value?.trimEnd()
        if (value.isNullOrBlank()) return emptyList()
        val psiValue = psiElement.propertyValuePsiElement() ?: return emptyList()

        val configurationProperty = PropertyUtil.findValueOwner(module, property.key)
        // A hint states the literals of this very property, so it outranks the value type: an enum-typed property that
        // also ships hints has already declared which spelling to write.
        val hint = PropertyUtil.getPropertyHint(module, property.key)
        val recommend = hintRecommendation(hint)
            ?: enumRecommendation(module, configurationProperty)
            ?: return emptyList()

        val problems = mutableListOf<ProblemDescriptor>()
        for (element in valueElements(value, configurationProperty, property.key)) {
            if (element.text.contains(PLACEHOLDER_PREFIX) && element.text.contains(PLACEHOLDER_SUFFIX)) continue
            val recommended = recommend(element.text) ?: continue
            if (element.text == recommended.spelling) continue

            problems += manager.createProblemDescriptor(
                psiValue,
                element.rangeIn(psiValue.text, value),
                message(recommended.messageKey, recommended.spelling),
                ProblemHighlightType.WEAK_WARNING,
                isOnTheFly,
                RewriteValueQuickFix(element.text, recommended.spelling)
            )
        }
        return problems
    }

    /**
     * The literal of [hint] that a written value denotes, matched the way Spring binds it — case-insensitively, so
     * `INFO` and `Info` both name the `info` literal that the metadata declares.
     */
    private fun hintRecommendation(hint: PropertyHint?): ((String) -> Recommendation?)? {
        val literals = hint?.values?.mapNotNull { it.value }?.takeIf { it.isNotEmpty() } ?: return null
        return { written ->
            literals.firstOrNull { it.equals(written, ignoreCase = true) }
                ?.let { Recommendation(it, "explyt.spring.inspection.properties.value.not.recommended") }
        }
    }

    private fun enumRecommendation(
        module: Module,
        configurationProperty: ConfigurationProperty?
    ): ((String) -> Recommendation?)? {
        val enumClass = configurationProperty
            ?.let { PropertyUtil.valueTypeOf(it) }
            ?.let { JavaPsiFacade.getInstance(module.project).findClass(it, GlobalSearchScope.allScope(module.project)) }
            ?.takeIf { it.isEnum }
            ?: return null
        return { written ->
            PropertyUtil.findEnumConstant(enumClass, written)?.let {
                Recommendation(
                    PropertyUtil.recommendedValueSpelling(it.name),
                    "explyt.spring.inspection.properties.value.enum.not.recommended"
                )
            }
        }
    }

    /**
     * The individual values written in [value]: the elements of a comma-separated list for a collection property, and
     * the whole value otherwise. An indexed key such as `include[0]` already denotes a single element.
     */
    private fun valueElements(
        value: String,
        configurationProperty: ConfigurationProperty?,
        key: String
    ): List<ValueElement> {
        val isCollection = configurationProperty != null
                && (configurationProperty.isArray() || configurationProperty.isList())
                && !key.endsWith("]")
        if (!isCollection) return listOf(ValueElement(value.trim(), value.indexOf(value.trim())))

        val elements = mutableListOf<ValueElement>()
        var offset = 0
        for (token in value.split(',')) {
            val trimmed = token.trim()
            if (trimmed.isNotEmpty()) elements += ValueElement(trimmed, offset + token.indexOf(trimmed))
            offset += token.length + 1
        }
        return elements
    }
}

/** The spelling to write instead of what the user wrote, and the message explaining where it comes from. */
private data class Recommendation(val spelling: String, val messageKey: String)

/** One value written in a property, with its offset inside the raw value text. */
private data class ValueElement(val text: String, val offsetInValue: Int) {

    /**
     * The element's range inside [psiValueText]. The value PSI of a quoted YAML scalar carries the quotes, so the raw
     * value text is located first instead of assuming the two start at the same offset.
     */
    fun rangeIn(psiValueText: String, rawValue: String): TextRange {
        val valueStart = psiValueText.indexOf(rawValue).coerceAtLeast(0)
        val start = valueStart + offsetInValue
        return TextRange(start, (start + text.length).coerceAtMost(psiValueText.length))
    }
}

private class RewriteValueQuickFix(
    private val oldValue: String,
    private val newValue: String
) : LocalQuickFix {

    override fun getFamilyName(): String =
        message("explyt.spring.inspection.value.replacement.fix", oldValue, newValue)

    /**
     * Only the reported element of the value is rewritten, so the other elements of a comma-separated list and the
     * separators around them survive verbatim. The edit goes through PSI rather than through the document, because the
     * Alt+Enter preview hands the fix a non-physical file copy that has no document to write to.
     */
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement ?: return
        if (!element.isValid) return

        val rangeInElement = descriptor.textRangeInElement ?: TextRange(0, element.textLength)
        val text = element.text
        if (rangeInElement.endOffset > text.length || rangeInElement.substring(text) != oldValue) return
        val rewritten = text.replaceRange(rangeInElement.startOffset, rangeInElement.endOffset, newValue)

        val property = element.parent as? Property
        if (property != null) {
            property.setValue(rewritten)
            return
        }
        // The manipulator rewrites the scalar content range only, so quoting style and indentation survive.
        if (element is YAMLScalar) ElementManipulators.handleContentChange(element, rewritten)
    }
}
