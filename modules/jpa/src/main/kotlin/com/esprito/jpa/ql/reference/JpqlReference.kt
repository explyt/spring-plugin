package com.esprito.jpa.ql.reference

import com.esprito.jpa.model.JpaEntity
import com.esprito.jpa.model.JpaEntityAttributeType
import com.esprito.jpa.model.impl.JpaEntityPsi
import com.esprito.jpa.ql.psi.*
import com.esprito.jpa.ql.psi.impl.JpqlElementFactory
import com.esprito.jpa.service.JpaEntitySearch
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
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
    private val jpaEntitySearch by lazy { JpaEntitySearch.getInstance(element.project) }

    override fun handleElementRename(newElementName: String): PsiElement {
        val newIdentifier = JpqlElementFactory.getInstance(element.project)
            .createIdentifier(newElementName)

        return element.replace(newIdentifier)
    }

    override fun getVariants(): Array<Any> {
        val previousIdentifier = element.previousIdentifierInPath()
        if (element.parent is JpqlEntityAccess && previousIdentifier == null) {
            return getEntityVariants()
        }

        if (previousIdentifier != null) {
            return previousIdentifier
                .multiResolve(true)
                .flatMap(::loadSubVariants)
                .toTypedArray()
        }

        return emptyArray()
    }

    private val JpaEntity.variants
        get() = attributes.mapNotNull { it.name }.map {
            LookupElementBuilder.create(it)
                .withIcon(AllIcons.Nodes.Field)
        }

    private fun loadSubVariants(resolveResult: ResolveResult): List<Any> = when (resolveResult) {
        is JpaEntityResolveResult -> resolveResult.entity.variants

        is JpaEntityAttributeResolveResult -> {
            val type = resolveResult.entityAttribute.type

            if (type is JpaEntityAttributeType.Entity) {
                type.jpaEntity.variants
            } else {
                emptyList()
            }
        }

        is JpqlAliasResolveResult -> {
            resolveResult.alias.referencedElement.multiResolve(true)
                .flatMap { loadSubVariants(it) }
        }

        else -> emptyList()
    }

    private fun getEntityVariants(): Array<Any> {
        val module = ModuleUtilCore.findModuleForPsiElement(element)
            ?: return emptyArray()

        return jpaEntitySearch.loadEntities(module)
            .filter { it.isPersistent }
            .mapNotNull { it.name }
            .sorted()
            .map {
                LookupElementBuilder.create(it)
                    .withIcon(AllIcons.Nodes.ModelClass)
            }
            .toTypedArray()
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

            val resolveResults = parentReference.multiResolve(incompleteCode)

            return resolveResults.flatMap {
                subResolve(it, incompleteCode)
            }.toTypedArray()
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

    private fun subResolve(resolveResult: ResolveResult, incompleteCode: Boolean): List<ResolveResult> {
        when (resolveResult) {
            is JpaEntityResolveResult -> {
                val entity = resolveResult.entity

                return resolveEntityAttributes(entity)
            }

            is JpaEntityAttributeResolveResult -> {
                val entityAttributeType = resolveResult
                    .entityAttribute.type

                if (entityAttributeType is JpaEntityAttributeType.Entity) {
                    return resolveEntityAttributes(entityAttributeType.jpaEntity)
                }


                return emptyList()
            }

            is JpqlAliasResolveResult -> {
                return resolveResult.alias.referencedElement
                    .multiResolve(incompleteCode)
                    .flatMap { subResolve(it, incompleteCode) }
            }
        }

        return emptyList()
    }

    private fun resolveEntityAttributes(entity: JpaEntity): List<JpaEntityAttributeResolveResult> {
        val entityAttributeResult = entity
            .attributes
            .firstOrNull { it.name == element.text }
            ?.let { JpaEntityAttributeResolveResult(it) }

        return listOfNotNull(entityAttributeResult)
    }

    private fun resolveChild(parent: PsiElement): ResolveResult? {
        val uClass = parent.toUElementOfType<UClass>()
            ?: return null

        val entityPsi = JpaEntityPsi(uClass)

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

    private fun PsiElement?.multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (this == null)
            return emptyArray()

        val reference = reference ?: return emptyArray()

        if (reference is PsiPolyVariantReference) {
            return reference.multiResolve(incompleteCode)
        }

        val singleResolveResult = reference.resolve()
            ?.let(::PsiElementResolveResult)
            ?: return emptyArray()
        return arrayOf(singleResolveResult)
    }

    private fun resolveAliasFromUpdate(updateClause: JpqlUpdateClause): Array<ResolveResult> = sequence {
        val aliasDeclaration = updateClause.entityAccess
            .aliasDeclaration

        if (aliasDeclaration != null) {
            yield(aliasDeclaration)
        }
    }.filter { it.name == element.text }
        .map { JpqlAliasResolveResult(it) }
        .toList()
        .toTypedArray()

    private fun resolveAliasFromDelete(deleteClause: JpqlDeleteClause): Array<ResolveResult> = sequence {
        val aliasDeclaration = deleteClause.entityAccess
            .aliasDeclaration

        if (aliasDeclaration != null) {
            yield(aliasDeclaration)
        }
    }.filter { it.name == element.text }
        .map { JpqlAliasResolveResult(it) }
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
            .map { JpqlAliasResolveResult(it) }
            .toList()
            .toTypedArray()

    private fun resolveEntity(): Array<ResolveResult> {
        val module = ModuleUtilCore.findModuleForPsiElement(element)
            ?: return emptyArray()

        val entities = jpaEntitySearch.loadEntities(module)

        return entities
            .asSequence()
            .filter { it.name == element.text }
            .map { JpaEntityResolveResult(it) }
            .toList()
            .toTypedArray()
    }
}