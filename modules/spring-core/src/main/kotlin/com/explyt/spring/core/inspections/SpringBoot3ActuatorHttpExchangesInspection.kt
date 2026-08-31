/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.completion.properties.PropertiesPropertySource
import com.explyt.spring.core.util.PropertyUtil.propertyKeyPsiElement
import com.explyt.spring.core.util.PropertyUtil.propertyValuePsiElement
import com.explyt.spring.core.util.SpringBootUtil
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.Property
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLValue

/**
 * Reports the legacy `httptrace` Actuator endpoint id, both in `management.endpoints.*.exposure.include/exclude`
 * property values and in `management.endpoint.httptrace.*` keys, and offers a quick-fix that renames it to
 * `httpexchanges`.
 *
 * Spring's own metadata carries a `deprecation.replacement` only for the `management.trace.http.*` family, so
 * without the key check `management.endpoint.httptrace.enabled` degrades to a bare "cannot resolve key", which reads
 * as a typo rather than a rename.
 *
 * The `httptrace` endpoint was renamed to `httpexchanges` in Spring Boot 3.0. The inspection only runs on Spring
 * configuration property files of a Spring Boot 3+ project.
 *
 * In YAML the value may be written either as a scalar (`include: health,httptrace`) or as a sequence — both a block
 * list and a flow array (`include: [health, httptrace]`); every form is inspected.
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide#httptrace-endpoint-renamed-to-httpexchanges">Spring Boot 3.0 Migration Guide</a>
 */
class SpringBoot3ActuatorHttpExchangesInspection : SpringBaseLocalInspectionTool() {

    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): Array<ProblemDescriptor>? {
        if (!SpringCoreUtil.isConfigurationPropertyFile(file)) return ProblemDescriptor.EMPTY_ARRAY
        if (!SpringBootUtil.isAtLeastSpringBoot3(file)) return ProblemDescriptor.EMPTY_ARRAY

        val problems = when (file) {
            is YAMLFile -> checkYamlFile(file, manager, isOnTheFly)
            is PropertiesFile -> checkPropertiesFile(file, manager, isOnTheFly)
            else -> emptyList()
        }
        return problems.toTypedArray()
    }

    private fun checkPropertiesFile(
        file: PropertiesFile,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): List<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        for (property in PropertiesPropertySource(file).properties) {
            ProgressManager.checkCanceled()
            val key = property.key
            if (isLegacyEndpointKey(key)) {
                val psiKey = property.psiElement?.propertyKeyPsiElement()
                if (psiKey != null) problems += createKeyProblem(psiKey, manager, isOnTheFly)
                continue
            }
            if (key !in EXPOSURE_KEYS) continue
            val value = property.value ?: continue
            if (!containsLegacyEndpoint(value)) continue
            val psiValue = property.psiElement?.propertyValuePsiElement() ?: continue

            problems += createProblem(psiValue, manager, isOnTheFly)
        }
        return problems
    }

    /**
     * A YAML sequence value is not a key-value scalar, so it is invisible to the generic property sources. The file is
     * traversed directly instead, and every scalar of an exposure value is inspected on its own.
     */
    private fun checkYamlFile(
        file: YAMLFile,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): List<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        PsiTreeUtil.processElements(file, YAMLKeyValue::class.java) { keyValue ->
            ProgressManager.checkCanceled()
            val fullName = YAMLUtil.getConfigFullName(keyValue)
            if (fullName in EXPOSURE_KEYS) {
                for (scalar in exposureScalars(keyValue.value)) {
                    if (containsLegacyEndpoint(scalar.textValue)) {
                        problems += createProblem(scalar, manager, isOnTheFly)
                    }
                }
            } else if (isLegacyEndpointKey(fullName) && keyValue.keyText.contains(LEGACY_ENDPOINT)) {
                // Only the key that spells out `httptrace` is reported; the segments above it are keys of their own.
                keyValue.key?.let { problems += createKeyProblem(it, manager, isOnTheFly) }
            }
            true
        }
        return problems
    }

    /** Scalars holding the exposure value: the value itself, or every item of a block list / flow array. */
    private fun exposureScalars(value: YAMLValue?): List<YAMLScalar> = when (value) {
        is YAMLScalar -> listOf(value)
        is YAMLSequence -> value.items.mapNotNull { it.value as? YAMLScalar }
        else -> emptyList()
    }

    private fun createProblem(
        psiValue: PsiElement,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): ProblemDescriptor = manager.createProblemDescriptor(
        psiValue,
        message("explyt.spring.inspection.boot3.actuator.httptrace"),
        isOnTheFly,
        arrayOf<LocalQuickFix>(RenameHttpTraceEndpointFix()),
        ProblemHighlightType.LIKE_DEPRECATED
    )

    private fun createKeyProblem(
        psiKey: PsiElement,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): ProblemDescriptor = manager.createProblemDescriptor(
        psiKey,
        message("explyt.spring.inspection.boot3.actuator.httptrace.key"),
        isOnTheFly,
        arrayOf<LocalQuickFix>(RenameHttpTraceKeyFix()),
        ProblemHighlightType.LIKE_DEPRECATED
    )
}

private const val LEGACY_ENDPOINT = "httptrace"
private const val NEW_ENDPOINT = "httpexchanges"
private const val LEGACY_ENDPOINT_KEY_PREFIX = "management.endpoint.$LEGACY_ENDPOINT"

private val EXPOSURE_KEYS: Set<String> = setOf(
    "management.endpoints.web.exposure.include",
    "management.endpoints.web.exposure.exclude",
    "management.endpoints.jmx.exposure.include",
    "management.endpoints.jmx.exposure.exclude",
)

private fun containsLegacyEndpoint(value: String): Boolean =
    value.split(',').any { it.trim() == LEGACY_ENDPOINT }

/** `management.endpoint.httptrace` and everything below it, e.g. `…httptrace.enabled`. */
private fun isLegacyEndpointKey(key: String?): Boolean =
    key == LEGACY_ENDPOINT_KEY_PREFIX || key?.startsWith("$LEGACY_ENDPOINT_KEY_PREFIX.") == true

/**
 * Returns [value] with every `httptrace` token replaced by `httpexchanges`. Only the matching token is rewritten:
 * separators and the whitespace around every other token are kept as the user wrote them.
 */
private fun migratedValue(value: String): String =
    value.split(',').joinToString(",") { token ->
        if (token.trim() == LEGACY_ENDPOINT) token.replace(LEGACY_ENDPOINT, NEW_ENDPOINT) else token
    }

private class RenameHttpTraceKeyFix : LocalQuickFix {
    override fun getFamilyName(): String = message("explyt.spring.inspection.boot3.actuator.httptrace.key.fix")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement ?: return
        if (!element.isValid) return

        val property = element.parent as? Property
        if (property != null) {
            val key = property.key ?: return
            property.setName(key.replaceFirst(LEGACY_ENDPOINT, NEW_ENDPOINT))
            return
        }
        rewriteKeyText(project, element)
    }

    /**
     * A YAML key is a leaf without an [ElementManipulators] manipulator, and regenerating the key-value would have to
     * reproduce the nested mapping below it. Editing the key range in the document renames the segment in place and
     * leaves indentation and children untouched.
     */
    private fun rewriteKeyText(project: Project, element: PsiElement) {
        val text = element.text
        if (!text.contains(LEGACY_ENDPOINT)) return
        val file = element.containingFile ?: return
        val documentManager = PsiDocumentManager.getInstance(project)
        val document = documentManager.getDocument(file) ?: return
        val range = element.textRange
        document.replaceString(range.startOffset, range.endOffset, text.replaceFirst(LEGACY_ENDPOINT, NEW_ENDPOINT))
        documentManager.commitDocument(document)
    }
}

private class RenameHttpTraceEndpointFix : LocalQuickFix {
    override fun getFamilyName(): String = message("explyt.spring.inspection.boot3.actuator.httptrace.fix")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement ?: return
        if (!element.isValid) return

        if (element is YAMLScalar) {
            // The manipulator rewrites only the scalar content range, so quoting style and indentation survive.
            val migrated = migratedValue(element.textValue)
            if (migrated != element.textValue) {
                ElementManipulators.handleContentChange(element, migrated)
            }
            return
        }

        val property = element.parent as? Property ?: return
        val value = property.value ?: return
        property.setValue(migratedValue(value))
    }
}
