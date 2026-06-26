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
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod

/**
 * Reports declarations (fields, method parameters and return types) whose type is the legacy `TestRestTemplate`
 * in Spring Boot 4+ projects, and offers a quick-fix that updates the import to the new package.
 *
 * In Spring Boot 4.0 `TestRestTemplate` moved from `org.springframework.boot.test.web.client` to
 * `org.springframework.boot.resttestclient`.
 *
 * The visible type usage is highlighted (not the import statement, which the IDE usually folds away).
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide">Spring Boot 4.0 Migration Guide</a>
 */
class SpringBoot4TestRestTemplatePackageInspection : Spring4UastLocalInspectionTool() {

    override fun checkField(field: UField, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        val problem = checkTypeReference(field.typeReference?.sourcePsi, field.type.canonicalText, manager, isOnTheFly)
        return problem?.let { arrayOf(it) } ?: emptyArray()
    }

    override fun checkMethod(method: UMethod, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        checkTypeReference(method.returnTypeReference?.sourcePsi, method.returnType?.canonicalText, manager, isOnTheFly)
            ?.let { problems += it }
        for (parameter in method.uastParameters) {
            checkTypeReference(parameter.typeReference?.sourcePsi, parameter.type.canonicalText, manager, isOnTheFly)
                ?.let { problems += it }
        }
        return problems.toTypedArray()
    }

    private fun checkTypeReference(
        typeSourcePsi: PsiElement?,
        canonicalText: String?,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): ProblemDescriptor? {
        if (typeSourcePsi == null) return null
        if (canonicalText != OLD_TEST_REST_TEMPLATE) return null

        return manager.createProblemDescriptor(
            typeSourcePsi,
            message("explyt.spring.inspection.boot4.testresttemplate"),
            isOnTheFly,
            arrayOf<LocalQuickFix>(MigrateImportQuickFix(OLD_TEST_REST_TEMPLATE, NEW_TEST_REST_TEMPLATE)),
            ProblemHighlightType.LIKE_DEPRECATED
        )
    }

    companion object {
        const val OLD_TEST_REST_TEMPLATE = "org.springframework.boot.test.web.client.TestRestTemplate"
        const val NEW_TEST_REST_TEMPLATE = "org.springframework.boot.resttestclient.TestRestTemplate"
    }
}
