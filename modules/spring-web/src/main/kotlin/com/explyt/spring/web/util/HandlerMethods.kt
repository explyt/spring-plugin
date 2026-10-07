/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.web.SpringWebClasses
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.MethodSignatureUtil
import com.intellij.psi.util.PsiModificationTracker

object HandlerMethods {

    private val parameterMethodsKey = Key.create<CachedValue<List<PsiMethod>>>("explyt.web.parameterMethods")

    enum class HierarchyOrder { INTERFACES_FIRST, SUPERCLASS_FIRST }

    data class MappedMethod(val handler: PsiMethod, val mappingSource: PsiMethod)

    fun mappedMethods(
        controller: PsiClass,
        order: HierarchyOrder = HierarchyOrder.INTERFACES_FIRST,
        isMapped: (PsiMethod) -> Boolean,
    ): List<MappedMethod> =
        controller.allMethods.asSequence()
            .filter(isMapped)
            .map { mostSpecificMethod(it, controller) to it }
            .distinctBy { (handler, _) -> handler }
            .map { (handler, mapped) -> MappedMethod(handler, mappingSourceOf(handler, order, isMapped) ?: mapped) }
            .toList()

    fun mappedType(
        controller: PsiClass,
        order: HierarchyOrder = HierarchyOrder.INTERFACES_FIRST,
        isMapped: (PsiClass) -> Boolean,
    ): PsiClass? = typeHierarchy(controller, order).firstOrNull(isMapped)

    fun requestMappingPrefixes(controller: PsiClass, requestMappingMah: MetaAnnotationsHolder): List<String> =
        mappedType(controller) { it.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING) }
            ?.let { requestMappingMah.getAnnotationMemberValues(it, setOf("path", "value")) }.orEmpty()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }

    fun mostSpecificMethod(method: PsiMethod, controller: PsiClass): PsiMethod {
        val overrides = controller.findMethodsByName(method.name, true)
            .filter { MethodSignatureUtil.isSuperMethod(method, it) }
        return overrides.firstOrNull { candidate -> overrides.none { MethodSignatureUtil.isSuperMethod(candidate, it) } }
            ?: method
    }

    fun mappingSourceOf(
        handler: PsiMethod,
        order: HierarchyOrder = HierarchyOrder.INTERFACES_FIRST,
        isMapped: (PsiMethod) -> Boolean,
    ): PsiMethod? = methodHierarchy(handler, order).firstOrNull(isMapped)

    fun annotatedParameter(parameter: PsiParameter, isAnnotated: (PsiParameter) -> Boolean): PsiParameter? =
        parameterHierarchy(parameter).firstOrNull(isAnnotated)

    fun bindingAnnotationOf(parameter: PsiParameter, isBinding: (PsiAnnotation) -> Boolean): PsiAnnotation? =
        parameterHierarchy(parameter).firstNotNullOfOrNull { candidate -> candidate.annotations.firstOrNull(isBinding) }

    private fun parameterHierarchy(parameter: PsiParameter): Sequence<PsiParameter> {
        val method = parameter.declarationScope as? PsiMethod ?: return sequenceOf(parameter)
        val index = method.parameterList.getParameterIndex(parameter).takeIf { it >= 0 } ?: return sequenceOf(parameter)
        return parameterMethodsOf(method).asSequence().mapNotNull { it.parameterList.getParameter(index) }
    }

    private fun parameterMethodsOf(handler: PsiMethod): List<PsiMethod> =
        CachedValuesManager.getCachedValue(handler, parameterMethodsKey) {
            CachedValueProvider.Result.create(
                methodHierarchy(handler, HierarchyOrder.INTERFACES_FIRST).toList(),
                PsiModificationTracker.MODIFICATION_COUNT,
                ProjectRootModificationTracker.getInstance(handler.project),
            )
        }

    private fun methodHierarchy(handler: PsiMethod, order: HierarchyOrder): Sequence<PsiMethod> {
        val declaringClass = handler.containingClass ?: return sequenceOf(handler)
        return typeHierarchy(declaringClass, order).asSequence()
            .flatMap { type -> if (type == declaringClass) sequenceOf(handler) else overriddenIn(type, handler) }
    }

    private fun overriddenIn(type: PsiClass, handler: PsiMethod): Sequence<PsiMethod> =
        type.findMethodsByName(handler.name, false).asSequence()
            .filter { MethodSignatureUtil.isSuperMethod(it, handler) }

    private fun typeHierarchy(type: PsiClass, order: HierarchyOrder): List<PsiClass> {
        val visited = LinkedHashSet<PsiClass>()
        fun visit(current: PsiClass) {
            if (current.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT || !visited.add(current)) return
            when (order) {
                HierarchyOrder.INTERFACES_FIRST -> {
                    current.interfaces.forEach { visit(it) }
                    current.superClass?.let { visit(it) }
                }

                HierarchyOrder.SUPERCLASS_FIRST -> {
                    current.superClass?.let { visit(it) }
                    current.interfaces.forEach { visit(it) }
                }
            }
        }
        visit(type)
        return visited.toList()
    }
}
