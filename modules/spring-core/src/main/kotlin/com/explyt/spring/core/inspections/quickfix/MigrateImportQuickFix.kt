/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.quickfix

import com.explyt.spring.core.SpringCoreBundle.message
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.base.psi.replaced
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.ImportPath

/**
 * Quick-fix that migrates an import of [oldFqName] to [newFqName] within the file containing the highlighted element.
 *
 * Used by the "package moved" Spring Boot 4 inspections, where the simple class name is unchanged: rewriting the
 * import is what actually migrates every usage in the file. The problem itself is highlighted on the visible type
 * usage (a field/parameter type), not on the import statement.
 */
class MigrateImportQuickFix(
    private val oldFqName: String,
    private val newFqName: String
) : LocalQuickFix {

    override fun getFamilyName(): String = message("explyt.spring.inspection.boot4.import.migrate.fix")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        when (val file = descriptor.psiElement.containingFile) {
            is KtFile -> migrateKotlinImport(project, file)
            is PsiJavaFile -> migrateJavaImport(project, file)
        }
    }

    private fun migrateKotlinImport(project: Project, file: KtFile) {
        val importDirective = file.importDirectives
            .firstOrNull { it.importedFqName?.asString() == oldFqName && !it.isAllUnder } ?: return
        val newImport = KtPsiFactory(project).createImportDirective(ImportPath(FqName(newFqName), false))
        importDirective.replaced(newImport)
    }

    private fun migrateJavaImport(project: Project, file: PsiJavaFile) {
        val importStatement = file.importList?.importStatements
            ?.firstOrNull { it.qualifiedName == oldFqName && !it.isOnDemand } ?: return
        val psiClass = JavaPsiFacade.getInstance(project)
            .findClass(newFqName, GlobalSearchScope.allScope(project)) ?: return
        val newImport = JavaPsiFacade.getInstance(project).elementFactory.createImportStatement(psiClass)
        importStatement.replace(newImport)
    }
}
