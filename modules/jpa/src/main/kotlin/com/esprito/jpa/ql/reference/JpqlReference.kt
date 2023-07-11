package com.esprito.jpa.ql.reference

import com.esprito.jpa.model.impl.JpaEntityPsi
import com.esprito.jpa.ql.psi.*
import com.esprito.jpa.service.JpaEntitySearch
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.parentOfType
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType

class JpqlReference(identifier: JpqlIdentifier) : PsiPolyVariantReferenceBase<JpqlIdentifier>(
    identifier,
    TextRange(0, identifier.textLength)
) {
    private val jpaEntitySearch by lazy { element.project.service<JpaEntitySearch>() }

    override fun getVariants(): Array<Any> {
        // todo
        return arrayOf(
            LookupElementBuilder.create("foo"),
            LookupElementBuilder.create("bar"),
            LookupElementBuilder.create("user"),
        )
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (element.parent is JpqlRangeVariableDeclaration) {
            return resolveEntity()
        }

        if (element.parent is JpqlReferenceExpression) {
            return resolveReference(incompleteCode)
        }

        return emptyArray()
    }

    private fun resolveReference(incompleteCode: Boolean): Array<ResolveResult> {
        val previousIdentifier = element.prevSibling?.prevSibling

        if (previousIdentifier == null) {
            return resolveToAlias(incompleteCode)
        } else {
            val parentReference = previousIdentifier.reference as? PsiPolyVariantReference
                ?: return emptyArray()

            return parentReference.deepResolve(incompleteCode)
                .mapNotNull { resolveChild(it) }
                .toTypedArray()
        }
    }

    private fun PsiPolyVariantReference.deepResolve(incompleteCode: Boolean): Array<PsiElement> {
        return multiResolve(incompleteCode)
            .asSequence()
            .filterIsInstance<PsiElementResolveResult>()
            .map { it.element }
            .flatMap { element ->
                when {
                    element is JpqlAliasDeclaration -> {
                        if (element.prevSibling?.prevSibling is JpqlIdentifier) {
                            (element.prevSibling?.prevSibling?.reference as? PsiPolyVariantReference)
                                ?.deepResolve(incompleteCode)
                                ?.asSequence()
                                ?: sequenceOf()
                        } else {
                            sequenceOf()
                        }
                    }

                    element.toUElementOfType<UClass>() != null -> sequenceOf(element)
                    else -> sequenceOf(element)
                }
            }.toList().toTypedArray()
    }

    private fun resolveChild(parent: PsiElement): ResolveResult? {
        val uClass = parent.toUElementOfType<UClass>()
            ?: return null

        val entityPsi = JpaEntityPsi(uClass)
            ?: return null

        return entityPsi.attributes
            .firstOrNull { it.name == element.text }
            ?.psiElement
            ?.let { PsiElementResolveResult(it) }
    }

    private fun resolveToAlias(incompleteCode: Boolean): Array<ResolveResult> {
        val subquery = element.parentOfType<JpqlSubquery>()
        // todo

        val selectStatement = element.parentOfType<JpqlSelectStatement>()
            ?: return emptyArray()

        val fromClause = selectStatement
            .fromClause

        return sequence {
            fromClause.identificationVariableDeclarationList
                .asSequence()
                .map { it.rangeVariableDeclaration }
                .mapNotNull { it.aliasDeclaration }
                .let { yieldAll(it) }

            fromClause.collectionMemberDeclarationList
                .asSequence()
                .map { it.aliasDeclaration }
                .let { yieldAll(it) }

            fromClause.identificationVariableDeclarationList
                .asSequence()
                .flatMap { it.joinExpressionList }
                .map { it.aliasDeclaration }
                .let { yieldAll(it) }
        }.filter { it.name == element.text }
            .map { PsiElementResolveResult(it) }
            .toList()
            .toTypedArray()
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