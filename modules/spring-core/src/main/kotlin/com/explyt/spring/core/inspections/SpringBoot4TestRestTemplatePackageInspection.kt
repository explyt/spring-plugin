/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.util.SpringBootUtil
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.base.psi.replaced
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.ImportPath

/**
 * Reports the legacy `TestRestTemplate` import location in Spring Boot 4+ projects and offers a quick-fix that
 * rewrites the import to the new package.
 *
 * In Spring Boot 4.0 `TestRestTemplate` moved from `org.springframework.boot.test.web.client` to
 * `org.springframework.boot.resttestclient`. The inspection only triggers when Spring Boot 4+ is detected and the
 * new class is resolvable on the classpath.
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide">Spring Boot 4.0 Migration Guide</a>
 */
class SpringBoot4TestRestTemplatePackageInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                when (element) {
                    is KtImportDirective ->
                        inspectImport(element, element.importedFqName?.asString(), element.isAllUnder, holder)

                    is PsiImportStatement ->
                        inspectImport(element, element.qualifiedName, element.isOnDemand, holder)
                }
            }
        }
    }

    private fun inspectImport(element: PsiElement, fqName: String?, isWildcard: Boolean, holder: ProblemsHolder) {
        if (isWildcard || fqName != OLD_TEST_REST_TEMPLATE) return
        if (!SpringBootUtil.isAtLeastSpringBoot4(element)) return
        if (!isResolvable(holder.project, NEW_TEST_REST_TEMPLATE)) return

        holder.registerProblem(
            element,
            message("explyt.spring.inspection.boot4.testresttemplate"),
            ProblemHighlightType.LIKE_DEPRECATED,
            ReplaceImportFix()
        )
    }

    private fun isResolvable(project: Project, fqName: String): Boolean {
        return JavaPsiFacade.getInstance(project).findClass(fqName, GlobalSearchScope.allScope(project)) != null
    }

    companion object {
        const val OLD_TEST_REST_TEMPLATE = "org.springframework.boot.test.web.client.TestRestTemplate"
        const val NEW_TEST_REST_TEMPLATE = "org.springframework.boot.resttestclient.TestRestTemplate"
    }
}

private class ReplaceImportFix : LocalQuickFix {

    override fun getFamilyName(): String = message("explyt.spring.inspection.boot4.testresttemplate.fix")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val target = SpringBoot4TestRestTemplatePackageInspection.NEW_TEST_REST_TEMPLATE
        when (val element = descriptor.psiElement) {
            is KtImportDirective -> {
                val newImport = KtPsiFactory(project).createImportDirective(ImportPath(FqName(target), false))
                element.replaced(newImport)
            }

            is PsiImportStatement -> {
                val psiClass = JavaPsiFacade.getInstance(project)
                    .findClass(target, GlobalSearchScope.allScope(project)) ?: return
                val newImport = JavaPsiFacade.getInstance(project).elementFactory.createImportStatement(psiClass)
                element.replace(newImport)
            }
        }
    }
}
