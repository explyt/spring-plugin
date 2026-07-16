/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.completion.properties.DefinedConfigurationProperty
import com.explyt.spring.core.completion.properties.PropertiesPropertySource
import com.explyt.spring.core.completion.properties.YamlPropertySource
import com.explyt.spring.core.util.PropertyUtil.propertyValuePsiElement
import com.explyt.spring.core.util.SpringBootUtil
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.Property
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * Reports the legacy `httptrace` Actuator endpoint id used in `management.endpoints.*.exposure.include/exclude`
 * property values, and offers a quick-fix that renames it to `httpexchanges`.
 *
 * The `httptrace` endpoint was renamed to `httpexchanges` in Spring Boot 3.0. The inspection only runs on Spring
 * configuration property files of a Spring Boot 3+ project.
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

        val fileProperties = loadFileProperties(file)
        if (fileProperties.isEmpty()) return ProblemDescriptor.EMPTY_ARRAY

        val problems = mutableListOf<ProblemDescriptor>()
        for (property in fileProperties) {
            if (property.key !in EXPOSURE_KEYS) continue
            val value = property.value ?: continue
            if (!containsLegacyEndpoint(value)) continue
            val psiElement = property.psiElement ?: continue
            val psiValue = psiElement.propertyValuePsiElement() ?: continue

            problems += manager.createProblemDescriptor(
                psiValue,
                message("explyt.spring.inspection.boot3.actuator.httptrace"),
                isOnTheFly,
                arrayOf<LocalQuickFix>(RenameHttpTraceEndpointFix()),
                ProblemHighlightType.LIKE_DEPRECATED
            )
        }
        return problems.toTypedArray()
    }

    private fun loadFileProperties(file: PsiFile): List<DefinedConfigurationProperty> = when (file) {
        is YAMLFile -> YamlPropertySource(file).properties
        is PropertiesFile -> PropertiesPropertySource(file).properties
        else -> emptyList()
    }

    companion object {
        private const val LEGACY_ENDPOINT = "httptrace"
        private const val NEW_ENDPOINT = "httpexchanges"

        private val EXPOSURE_KEYS: Set<String> = setOf(
            "management.endpoints.web.exposure.include",
            "management.endpoints.web.exposure.exclude",
            "management.endpoints.jmx.exposure.include",
            "management.endpoints.jmx.exposure.exclude",
        )

        /** Splits a comma-separated exposure value into trimmed, non-empty tokens. */
        private fun tokens(value: String): List<String> =
            value.split(',').map { it.trim() }.filter { it.isNotEmpty() }

        fun containsLegacyEndpoint(value: String): Boolean =
            tokens(value).any { it == LEGACY_ENDPOINT }

        /** Returns [value] with every `httptrace` token replaced by `httpexchanges`, preserving other tokens. */
        fun migratedValue(value: String): String =
            tokens(value).joinToString(",") { if (it == LEGACY_ENDPOINT) NEW_ENDPOINT else it }
    }
}

private class RenameHttpTraceEndpointFix : LocalQuickFix {
    override fun getFamilyName(): String = message("explyt.spring.inspection.boot3.actuator.httptrace.fix")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        when (val property = descriptor.psiElement.parent) {
            is Property -> {
                val migrated = SpringBoot3ActuatorHttpExchangesInspection
                    .migratedValue(property.value ?: return)
                property.setValue(migrated)
            }

            is YAMLKeyValue -> {
                val migrated = SpringBoot3ActuatorHttpExchangesInspection
                    .migratedValue(property.valueText)
                property.setValue(
                    org.jetbrains.yaml.YAMLElementGenerator.getInstance(project)
                        .createYamlKeyValue("k", migrated).value ?: return
                )
            }
        }
    }
}
