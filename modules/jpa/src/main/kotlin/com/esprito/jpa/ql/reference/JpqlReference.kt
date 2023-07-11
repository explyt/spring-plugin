package com.esprito.jpa.ql.reference

import com.esprito.jpa.ql.psi.JpqlIdentifier
import com.esprito.jpa.ql.psi.JpqlRangeVariableDeclaration
import com.esprito.jpa.service.JpaEntitySearch
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

class JpqlReference(identifier: JpqlIdentifier) : PsiPolyVariantReferenceBase<JpqlIdentifier>(
    identifier,
    TextRange(0, identifier.textLength)
) {
    private val jpaEntitySearch by lazy { element.project.service<JpaEntitySearch>() }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (element.parent is JpqlRangeVariableDeclaration) {
            return resolveEntity()
        }

        return emptyArray()
    }

    private fun resolveEntity(): Array<ResolveResult> {
        val module = ModuleUtilCore.findModuleForPsiElement(element)
            ?: return emptyArray()

        val entities = jpaEntitySearch.loadEntities(module)

        return entities
            .asSequence()
            .filter { it.name == element.text }
            .mapNotNull { it.psiElement }
            .map { PsiElementResolveResult(it) }
            .toList()
            .toTypedArray()
    }
}