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
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtVisitorVoid

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
                && SpringBootUtil.isAtLeastSpringBoot3(file)
                && isAnyLegacyJavaxAvailable(file)
    }

    /**
     * A fresh Spring Boot 3 project has no legacy `javax.*` Jakarta EE classes on the classpath; in that case the
     * inspection is skipped entirely.
     */
    private fun isAnyLegacyJavaxAvailable(file: PsiFile): Boolean {
        val facade = JavaPsiFacade.getInstance(file.project)
        return PACKAGE_PREFIXES.any { (oldPackage, _) -> facade.findPackage(oldPackage) != null }
                || ANNOTATION_FQNS.any { facade.findClass(it, file.resolveScope) != null }
    }

    /**
     * Only import statements are inspected, so the visitor is narrowed to the two languages that have them: any
     * other file gets [PsiElementVisitor.EMPTY_VISITOR] instead of a walk over every PSI node.
     */
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = when (holder.file) {
        is PsiJavaFile -> object : JavaElementVisitor() {
            override fun visitImportStatement(statement: PsiImportStatement) = inspectJavaImport(statement, holder)
        }

        is KtFile -> object : KtVisitorVoid() {
            override fun visitImportDirective(importDirective: KtImportDirective) =
                inspectKotlinImport(importDirective, holder)
        }

        else -> PsiElementVisitor.EMPTY_VISITOR
    }

    private fun inspectKotlinImport(importDirective: KtImportDirective, holder: ProblemsHolder) {
        val fqName = importDirective.importedFqName?.asString() ?: return
        val isWildcard = importDirective.isAllUnder
        val target = migrate(fqName, isWildcard) ?: return
        if (!isMigrationTargetResolvable(importDirective, target, isWildcard)) return

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
        if (!isMigrationTargetResolvable(importStatement, target, isWildcard)) return

        holder.registerProblem(
            importStatement,
            message("explyt.spring.inspection.jakarta.migration"),
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            ReplaceWithJakartaImportFix(target, isWildcard)
        )
    }

    /**
     * Resolved in the edited file's scope: a sibling module may host the `jakarta.*` target while this module cannot
     * see it, and migrating to it would leave the file uncompilable.
     */
    private fun isMigrationTargetResolvable(context: PsiElement, target: String, isWildcard: Boolean): Boolean {
        val facade = JavaPsiFacade.getInstance(context.project)
        return if (isWildcard) {
            facade.findPackage(target) != null
        } else {
            facade.findClass(target, context.resolveScope) != null
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

    /**
     * Replaces only the imported reference so the directive keeps its `.*` suffix and any `as` alias.
     */
    private fun replaceKotlinImport(project: Project, importDirective: KtImportDirective) {
        val importedReference = importDirective.importedReference ?: return
        importedReference.replace(KtPsiFactory(project).createExpression(targetFqName))
    }

    private fun replaceJavaImport(project: Project, importStatement: PsiImportStatement) {
        val facade = JavaPsiFacade.getInstance(project)
        val newImport = if (isWildcard) {
            facade.elementFactory.createImportStatementOnDemand(targetFqName)
        } else {
            val psiClass = facade.findClass(targetFqName, importStatement.resolveScope) ?: return
            facade.elementFactory.createImportStatement(psiClass)
        }
        importStatement.replace(newImport)
    }

    private fun presentableImport(): String = if (isWildcard) "$targetFqName.*" else targetFqName
}
