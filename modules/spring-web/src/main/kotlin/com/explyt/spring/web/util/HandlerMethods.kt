/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.MethodSignatureUtil

object HandlerMethods {

    data class MappedMethod(val handler: PsiMethod, val mappingSource: PsiMethod)

    fun mappedMethods(controller: PsiClass, isMapped: (PsiMethod) -> Boolean): List<MappedMethod> =
        controller.allMethods.asSequence()
            .filter(isMapped)
            .map { mostSpecificMethod(it, controller) to it }
            .distinctBy { (handler, _) -> handler }
            .map { (handler, mapped) -> MappedMethod(handler, mappingSourceOf(handler, isMapped) ?: mapped) }
            .toList()

    fun mappedType(controller: PsiClass, isMapped: (PsiClass) -> Boolean): PsiClass? =
        typeHierarchy(controller).firstOrNull(isMapped)

    fun mostSpecificMethod(method: PsiMethod, controller: PsiClass): PsiMethod {
        val overrides = controller.findMethodsByName(method.name, true)
            .filter { MethodSignatureUtil.isSuperMethod(method, it) }
        return overrides.firstOrNull { candidate -> overrides.none { MethodSignatureUtil.isSuperMethod(candidate, it) } }
            ?: method
    }

    fun mappingSourceOf(handler: PsiMethod, isMapped: (PsiMethod) -> Boolean): PsiMethod? {
        val declaringClass = handler.containingClass ?: return handler.takeIf(isMapped)
        return typeHierarchy(declaringClass)
            .flatMap { type -> if (type == declaringClass) sequenceOf(handler) else overriddenIn(type, handler) }
            .firstOrNull(isMapped)
    }

    private fun overriddenIn(type: PsiClass, handler: PsiMethod): Sequence<PsiMethod> =
        type.findMethodsByName(handler.name, false).asSequence()
            .filter { MethodSignatureUtil.isSuperMethod(it, handler) }

    private fun typeHierarchy(type: PsiClass, visited: MutableSet<PsiClass> = mutableSetOf()): Sequence<PsiClass> =
        sequence {
            if (!visited.add(type) || type.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT) return@sequence
            yield(type)
            for (superInterface in type.interfaces) yieldAll(typeHierarchy(superInterface, visited))
            type.superClass?.let { yieldAll(typeHierarchy(it, visited)) }
        }
}
