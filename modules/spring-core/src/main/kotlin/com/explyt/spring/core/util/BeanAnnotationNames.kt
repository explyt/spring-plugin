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
        return Reader(conventionMapsName(owner))
            .namesOf(beanAnnotation, isRoot = true, visitedTypes = emptySet())
            ?.ifEmpty { null }
    }

    private fun conventionMapsName(owner: PsiModifierListOwner): Boolean {
        val module = ModuleUtilCore.findModuleForPsiElement(owner) ?: return false
        val major = SpringBootUtil.getSpringCoreMajorVersion(module) ?: return false
        return major <= LAST_SPRING_MAJOR_WITH_CONVENTION_MAPPING
    }

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

    private class Reader(private val conventionMapsName: Boolean) {

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
            return beanNameAttributes(type, isRoot, visited)
                .flatMap { annotation.getStringMemberValues(it) }
                .toNames()
                .ifEmpty { metaNames }
        }

        private fun declaredNames(bean: PsiAnnotation): Set<String> =
            BEAN_NAME_ATTRIBUTES.flatMap { bean.getStringMemberValues(it) }.toNames()

        private fun beanNameAttributes(type: PsiClass, isRoot: Boolean, visitedTypes: Set<String>): List<String> =
            type.methods.filter { mapsToBeanName(type, it, isRoot, visitedTypes, emptySet()) }.map { it.name }

        private fun mapsToBeanName(
            type: PsiClass,
            attribute: PsiMethod,
            isRoot: Boolean,
            visitedTypes: Set<String>,
            visitedAttributes: Set<PsiMethod>,
        ): Boolean {
            ProgressManager.checkCanceled()
            if (attribute in visitedAttributes) return false
            val alias = attribute.getAnnotation(SpringCoreClasses.ALIAS_FOR)
                ?: return isRoot && conventionMapsName && attribute.name == NAME
            val targetAttribute = AliasUtils.getAliasedMethodName(alias) ?: attribute.name
            val target = aliasTargetOf(alias) ?: type
            return when (val targetName = target.qualifiedName) {
                null -> false
                SpringCoreClasses.BEAN -> targetAttribute in BEAN_NAME_ATTRIBUTES
                type.qualifiedName -> type.findMethodsByName(targetAttribute, false).any {
                    mapsToBeanName(type, it, isRoot, visitedTypes, visitedAttributes + attribute)
                }

                in visitedTypes -> false
                else -> target.findMethodsByName(targetAttribute, false).any {
                    mapsToBeanName(target, it, isRoot = false, visitedTypes = visitedTypes + targetName, emptySet())
                }
            }
        }

        private fun aliasTargetOf(alias: PsiAnnotation): PsiClass? {
            val uAlias = alias.toUElement() as? UAnnotation
            val target = uAlias?.let { AliasUtils.getAliasedClass(it) } ?: AliasUtils.getAliasedClass(alias)
            return target?.takeIf { it.isAnnotationType }
        }

        private fun List<String>.toNames(): Set<String> = filter(String::isNotBlank).toCollection(LinkedHashSet())
    }
}
