/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.completion.properties.DefinedConfigurationProperty
import com.explyt.spring.core.completion.properties.PropertiesPropertySource
import com.explyt.spring.core.completion.properties.YamlPropertySource
import com.explyt.spring.core.inspections.quickfix.ReplacementKeyQuickFix
import com.explyt.spring.core.util.PropertyUtil.propertyKeyPsiElement
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.properties.psi.Property
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * Reports Spring Boot configuration properties that were renamed in Spring Boot 4.0 and offers a quick-fix that
 * renames the key (`.properties`).
 *
 * Only runs on Spring configuration property files of a Spring Boot 4+ project.
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide">Spring Boot 4.0 Migration Guide</a>
 */
class SpringBoot4PropertyMigrationInspection : Spring4LocalInspectionTool() {

    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): Array<ProblemDescriptor>? {
        if (!SpringCoreUtil.isConfigurationPropertyFile(file)) return ProblemDescriptor.EMPTY_ARRAY

        val fileProperties = loadFileProperties(file)
        if (fileProperties.isEmpty()) return ProblemDescriptor.EMPTY_ARRAY

        val problems = mutableListOf<ProblemDescriptor>()
        for (property in fileProperties) {
            val psiElement = property.psiElement ?: continue
            val psiKey = psiElement.propertyKeyPsiElement() ?: continue

            val renamed = renameTargetFor(property.key) ?: continue
            problems += manager.createProblemDescriptor(
                psiKey,
                message("explyt.spring.inspection.boot4.property.renamed", renamed),
                isOnTheFly,
                renameQuickFixes(property, renamed),
                ProblemHighlightType.LIKE_DEPRECATED
            )
        }
        return problems.toTypedArray()
    }

    private fun loadFileProperties(file: PsiFile): List<DefinedConfigurationProperty> = when (file) {
        is YAMLFile -> YamlPropertySource(file).properties
        is com.intellij.lang.properties.psi.PropertiesFile -> PropertiesPropertySource(file).properties
        else -> emptyList()
    }

    private fun renameQuickFixes(property: DefinedConfigurationProperty, newKey: String): Array<LocalQuickFix> {
        // A robust one-click rename is currently provided for .properties files; for YAML the suggested key is
        // shown in the message (nested-mapping restructuring is intentionally out of scope here).
        val propertyElement = property.psiElement as? Property ?: return LocalQuickFix.EMPTY_ARRAY
        return arrayOf(ReplacementKeyQuickFix(newKey, propertyElement))
    }

    companion object {
        // Exact key renames: old fully-qualified key -> new fully-qualified key.
        private val EXACT_RENAMES: Map<String, String> = mapOf(
            "spring.dao.exceptiontranslation.enabled" to "spring.persistence.exceptiontranslation.enabled",
            "spring.kafka.retry.topic.backoff.random" to "spring.kafka.retry.topic.backoff.jitter",
        )

        // Prefix renames: a key equal to the old prefix, or starting with "<old>.", is rewritten by swapping the
        // prefix. Ordered; the first matching prefix wins, so list more specific prefixes first.
        private val PREFIX_RENAMES: List<Pair<String, String>> = listOf(
            "spring.jackson.read" to "spring.jackson.json.read",
            "spring.jackson.write" to "spring.jackson.json.write",
            "spring.session.redis" to "spring.session.data.redis",
            "spring.session.mongodb" to "spring.session.data.mongodb",
        )

        /**
         * Returns the Spring Boot 4 replacement key for [key], or `null` when [key] was not renamed.
         */
        fun renameTargetFor(key: String): String? {
            EXACT_RENAMES[key]?.let { return it }
            for ((oldPrefix, newPrefix) in PREFIX_RENAMES) {
                if (key == oldPrefix || key.startsWith("$oldPrefix.")) {
                    return newPrefix + key.substring(oldPrefix.length)
                }
            }
            return null
        }
    }
}
