/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
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

    fun mappingSourceOf(handler: PsiMethod, isMapped: (PsiMethod) -> Boolean): PsiMethod? =
        methodHierarchy(handler).firstOrNull(isMapped)

    fun annotatedParameter(parameter: PsiParameter, isAnnotated: (PsiParameter) -> Boolean): PsiParameter? =
        parameterHierarchy(parameter).firstOrNull(isAnnotated)

    fun bindingAnnotationOf(parameter: PsiParameter, isBinding: (PsiAnnotation) -> Boolean): PsiAnnotation? =
        parameterHierarchy(parameter).firstNotNullOfOrNull { candidate -> candidate.annotations.firstOrNull(isBinding) }

    private fun parameterHierarchy(parameter: PsiParameter): Sequence<PsiParameter> {
        val method = parameter.declarationScope as? PsiMethod ?: return sequenceOf(parameter)
        val index = method.parameterList.getParameterIndex(parameter).takeIf { it >= 0 } ?: return sequenceOf(parameter)
        return methodHierarchy(method).mapNotNull { it.parameterList.getParameter(index) }
    }

    private fun methodHierarchy(handler: PsiMethod): Sequence<PsiMethod> {
        val declaringClass = handler.containingClass ?: return sequenceOf(handler)
        return typeHierarchy(declaringClass)
            .flatMap { type -> if (type == declaringClass) sequenceOf(handler) else overriddenIn(type, handler) }
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
