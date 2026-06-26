/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.util.SpringBootUtil
import com.explyt.util.SpringBaseClasses.CORE_ENVIRONMENT
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.base.psi.replaced
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.ImportPath

/**
 * Reports legacy `javax.*` Jakarta EE imports in Spring Boot 3+ projects and offers a quick-fix that rewrites
 * them to the corresponding `jakarta.*` namespace.
 *
 * Spring Boot 3 / Spring Framework 6 moved from Java EE (`javax.*`) to Jakarta EE 9+ (`jakarta.*`). The inspection
 * only triggers when Spring Boot 3+ is detected and the matching `jakarta.*` target is resolvable on the classpath,
 * so it never proposes a migration the project cannot satisfy.
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide">Spring Boot 3.0 Migration Guide</a>
 */
class SpringBoot3JakartaMigrationInspection : LocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        return LibraryClassCache.searchForLibraryClass(file.project, CORE_ENVIRONMENT) != null
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                when (element) {
                    is KtImportDirective -> inspectKotlinImport(element, holder)
                    is PsiImportStatement -> inspectJavaImport(element, holder)
                }
            }
        }
    }

    private fun inspectKotlinImport(importDirective: KtImportDirective, holder: ProblemsHolder) {
        val fqName = importDirective.importedFqName?.asString() ?: return
        val isWildcard = importDirective.isAllUnder
        val target = migrate(fqName, isWildcard) ?: return
        if (!SpringBootUtil.isAtLeastSpringBoot3(importDirective)) return
        if (!isMigrationTargetResolvable(holder.project, target, isWildcard)) return

        holder.registerProblem(
            importDirective,
            message("explyt.spring.inspection.jakarta.migration"),
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            ReplaceWithJakartaImportFix(target, isWildcard)
        )
    }

    private fun inspectJavaImport(importStatement: PsiImportStatement, holder: ProblemsHolder) {
        val fqName = importStatement.qualifiedName ?: return
        val isWildcard = importStatement.isOnDemand
        val target = migrate(fqName, isWildcard) ?: return
        if (!SpringBootUtil.isAtLeastSpringBoot3(importStatement)) return
        if (!isMigrationTargetResolvable(holder.project, target, isWildcard)) return

        holder.registerProblem(
            importStatement,
            message("explyt.spring.inspection.jakarta.migration"),
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            ReplaceWithJakartaImportFix(target, isWildcard)
        )
    }

    private fun isMigrationTargetResolvable(project: Project, target: String, isWildcard: Boolean): Boolean {
        val facade = JavaPsiFacade.getInstance(project)
        return if (isWildcard) {
            facade.findPackage(target) != null
        } else {
            facade.findClass(target, GlobalSearchScope.allScope(project)) != null
        }
    }

    companion object {
        // Package-prefix migrations (without trailing dot). Matched as the package itself (wildcard import) or as a
        // prefix of a fully qualified class name.
        private val PACKAGE_PREFIXES: List<Pair<String, String>> = listOf(
            "javax.servlet" to "jakarta.servlet",
            "javax.validation" to "jakarta.validation",
            "javax.persistence" to "jakarta.persistence",
            "javax.transaction" to "jakarta.transaction",
            "javax.ws.rs" to "jakarta.ws.rs",
            "javax.inject" to "jakarta.inject",
        )

        // `javax.annotation` also hosts JSR-305 (@Nonnull/@Nullable) which is NOT a Jakarta EE artifact, so only the
        // explicit Jakarta EE annotations are migrated and wildcard `javax.annotation.*` imports are left untouched.
        private val ANNOTATION_FQNS: Set<String> = setOf(
            "javax.annotation.PostConstruct",
            "javax.annotation.PreDestroy",
            "javax.annotation.Resource",
            "javax.annotation.Resources",
            "javax.annotation.Generated",
        )

        /**
         * Returns the `jakarta.*` replacement for a legacy `javax.*` [fqName], or `null` when it must not be migrated.
         *
         * @param isWildcard `true` when [fqName] denotes a package (wildcard / on-demand import) rather than a class.
         */
        fun migrate(fqName: String, isWildcard: Boolean): String? {
            for ((prefix, replacement) in PACKAGE_PREFIXES) {
                if (fqName == prefix || fqName.startsWith("$prefix.")) {
                    return replacement + fqName.substring(prefix.length)
                }
            }
            if (!isWildcard && fqName in ANNOTATION_FQNS) {
                return "jakarta.annotation." + fqName.substringAfterLast('.')
            }
            return null
        }
    }
}

private class ReplaceWithJakartaImportFix(
    private val targetFqName: String,
    private val isWildcard: Boolean
) : LocalQuickFix {

    override fun getName(): String =
        message("explyt.spring.inspection.jakarta.migration.fix", presentableImport())

    override fun getFamilyName(): String = message("explyt.spring.inspection.jakarta.migration.fix.family")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        when (val element = descriptor.psiElement) {
            is KtImportDirective -> replaceKotlinImport(project, element)
            is PsiImportStatement -> replaceJavaImport(project, element)
        }
    }

    private fun replaceKotlinImport(project: Project, importDirective: KtImportDirective) {
        val importPath = ImportPath(FqName(targetFqName), isWildcard)
        val newImport = KtPsiFactory(project).createImportDirective(importPath)
        importDirective.replaced(newImport)
    }

    private fun replaceJavaImport(project: Project, importStatement: PsiImportStatement) {
        val factory = JavaPsiFacade.getInstance(project).elementFactory
        val newImport = if (isWildcard) {
            factory.createImportStatementOnDemand(targetFqName)
        } else {
            val psiClass = JavaPsiFacade.getInstance(project)
                .findClass(targetFqName, GlobalSearchScope.allScope(project)) ?: return
            factory.createImportStatement(psiClass)
        }
        importStatement.replace(newImport)
    }

    private fun presentableImport(): String = if (isWildcard) "$targetFqName.*" else targetFqName
}
