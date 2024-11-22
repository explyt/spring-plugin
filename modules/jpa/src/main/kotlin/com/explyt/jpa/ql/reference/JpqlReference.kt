/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.jpa.ql.reference

import com.explyt.jpa.JpaIcons
import com.explyt.jpa.model.JpaEntity
import com.explyt.jpa.model.JpaEntityAttributeType
import com.explyt.jpa.ql.psi.*
import com.explyt.jpa.ql.psi.impl.JpqlElementFactory
import com.explyt.jpa.service.JpaEntitySearch
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.RecursionManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.impl.source.resolve.ResolveCache.PolyVariantResolver
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType

class JpqlReference(identifier: JpqlIdentifier) : PsiPolyVariantReferenceBase<JpqlIdentifier>(
    identifier,
    TextRange(0, identifier.textLength)
) {
    private val resolver = PolyVariantResolver<PsiPolyVariantReferenceBase<*>>
    { ref: PsiPolyVariantReferenceBase<*>,
      incompleteCode: Boolean ->
        (ref as JpqlReference).resolveInner(incompleteCode)
    }

    private val jpaEntitySearch by lazy { JpaEntitySearch.getInstance(element.project) }

    override fun handleElementRename(newElementName: String): PsiElement {
        val newIdentifier = JpqlElementFactory.getInstance(element.project)
            .createIdentifier(newElementName)

        return element.replace(newIdentifier)
    }

    override fun getVariants(): Array<Any> {
        val previousIdentifier = element.previousIdentifierInPath()
        if (previousIdentifier == null && isEntityReference()) {
            return getEntityVariants()
        }

        if (previousIdentifier != null) {
            return getSubReferenceVariants(previousIdentifier)
        }

        if (element.parentOfType<JpqlInsertFields>() != null) {
            return getInsertFieldVariants()
        }

        return loadAllAvailableAliases(element)
            .flatMap {
                if (it is EmptyJpqlAliasDeclaration) {
                    getSubReferenceVariants(it.referencedElement).asSequence()
                } else {
                    var lookupElementBuilder = LookupElementBuilder.create(it)
                        .withIcon(JpaIcons.Alias)

                    val referencedElement = it.referencedElement
                    if (referencedElement != null) {
                        lookupElementBuilder = lookupElementBuilder
                            .withTypeText(referencedElement.text, true)
                    }

                    sequenceOf(lookupElementBuilder)
                }
            }

            .toList()
            .toTypedArray()
    }

    private fun getSubReferenceVariants(previousIdentifier: JpqlIdentifier) = previousIdentifier
        .multiResolve(true)
        .flatMap(::loadSubVariants)
        .toTypedArray()

    private fun getInsertFieldVariants(): Array<Any> {
        return element.parentOfType<JpqlInsertStatement>()
            ?.entityAccess
            ?.identifier
            ?.multiResolve(true)
            ?.flatMap(::loadSubVariants)
            ?.toTypedArray()
            ?: emptyArray()
    }

    private val JpaEntity.variants
        get() = attributes.mapNotNull {
            val name = it.name ?: return@mapNotNull null
            LookupElementBuilder.create(name)
                .withIcon(AllIcons.Nodes.Field)
                .withTypeText(createAttributeTypeText(it.type), true)
        }

    private fun createAttributeTypeText(type: JpaEntityAttributeType): String? = when (type) {
        is JpaEntityAttributeType.Entity -> type.jpaEntity.name
        is JpaEntityAttributeType.Scalar -> type.psiType.presentableText
        JpaEntityAttributeType.Unknown -> null
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

    private fun resolveInner(incompleteCode: Boolean): Array<ResolveResult> {
        if (isEntityReference()) {
            return resolveToEntity()
        }

        if (element.parent is JpqlReferenceExpression || element.parent is JpqlObjectExpression || element.parent is JpqlInsertFields) {
            return resolveReference(incompleteCode)
        }

        return emptyArray()
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return ResolveCache.getInstance(element.project)
            .resolveWithCaching(
                this,
                resolver,
                false,
                incompleteCode
            )
    }

    private fun isEntityReference(): Boolean {
        if (element.parent is JpqlEntityAccess)
            return true

        if ((element.parent.parent as? JpqlInExpression)?.expressionList?.firstOrNull() is JpqlTypeExpression) {
            return true
        }

        return false
    }

    private fun resolveReference(incompleteCode: Boolean): Array<ResolveResult> {
        val previousIdentifier = element.previousIdentifierInPath()

        if (previousIdentifier != null) {
            return resolveToSubReferenceField(previousIdentifier, incompleteCode)
        }

        if (element.parent is JpqlInsertFields) {
            return resolveToInsertFields(incompleteCode)
        } else {
            return resolveToAliasesOrNoAliasFields()
        }
    }

    private fun resolveToInsertFields(incompleteCode: Boolean): Array<ResolveResult> {
        return element.parentOfType<JpqlInsertStatement>()
            ?.entityAccess
            ?.identifier
            ?.multiResolve(incompleteCode)
            ?.flatMap {
                subResolve(it, incompleteCode)
            }?.toTypedArray()
            ?: return emptyArray()
    }

    private fun resolveToSubReferenceField(
        previousIdentifier: JpqlIdentifier,
        incompleteCode: Boolean
    ): Array<ResolveResult> {
        val parentReference = previousIdentifier.reference
            ?: return emptyArray()

        val resolveResults = parentReference.multiResolve(incompleteCode)

        return resolveResults.flatMap {
            subResolve(it, incompleteCode)
        }.toTypedArray()
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
        val element = resolveResult.element ?: return emptyList()

        return RecursionManager.doPreventingRecursion(element, false) {
            return@doPreventingRecursion when (resolveResult) {
                is JpaEntityResolveResult -> {
                    val entity = resolveResult.entity

                    resolveToEntityAttributes(entity)
                }

                is JpaEntityAttributeResolveResult -> {
                    val entityAttributeType = resolveResult
                        .entityAttribute.type

                    if (entityAttributeType is JpaEntityAttributeType.Entity) {
                        resolveToEntityAttributes(entityAttributeType.jpaEntity)
                    } else {
                        emptyList()
                    }
                }

                is JpqlAliasResolveResult -> {
                    resolveResult.alias.referencedElement
                        .multiResolve(incompleteCode)
                        .flatMap { subResolve(it, incompleteCode) }
                }

                else -> emptyList()
            }
        } ?: emptyList()
    }

    private fun resolveToEntityAttributes(entity: JpaEntity): List<JpaEntityAttributeResolveResult> {
        val entityAttributeResult = entity
            .attributes
            .firstOrNull { it.name == element.text }
            ?.let { JpaEntityAttributeResolveResult(it) }

        return listOfNotNull(entityAttributeResult)
    }

    private fun resolveToAliasesOrNoAliasFields(): Array<ResolveResult> {
        val visited = mutableSetOf<String>() // names shadowing support

        val myName = element.text

        val aliases = loadAllAvailableAliases(element)
            .filter { it is EmptyJpqlAliasDeclaration || it.name == myName }
            .filter {
                if (it is EmptyJpqlAliasDeclaration) {
                    return@filter true
                }
                val aliasName = it.name

                if (aliasName in visited) {
                    false
                } else {
                    visited.add(aliasName)
                    true
                }
            }

        val result = mutableListOf<ResolveResult>()
        for (alias in aliases) {
            if (alias is EmptyJpqlAliasDeclaration) {
                result.addAll(resolveToSubReferenceField(alias.referencedElement, false))
            } else {
                result.add(JpqlAliasResolveResult(alias))
            }
        }

        return result.toTypedArray()
    }

    private fun loadAllAvailableAliases(element: PsiElement): Sequence<JpqlAliasDeclaration> = sequence {
        val subQueryStatement = element.parentOfType<JpqlSubquery>()
        if (subQueryStatement != null) {
            val fromClause = subQueryStatement
                .subqueryFromClause

            yieldAll(loadAliasesInSubQueryFromClause(fromClause))

            yieldAll(loadAllAvailableAliases(subQueryStatement))

            return@sequence
        }

        val selectStatement = element.parentOfType<JpqlSelectStatement>()
        if (selectStatement != null) {
            val fromClause = selectStatement
                .fromClause

            yieldAll(loadAliasesInFromClause(fromClause))

            return@sequence
        }

        val deleteStatement = element.parentOfType<JpqlDeleteStatement>()
        if (deleteStatement != null) {
            val deleteClause = deleteStatement
                .deleteClause

            yieldAll(loadAliasesInDeleteClause(deleteClause))

            return@sequence
        }

        val updateStatement = element.parentOfType<JpqlUpdateStatement>()
        if (updateStatement != null) {
            val updateClause = updateStatement
                .updateClause

            yieldAll(loadAliasedInUpdateClause(updateClause))

            return@sequence
        }
    }

    private fun PsiElement?.multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (this == null)
            return emptyArray()

        val reference = if (this is JpqlReferenceExpression) {
            (lastChild as? JpqlIdentifier)?.reference
        } else {
            reference
        }

        if (reference == null) {
            return emptyArray()
        }

        if (reference is PsiPolyVariantReference) {
            return reference.multiResolve(incompleteCode)
        }

        val singleResolveResult = reference.resolve()
            ?.let(::PsiElementResolveResult)
            ?: return emptyArray()

        return arrayOf(singleResolveResult)
    }

    private fun loadAliasedInUpdateClause(updateClause: JpqlUpdateClause) = sequence {
        val aliasDeclaration = updateClause.entityAccess
            .aliasDeclaration

        if (aliasDeclaration != null) {
            yield(aliasDeclaration)
        }
    }


    private fun loadAliasesInDeleteClause(deleteClause: JpqlDeleteClause) = sequence {
        val aliasDeclaration = deleteClause.entityAccess
            .aliasDeclaration

        if (aliasDeclaration != null) {
            yield(aliasDeclaration)
        }
    }


    private fun loadAliasesInSubQueryFromClause(fromClause: JpqlSubqueryFromClause): Sequence<JpqlAliasDeclaration> =
        sequence {
            fromClause.identificationVariableDeclarationList
                .asSequence()
                .map { it.entityAccess }
                .map { it.aliasDeclaration ?: EmptyJpqlAliasDeclaration(it) }
                .let { yieldAll(it) }

            fromClause.collectionMemberDeclarationList
                .asSequence()
                .map { it.aliasDeclaration }
                .let { yieldAll(it) }

            fromClause.identificationVariableDeclarationList
                .asSequence()
                .flatMap { it.joinExpressionList }
                .mapNotNull { it.aliasDeclaration /*do not use EmptyJpqlAliasDeclaration for join*/ }
                .let { yieldAll(it) }
        }


    private fun loadAliasesInFromClause(fromClause: JpqlFromClause): Sequence<JpqlAliasDeclaration> = sequence {
        fromClause.identificationVariableDeclarationList
            .asSequence()
            .map { it.entityAccess }
            .map { it.aliasDeclaration ?: EmptyJpqlAliasDeclaration(it) }
            .let { yieldAll(it) }

        fromClause.collectionMemberDeclarationList
            .asSequence()
            .map { it.aliasDeclaration }
            .let { yieldAll(it) }

        fromClause.identificationVariableDeclarationList
            .asSequence()
            .flatMap { it.joinExpressionList }
            .mapNotNull { it.aliasDeclaration  /*do not use EmptyJpqlAliasDeclaration for join */ }
            .let { yieldAll(it) }
    }

    private fun resolveToEntity(): Array<ResolveResult> {
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

private class EmptyJpqlAliasDeclaration(private val parent: JpqlEntityAccess) : FakePsiElement(), JpqlAliasDeclaration {
    override fun getParent(): PsiElement = parent

    override fun getName(): String = ""

    override fun getNameIdentifier(): PsiElement? = null

    override fun getIdentifier(): JpqlIdentifier {
        throw ProcessCanceledException()
    }

    override fun getReferencedElement(): JpqlIdentifier {
        return parent.identifier
    }
}