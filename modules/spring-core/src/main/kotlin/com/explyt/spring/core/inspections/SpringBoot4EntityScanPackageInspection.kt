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
 * Reports the legacy `@EntityScan` import location in Spring Boot 4+ projects and offers a quick-fix that rewrites
 * the import to the new package.
 *
 * In Spring Boot 4.0 `@EntityScan` moved from `org.springframework.boot.autoconfigure.domain` to
 * `org.springframework.boot.persistence.autoconfigure`. The inspection only triggers when Spring Boot 4+ is detected
 * and the new class is resolvable on the classpath, so it never proposes a migration the project cannot satisfy.
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide">Spring Boot 4.0 Migration Guide</a>
 */
class SpringBoot4EntityScanPackageInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                when (element) {
                    is KtImportDirective -> {
                        val fqName = element.importedFqName?.asString() ?: return
                        inspectImport(element, fqName, element.isAllUnder, holder)
                    }

                    is PsiImportStatement -> {
                        val fqName = element.qualifiedName ?: return
                        inspectImport(element, fqName, element.isOnDemand, holder)
                    }
                }
            }
        }
    }

    private fun inspectImport(element: PsiElement, fqName: String, isWildcard: Boolean, holder: ProblemsHolder) {
        if (isWildcard) return
        if (fqName != OLD_ENTITY_SCAN) return
        if (!SpringBootUtil.isAtLeastSpringBoot4(element)) return
        if (!isTargetResolvable(holder.project)) return

        holder.registerProblem(
            element,
            message("explyt.spring.inspection.boot4.entityscan"),
            ProblemHighlightType.LIKE_DEPRECATED,
            ReplaceEntityScanImportFix()
        )
    }

    private fun isTargetResolvable(project: Project): Boolean {
        return JavaPsiFacade.getInstance(project)
            .findClass(NEW_ENTITY_SCAN, GlobalSearchScope.allScope(project)) != null
    }

    companion object {
        const val OLD_ENTITY_SCAN = "org.springframework.boot.autoconfigure.domain.EntityScan"
        const val NEW_ENTITY_SCAN = "org.springframework.boot.persistence.autoconfigure.EntityScan"
    }
}

private class ReplaceEntityScanImportFix : LocalQuickFix {

    override fun getFamilyName(): String = message("explyt.spring.inspection.boot4.entityscan.fix")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        when (val element = descriptor.psiElement) {
            is KtImportDirective -> {
                val importPath = ImportPath(FqName(SpringBoot4EntityScanPackageInspection.NEW_ENTITY_SCAN), false)
                val newImport = KtPsiFactory(project).createImportDirective(importPath)
                element.replaced(newImport)
            }

            is PsiImportStatement -> {
                val psiClass = JavaPsiFacade.getInstance(project)
                    .findClass(SpringBoot4EntityScanPackageInspection.NEW_ENTITY_SCAN, GlobalSearchScope.allScope(project))
                    ?: return
                val newImport = JavaPsiFacade.getInstance(project).elementFactory.createImportStatement(psiClass)
                element.replace(newImport)
            }
        }
    }
}
