/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.inspections.quickfix.MigrateImportQuickFix
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod

/**
 * Reports declarations (fields, method parameters and return types) whose type is a `BootstrapRegistry` /
 * `BootstrapContext` family type from the legacy `org.springframework.boot` package in Spring Boot 4+ projects, and
 * offers a quick-fix that updates the import to the new `org.springframework.boot.bootstrap` package.
 *
 * The visible type usage is highlighted (not the import statement, which the IDE usually folds away).
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide">Spring Boot 4.0 Migration Guide</a>
 */
class SpringBoot4BootstrapPackageInspection : Spring4UastLocalInspectionTool() {

    override fun checkField(field: UField, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        val problem = checkType(field.typeReference?.sourcePsi, field.type.canonicalText, manager, isOnTheFly)
        return problem?.let { arrayOf(it) } ?: emptyArray()
    }

    override fun checkMethod(method: UMethod, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        checkType(method.returnTypeReference?.sourcePsi, method.returnType?.canonicalText, manager, isOnTheFly)
            ?.let { problems += it }
        for (parameter in method.uastParameters) {
            checkType(parameter.typeReference?.sourcePsi, parameter.type.canonicalText, manager, isOnTheFly)
                ?.let { problems += it }
        }
        return problems.toTypedArray()
    }

    private fun checkType(
        typeSourcePsi: PsiElement?,
        canonicalText: String?,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): ProblemDescriptor? {
        if (typeSourcePsi == null || canonicalText == null) return null
        val target = MOVED_TYPES[canonicalText] ?: return null

        return manager.createProblemDescriptor(
            typeSourcePsi,
            message("explyt.spring.inspection.boot4.bootstrap"),
            isOnTheFly,
            arrayOf<LocalQuickFix>(MigrateImportQuickFix(canonicalText, target)),
            ProblemHighlightType.LIKE_DEPRECATED
        )
    }

    companion object {
        private const val OLD_PKG = "org.springframework.boot"
        private const val NEW_PKG = "org.springframework.boot.bootstrap"

        // old FQN -> new FQN for the bootstrap types relocated in Spring Boot 4.
        val MOVED_TYPES: Map<String, String> = listOf(
            "BootstrapRegistry",
            "ConfigurableBootstrapContext",
            "DefaultBootstrapContext",
            "BootstrapContext",
            "BootstrapRegistryInitializer",
        ).associate { "$OLD_PKG.$it" to "$NEW_PKG.$it" }
    }
}
