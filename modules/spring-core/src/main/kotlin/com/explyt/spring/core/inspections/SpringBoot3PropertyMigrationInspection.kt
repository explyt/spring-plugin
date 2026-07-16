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
import com.explyt.spring.core.inspections.quickfix.ReplacementKeyQuickFix
import com.explyt.spring.core.util.PropertyUtil.propertyKeyPsiElement
import com.explyt.spring.core.util.SpringBootUtil
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.properties.PropertiesQuickFixFactory
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.Property
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * Reports Spring Boot configuration properties that were renamed or removed in Spring Boot 3.0 and offers a
 * quick-fix that renames the key (`.properties`) or removes a no-longer-supported key.
 *
 * Only runs on Spring configuration property files of a Spring Boot 3+ project.
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide">Spring Boot 3.0 Migration Guide</a>
 */
class SpringBoot3PropertyMigrationInspection : SpringBaseLocalInspectionTool() {

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
            val psiElement = property.psiElement ?: continue
            val psiKey = psiElement.propertyKeyPsiElement() ?: continue

            val renamed = renameTargetFor(property.key)
            if (renamed != null) {
                problems += manager.createProblemDescriptor(
                    psiKey,
                    message("explyt.spring.inspection.boot3.property.renamed", renamed),
                    isOnTheFly,
                    renameQuickFixes(property, renamed),
                    ProblemHighlightType.LIKE_DEPRECATED
                )
                continue
            }
            if (property.key in REMOVED_KEYS) {
                problems += manager.createProblemDescriptor(
                    psiKey,
                    message("explyt.spring.inspection.boot3.property.removed"),
                    isOnTheFly,
                    removeQuickFixes(property),
                    ProblemHighlightType.LIKE_DEPRECATED
                )
            }
        }
        return problems.toTypedArray()
    }

    private fun loadFileProperties(file: PsiFile): List<DefinedConfigurationProperty> = when (file) {
        is YAMLFile -> YamlPropertySource(file).properties
        is PropertiesFile -> PropertiesPropertySource(file).properties
        else -> emptyList()
    }

    private fun renameQuickFixes(property: DefinedConfigurationProperty, newKey: String): Array<LocalQuickFix> {
        // A robust one-click rename is currently provided for .properties files; for YAML the suggested key is
        // shown in the message (nested-mapping restructuring is intentionally out of scope here).
        val propertyElement = property.psiElement as? Property ?: return LocalQuickFix.EMPTY_ARRAY
        return arrayOf(ReplacementKeyQuickFix(newKey, propertyElement))
    }

    private fun removeQuickFixes(property: DefinedConfigurationProperty): Array<LocalQuickFix> {
        return when (val element = property.psiElement) {
            is Property -> PropertiesQuickFixFactory.getInstance()
                .createRemovePropertyLocalFix(element)
                ?.let { arrayOf<LocalQuickFix>(it) } ?: LocalQuickFix.EMPTY_ARRAY

            is YAMLKeyValue -> arrayOf(RemoveYamlKeyQuickFix())
            else -> LocalQuickFix.EMPTY_ARRAY
        }
    }

    companion object {
        // Exact key renames: old fully-qualified key -> new fully-qualified key.
        private val EXACT_RENAMES: Map<String, String> = mapOf(
            "server.max-http-header-size" to "server.max-http-request-header-size",
        )

        // Prefix renames: a key equal to the old prefix, or starting with "<old>.", is rewritten by swapping the
        // prefix. Ordered; the first matching prefix wins.
        private val PREFIX_RENAMES: List<Pair<String, String>> = listOf(
            "spring.data.cassandra" to "spring.cassandra",
            "spring.redis" to "spring.data.redis",
        )

        // Keys that are no longer supported in Spring Boot 3 and should be removed.
        private val REMOVED_KEYS: Set<String> = setOf(
            "spring.jpa.hibernate.use-new-id-generator-mappings",
            "spring.session.store-type",
        )

        /**
         * Returns the Spring Boot 3 replacement key for [key], or `null` when [key] was not renamed.
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

private class RemoveYamlKeyQuickFix : LocalQuickFix {
    override fun getFamilyName(): String = message("explyt.spring.inspection.boot3.property.removed.fix")

    override fun availableInBatchMode(): Boolean = false

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val keyValue = descriptor.psiElement.parent as? YAMLKeyValue
            ?: descriptor.psiElement as? YAMLKeyValue
            ?: return
        keyValue.parentMapping?.deleteKeyValue(keyValue)
    }
}
