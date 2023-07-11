package com.esprito.jpa.ql.reference

import com.esprito.jpa.model.impl.JpaEntityPsi
import com.esprito.jpa.ql.psi.*
import com.esprito.jpa.ql.psi.impl.JpqlElementFactory
import com.esprito.jpa.service.JpaEntitySearch
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType

class JpqlReference(identifier: JpqlIdentifier) : PsiPolyVariantReferenceBase<JpqlIdentifier>(
    identifier,
    TextRange(0, identifier.textLength)
) {
    private val jpaEntitySearch by lazy { element.project.service<JpaEntitySearch>() }

    override fun handleElementRename(newElementName: String): PsiElement {
        val newIdentifier = element.project.service<JpqlElementFactory>()
            .createIdentifier(newElementName)

        return element.replace(newIdentifier)
    }

    override fun getVariants(): Array<Any> {
        if (element.previousIdentifierInPath() == null) {
            val module = ModuleUtilCore.findModuleForPsiElement(element)
                ?: return emptyArray()

            return jpaEntitySearch.loadEntities(module)
                .mapNotNull { it.name }
                .map { LookupElementBuilder.create(it) }
                .toTypedArray()
        }

        // todo
        return arrayOf(
            LookupElementBuilder.create("foo"),
            LookupElementBuilder.create("bar"),
            LookupElementBuilder.create("user"),
        )
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (element.parent is JpqlEntityAccess) {
            return resolveEntity()
        }

        if (element.parent is JpqlReferenceExpression) {
            return resolveReference(incompleteCode)
        }

        return emptyArray()
    }

    private fun resolveReference(incompleteCode: Boolean): Array<ResolveResult> {
        val previousIdentifier = element.previousIdentifierInPath()

        if (previousIdentifier == null) {
            return resolveToAlias(incompleteCode)
        } else {
            val parentReference = previousIdentifier.reference
                ?: return emptyArray()

            return parentReference.deepResolve(incompleteCode)
                .mapNotNull { resolveChild(it) }
                .toTypedArray()
        }
    }

    private fun JpqlIdentifier.previousIdentifierInPath(): JpqlIdentifier? {
        val prevElement = PsiTreeUtil.skipMatching(
            this,
            { it.prevSibling },
            { it is PsiWhiteSpace || it.elementType == JpqlTypes.DOT }
        ) as? JpqlIdentifier ?: return null

        if (prevElement.parent.isEquivalentTo(parent)) {
            return prevElement
        }

        return null
    }

    private fun PsiPolyVariantReference.deepResolve(incompleteCode: Boolean): Array<PsiElement> {
        return multiResolve(incompleteCode)
            .asSequence()
            .mapNotNull { it.element }
            .flatMap { element ->
                when {
                    element.parent is JpqlAliasDeclaration -> {
                        ((element.parent as JpqlAliasDeclaration).referencedElement?.reference as? PsiPolyVariantReference)
                            ?.deepResolve(incompleteCode)
                            ?.asSequence() ?: sequenceOf()
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
            ?.let { JpaEntityAttributeResolveResult(it) }
    }

    private fun resolveToAlias(incompleteCode: Boolean): Array<ResolveResult> {
        val selectStatement = element.parentOfType<JpqlSelectStatement>()
        if (selectStatement != null) {
            val fromClause = selectStatement
                .fromClause

            return resolveAliasFromFrom(fromClause)
        }

        val deleteStatement = element.parentOfType<JpqlDeleteStatement>()
        if (deleteStatement != null) {
            val deleteClause = deleteStatement
                .deleteClause

            return resolveAliasFromDelete(deleteClause)
        }

        val updateStatement = element.parentOfType<JpqlUpdateStatement>()
        if (updateStatement != null) {
            val updateClause = updateStatement
                .updateClause

            return resolveAliasFromUpdate(updateClause)
        }


        return emptyArray()
    }

    private fun resolveAliasFromUpdate(updateClause: JpqlUpdateClause): Array<ResolveResult> = sequence {
        val aliasDeclaration = updateClause.entityAccess
            .aliasDeclaration

        if (aliasDeclaration != null) {
            yield(aliasDeclaration)
        }
    }.filter { it.name == element.text }
        .map { PsiElementResolveResult(it.identifier) }
        .toList()
        .toTypedArray()

    private fun resolveAliasFromDelete(deleteClause: JpqlDeleteClause): Array<ResolveResult> = sequence {
        val aliasDeclaration = deleteClause.entityAccess
            .aliasDeclaration

        if (aliasDeclaration != null) {
            yield(aliasDeclaration)
        }
    }.filter { it.name == element.text }
        .map { PsiElementResolveResult(it.identifier) }
        .toList()
        .toTypedArray()

    private fun resolveAliasFromFrom(fromClause: JpqlFromClause): Array<ResolveResult> =
        sequence {
            fromClause.identificationVariableDeclarationList
                .asSequence()
                .map { it.entityAccess }
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
            .map { PsiElementResolveResult(it.identifier) }
            .toList()
            .toTypedArray()

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