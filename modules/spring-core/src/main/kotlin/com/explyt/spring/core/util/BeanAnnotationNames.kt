/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.AliasUtils
import com.explyt.util.ExplytAnnotationUtil.getStringMemberValues
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.resolveUAnnotationType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.toUElement

object BeanAnnotationNames {
    private const val VALUE = "value"
    private const val NAME = "name"
    private const val LAST_SPRING_MAJOR_WITH_CONVENTION_MAPPING = 6
    private val BEAN_NAME_ATTRIBUTES = listOf(VALUE, NAME)
    private val LANGUAGE_ANNOTATION_PACKAGES = listOf("java.", "javax.annotation.", "kotlin.")

    fun of(owner: PsiModifierListOwner): Set<String>? {
        val beanAnnotation = owner.annotations.firstOrNull { isBeanOrMetaAnnotatedByBean(it) } ?: return null
        val names = if (beanAnnotation.qualifiedName == SpringCoreClasses.BEAN) {
            declaredNames(beanAnnotation)
        } else {
            Reader(lazy { conventionMapsName(owner) }).namesOf(beanAnnotation, isRoot = true, visitedTypes = emptySet())
        }
        return names?.ifEmpty { null }
    }

    private fun conventionMapsName(owner: PsiModifierListOwner): Boolean {
        val module = moduleOf(owner) ?: return false
        val major = SpringBootUtil.getSpringCoreMajorVersion(module) ?: return false
        return major <= LAST_SPRING_MAJOR_WITH_CONVENTION_MAPPING
    }

    private fun moduleOf(owner: PsiModifierListOwner): Module? =
        ModuleUtilCore.findModuleForPsiElement(owner)
            ?: owner.containingFile?.originalFile?.let { ModuleUtilCore.findModuleForPsiElement(it) }

    private fun isBeanOrMetaAnnotatedByBean(annotation: PsiAnnotation): Boolean {
        val annotationName = annotation.qualifiedName ?: return false
        if (annotationName == SpringCoreClasses.BEAN) return true
        if (LANGUAGE_ANNOTATION_PACKAGES.any { annotationName.startsWith(it) }) return false
        return annotationTypeOf(annotation)?.isMetaAnnotatedBy(SpringCoreClasses.BEAN) == true
    }

    private fun annotationTypeOf(annotation: PsiAnnotation): PsiClass? {
        val type = annotation.resolveUAnnotationType()
            ?: annotation.qualifiedName?.let {
                JavaPsiFacade.getInstance(annotation.project).findClass(it, annotation.resolveScope)
            }
        return type?.takeIf { it.isAnnotationType }
    }

    private fun declaredNames(bean: PsiAnnotation): Set<String> =
        BEAN_NAME_ATTRIBUTES.flatMap { bean.getStringMemberValues(it) }.toNames()

    private fun List<String>.toNames(): Set<String> = filter(String::isNotBlank).toCollection(LinkedHashSet())

    private class Reader(conventionMapsName: Lazy<Boolean>) {
        private val conventionMapsName by conventionMapsName

        fun namesOf(annotation: PsiAnnotation, isRoot: Boolean, visitedTypes: Set<String>): Set<String>? {
            ProgressManager.checkCanceled()
            if (annotation.qualifiedName == SpringCoreClasses.BEAN) return declaredNames(annotation)
            val type = annotationTypeOf(annotation) ?: return null
            val typeName = type.qualifiedName ?: return null
            if (typeName in visitedTypes) return null
            val visited = visitedTypes + typeName
            val metaNames = type.annotations
                .filter { isBeanOrMetaAnnotatedByBean(it) }
                .firstNotNullOfOrNull { namesOf(it, isRoot = false, visitedTypes = visited) }
                ?: return null
            val nameAttributes = beanNameAttributes(type, isRoot, visited)
            if (nameAttributes.isEmpty()) return metaNames
            return nameAttributes.flatMap { annotation.getStringMemberValues(it) }.toNames()
        }

        private fun beanNameAttributes(type: PsiClass, isRoot: Boolean, visitedTypes: Set<String>): List<String> {
            val explicit = type.methods.filter { aliasesBeanName(type, it, visitedTypes, emptySet()) }
            if (explicit.isNotEmpty() || !isRoot) return explicit.map { it.name }
            val conventionName = type.findMethodsByName(NAME, false).firstOrNull { overridesByConvention(type, it) }
                ?: return emptyList()
            if (!conventionMapsName) return emptyList()
            return sameTypeMirrorsOf(type, conventionName).map { it.name }
        }

        private fun overridesByConvention(type: PsiClass, attribute: PsiMethod): Boolean {
            val alias = attribute.getAnnotation(SpringCoreClasses.ALIAS_FOR) ?: return true
            return (aliasTargetOf(alias) ?: type).qualifiedName == type.qualifiedName
        }

        private fun sameTypeMirrorsOf(type: PsiClass, attribute: PsiMethod): Set<PsiMethod> {
            val mirrors = linkedSetOf(attribute)
            val pending = ArrayDeque(listOf(attribute))
            while (pending.isNotEmpty()) {
                ProgressManager.checkCanceled()
                val current = pending.removeFirst()
                type.methods
                    .filter { it !in mirrors }
                    .filter { sameTypeAliasTarget(type, current) == it.name || sameTypeAliasTarget(type, it) == current.name }
                    .forEach {
                        mirrors += it
                        pending += it
                    }
            }
            return mirrors
        }

        private fun sameTypeAliasTarget(type: PsiClass, attribute: PsiMethod): String? {
            val alias = attribute.getAnnotation(SpringCoreClasses.ALIAS_FOR) ?: return null
            if ((aliasTargetOf(alias) ?: type).qualifiedName != type.qualifiedName) return null
            return AliasUtils.getAliasedMethodName(alias)
        }

        private fun aliasesBeanName(
            type: PsiClass,
            attribute: PsiMethod,
            visitedTypes: Set<String>,
            visitedAttributes: Set<PsiMethod>,
        ): Boolean {
            ProgressManager.checkCanceled()
            if (attribute in visitedAttributes) return false
            val alias = attribute.getAnnotation(SpringCoreClasses.ALIAS_FOR) ?: return false
            val targetAttribute = AliasUtils.getAliasedMethodName(alias) ?: attribute.name
            val target = aliasTargetOf(alias) ?: type
            return when (val targetName = target.qualifiedName) {
                null -> false
                SpringCoreClasses.BEAN -> targetAttribute in BEAN_NAME_ATTRIBUTES
                type.qualifiedName -> type.findMethodsByName(targetAttribute, false).any {
                    aliasesBeanName(type, it, visitedTypes, visitedAttributes + attribute)
                }

                in visitedTypes -> false
                else -> target.findMethodsByName(targetAttribute, false).any {
                    aliasesBeanName(target, it, visitedTypes + targetName, emptySet())
                }
            }
        }

        private fun aliasTargetOf(alias: PsiAnnotation): PsiClass? {
            val uAlias = alias.toUElement() as? UAnnotation
            val target = uAlias?.let { AliasUtils.getAliasedClass(it) } ?: AliasUtils.getAliasedClass(alias)
            return target?.takeIf { it.isAnnotationType }
        }
    }
}
