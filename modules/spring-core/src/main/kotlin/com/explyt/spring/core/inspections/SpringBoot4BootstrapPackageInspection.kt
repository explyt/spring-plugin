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
 * Reports the legacy `BootstrapRegistry` / `BootstrapContext` import locations in Spring Boot 4+ projects and offers
 * a quick-fix that rewrites the import to the new `org.springframework.boot.bootstrap` package.
 *
 * In Spring Boot 4.0 `BootstrapRegistry`, `ConfigurableBootstrapContext` and `DefaultBootstrapContext` moved from
 * `org.springframework.boot` to `org.springframework.boot.bootstrap`. The inspection only triggers when Spring Boot
 * 4+ is detected and the new class is resolvable on the classpath.
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide">Spring Boot 4.0 Migration Guide</a>
 */
class SpringBoot4BootstrapPackageInspection : LocalInspectionTool() {

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
        if (isWildcard || fqName == null) return
        val target = MOVED_TYPES[fqName] ?: return
        if (!SpringBootUtil.isAtLeastSpringBoot4(element)) return
        if (!isResolvable(holder.project, target)) return

        holder.registerProblem(
            element,
            message("explyt.spring.inspection.boot4.bootstrap"),
            ProblemHighlightType.LIKE_DEPRECATED,
            ReplaceImportFix(target)
        )
    }

    private fun isResolvable(project: Project, fqName: String): Boolean {
        return JavaPsiFacade.getInstance(project).findClass(fqName, GlobalSearchScope.allScope(project)) != null
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

private class ReplaceImportFix(private val targetFqName: String) : LocalQuickFix {

    override fun getFamilyName(): String = message("explyt.spring.inspection.boot4.bootstrap.fix")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        when (val element = descriptor.psiElement) {
            is KtImportDirective -> {
                val newImport = KtPsiFactory(project).createImportDirective(ImportPath(FqName(targetFqName), false))
                element.replaced(newImport)
            }

            is PsiImportStatement -> {
                val psiClass = JavaPsiFacade.getInstance(project)
                    .findClass(targetFqName, GlobalSearchScope.allScope(project)) ?: return
                val newImport = JavaPsiFacade.getInstance(project).elementFactory.createImportStatement(psiClass)
                element.replace(newImport)
            }
        }
    }
}
