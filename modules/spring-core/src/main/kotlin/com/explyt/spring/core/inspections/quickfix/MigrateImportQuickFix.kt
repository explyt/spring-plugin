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
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

/**
 * Quick-fix that migrates a usage of [oldFqName] to [newFqName], where the simple class name is unchanged.
 *
 * Used by the "package moved" Spring Boot 4 inspections. The problem is highlighted on the visible type usage
 * (a field/parameter type), not on the import statement.
 *
 * When the file has an explicit (non-wildcard) import of [oldFqName], rewriting only its imported reference migrates
 * every usage in the file at once and keeps a Kotlin `as` alias intact. Otherwise - a fully qualified usage or a
 * wildcard import - the highlighted type reference itself is replaced through [ReplaceTypeQuickFix].
 */
class MigrateImportQuickFix(
    private val oldFqName: String,
    private val newFqName: String
) : LocalQuickFix {

    override fun getFamilyName(): String = message("explyt.spring.inspection.boot4.import.migrate.fix")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement?.takeIf { it.isValid } ?: return
        // Resolve in the edited file's scope: a sibling module may host the replacement while this module cannot
        // see it, and migrating to it would leave the file uncompilable.
        val psiClass = JavaPsiFacade.getInstance(project).findClass(newFqName, element.resolveScope) ?: return

        val migrated = when (val file = element.containingFile) {
            is KtFile -> migrateKotlinImport(project, file)
            is PsiJavaFile -> migrateJavaImport(project, file, psiClass)
            else -> false
        }
        if (!migrated) {
            ReplaceTypeQuickFix(newFqName).applyFix(project, descriptor)
        }
    }

    /**
     * Replaces only the imported reference, so a trailing `as` alias of the directive is preserved.
     */
    private fun migrateKotlinImport(project: Project, file: KtFile): Boolean {
        val importedReference = file.importDirectives
            .firstOrNull { !it.isAllUnder && it.importedFqName?.asString() == oldFqName }
            ?.importedReference ?: return false
        importedReference.replace(KtPsiFactory(project).createExpression(newFqName))
        return true
    }

    private fun migrateJavaImport(project: Project, file: PsiJavaFile, psiClass: PsiClass): Boolean {
        val importStatement = file.importList?.importStatements
            ?.firstOrNull { !it.isOnDemand && it.qualifiedName == oldFqName } ?: return false
        val factory = JavaPsiFacade.getInstance(project).elementFactory
        importStatement.replace(factory.createImportStatement(psiClass))
        return true
    }
}
