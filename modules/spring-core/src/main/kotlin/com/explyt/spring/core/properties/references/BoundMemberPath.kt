/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.references

import com.explyt.spring.core.properties.references.ConfigurationPropertyListElementReference.Companion.INDEX_START
import com.explyt.spring.core.util.PropertyUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.PsiWildcardType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtil

/**
 * The member a configuration key addresses below a bound class, found by descending the way the binder does: a
 * member of an object, an index into a collection or array, a key of a map — in any nesting order.
 *
 * Map keys are arbitrary and indexes name no member, so neither is declared as a configuration property of its
 * own; the walk keeps the member that owns them. A path ending in a map key or an index therefore resolves to the
 * map or collection member, as a top-level map key resolves to its map.
 *
 * A map key is written plain (`models.free`), in brackets (`models[free]`) or as a YAML bracket key kept whole
 * with its dots (`[my.registration]`); an index is written `routes[0]`.
 */
object BoundMemberPath {

    fun resolve(module: Module, rootClassName: String, path: String): PsiMember? {
        val project = module.project
        val rootClass = JavaPsiFacade.getInstance(project)
            .findClass(rootClassName.replace('$', '.'), GlobalSearchScope.allScope(project)) ?: return null

        var position: Position = Position.Bean(rootClass)
        var member: PsiMember? = null
        for (segment in PropertyUtil.keySegments(path)) {
            val name = segment.substringBefore(INDEX_START)
            if (name.isNotEmpty()) {
                val current = position
                if (current is Position.Entries) {
                    position = current.next
                } else {
                    val owner = current as? Position.Bean ?: return null
                    val found = PropertyUtil.findPropertyMember(module, owner.psiClass, name) ?: return null
                    member = found
                    position = Position.of(found.boundType ?: return null)
                }
            }
            repeat(subscriptCount(segment)) {
                position = (position as? Position.Container)?.next ?: return null
            }
        }
        return member
    }

    private sealed interface Position {
        class Bean(val psiClass: PsiClass) : Position

        sealed class Container(private val itemType: PsiType) : Position {
            val next: Position get() = of(itemType)
        }

        class Elements(elementType: PsiType) : Container(elementType)

        class Entries(valueType: PsiType) : Container(valueType)

        data object Scalar : Position

        companion object {
            fun of(type: PsiType): Position {
                val bound = (type as? PsiWildcardType)?.extendsBound ?: type
                if (bound is PsiArrayType) return Elements(bound.componentType)
                PsiUtil.substituteTypeParameter(bound, CommonClassNames.JAVA_UTIL_MAP, 1, false)
                    ?.let { return Entries(it) }
                PsiUtil.extractIterableTypeParameter(bound, false)?.let { return Elements(it) }
                return PsiUtil.resolveClassInClassTypeOnly(bound)?.let(::Bean) ?: Scalar
            }
        }
    }

    private val PsiMember.boundType: PsiType?
        get() = when (this) {
            is PsiField -> type
            is PsiMethod -> parameterList.parameters.singleOrNull()?.type ?: returnType
            else -> null
        }

    private fun subscriptCount(segment: String): Int {
        var depth = 0
        return segment.count { char ->
            when (char) {
                INDEX_START -> depth++ == 0
                ']' -> {
                    if (depth > 0) depth--
                    false
                }

                else -> false
            }
        }
    }
}
