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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiParameter
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.MethodSignatureUtil
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UResolvable
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.toUElement

object HandlerMethods {

    private val parameterMethodsKey = Key.create<CachedValue<List<PsiMethod>>>("explyt.web.parameterMethods")
    private val MAPPING_PATH_ATTRIBUTES = setOf("path", "value")
    private val MAPPING_METHOD_ATTRIBUTES = setOf("method")
    private val MAPPING_PRODUCES_ATTRIBUTES = setOf("produces")
    private val MAPPING_CONSUMES_ATTRIBUTES = setOf("consumes")
    private val NO_MAPPING = MethodMapping(emptyList(), false, emptyList(), emptyList(), emptyList())

    enum class HierarchyOrder { INTERFACES_FIRST, SUPERCLASS_FIRST }

    data class MappedMethod(val handler: PsiMethod, val mappingSource: PsiMethod)

    data class MappedPath(val path: String, val anchor: PsiElement?)

    data class MethodMapping(
        val paths: List<MappedPath>,
        val pathsDeclared: Boolean,
        val methods: List<String>,
        val produces: List<String>,
        val consumes: List<String>,
    )

    fun MethodMapping.firstPathOrNull(): String? =
        if (pathsDeclared) paths.firstOrNull()?.path else ""

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

    fun requestMappingPrefixes(controller: PsiClass, requestMappingMah: MetaAnnotationsHolder): List<String> {
        val mappedType = mappedType(controller) { it.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING) }
            ?: return emptyList()
        val annotated = mappedType.toUElement() as? UAnnotated
            ?: return requestMappingMah.nearestAnnotation(mappedType)
                ?.let { requestMappingMah.getAnnotationMemberValues(it, MAPPING_PATH_ATTRIBUTES) }.orEmpty()
                .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
        return requestMappingMah.nearestAnnotation(annotated)
            ?.let { requestMappingMah.getAnnotationMemberValues(it, MAPPING_PATH_ATTRIBUTES) }.orEmpty()
            .mapNotNull { it.evaluateString() }
    }

    fun methodMapping(member: PsiModifierListOwner, requestMappingMah: MetaAnnotationsHolder): MethodMapping {
        val annotated = member.toUElement() as? UAnnotated ?: return declaredMethodMapping(member, requestMappingMah)
        val annotation = requestMappingMah.nearestAnnotation(annotated) ?: return NO_MAPPING
        fun values(attributes: Set<String>) = requestMappingMah.getAnnotationMemberValues(annotation, attributes)
        val nameAnchor = annotation.uastAnchor?.sourcePsi ?: annotation.sourcePsi
        val pathValues = values(MAPPING_PATH_ATTRIBUTES)
        return MethodMapping(
            paths = pathValues.mapNotNull { value ->
                value.evaluateString()?.let { MappedPath(it, anchorOf(value.sourcePsi, annotation.sourcePsi, nameAnchor)) }
            },
            pathsDeclared = pathValues.isNotEmpty(),
            methods = values(MAPPING_METHOD_ATTRIBUTES).mapNotNull(::requestMethodName),
            produces = values(MAPPING_PRODUCES_ATTRIBUTES).mapNotNull { it.evaluateString() },
            consumes = values(MAPPING_CONSUMES_ATTRIBUTES).mapNotNull { it.evaluateString() },
        )
    }

    private fun declaredMethodMapping(
        member: PsiModifierListOwner,
        requestMappingMah: MetaAnnotationsHolder,
    ): MethodMapping {
        val annotation = requestMappingMah.nearestAnnotation(member) ?: return NO_MAPPING
        fun values(attributes: Set<String>) = requestMappingMah.getAnnotationMemberValues(annotation, attributes)
        val pathValues = values(MAPPING_PATH_ATTRIBUTES)
        return MethodMapping(
            paths = pathValues.mapNotNull { value ->
                AnnotationUtil.getStringAttributeValue(value)
                    ?.let { MappedPath(it, anchorOf(value, annotation, annotation.nameReferenceElement)) }
            },
            pathsDeclared = pathValues.isNotEmpty(),
            methods = values(MAPPING_METHOD_ATTRIBUTES).map { it.text.split('.').last() },
            produces = values(MAPPING_PRODUCES_ATTRIBUTES).mapNotNull { AnnotationUtil.getStringAttributeValue(it) },
            consumes = values(MAPPING_CONSUMES_ATTRIBUTES).mapNotNull { AnnotationUtil.getStringAttributeValue(it) },
        )
    }

    private fun anchorOf(value: PsiElement?, useSite: PsiElement?, name: PsiElement?): PsiElement? =
        value?.takeIf { useSite != null && PsiTreeUtil.isAncestor(useSite, it, false) } ?: name

    private fun requestMethodName(value: UExpression): String? =
        ((value as? UResolvable)?.resolve() as? PsiEnumConstant)?.name
            ?: value.sourcePsi?.text?.split('.')?.last()

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
