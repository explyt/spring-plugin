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

package com.explyt.spring.core.service

import com.explyt.spring.core.JavaCoreClasses
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.util.ExplytAnnotationUtil.getMemberValues
import com.explyt.util.ExplytPsiUtil.isAnnotatedBy
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.*
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.parentOfType
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.toUElement

class MetaAnnotationsHolder private constructor(
    private val rootFqn: String,
    private val annotationByFqn: Map<String, AnnotationInfo>
) {

    fun contains(psi: PsiAnnotation) =
        annotationByFqn.contains(psi.qualifiedName)

    fun contains(uAnnotation: UAnnotation): Boolean {
        val javaPsi = uAnnotation.javaPsi ?: return false
        return annotationByFqn.contains(javaPsi.qualifiedName)
    }

    fun isAttributeRelatedWith(
        annotationFqn: String,
        attributeName: String,
        targetAnnotationFqn: String,
        targetAttributes: Set<String>
    ): Boolean {
        val annotationInfo = annotationByFqn[annotationFqn] ?: return false
        val attributeInfo = annotationInfo.attributeByName[attributeName] ?: return false

        if (targetAnnotationFqn == annotationFqn && targetAttributes.contains(attributeInfo.name)) {
            return true
        }

        val aliasInfo = attributeInfo.aliasInfo ?: return false

        if (aliasInfo.annotationFqn == annotationFqn) {
            return targetAnnotationFqn == annotationFqn && targetAttributes.contains(aliasInfo.methodName)
        }

        return isAttributeRelatedWith(
            aliasInfo.annotationFqn,
            aliasInfo.methodName,
            targetAnnotationFqn,
            targetAttributes
        )
    }

    fun getAnnotationMemberValues(psiMember: PsiMember, targetMethods: Set<String>): Set<PsiAnnotationMemberValue> {
        return psiMember.annotations
            .flatMapTo(mutableSetOf()) {
                getAnnotationMemberValues(it, targetMethods)
            }
    }

    fun getAnnotationMemberValues(psiMember: PsiMember, targetMethod: String): Set<PsiAnnotationMemberValue> {
        val targetMethods = setOf(targetMethod)
        return psiMember.annotations
            .flatMapTo(mutableSetOf()) {
                getAnnotationMemberValues(it, targetMethods)
            }
    }

    fun getAnnotationMemberValues(
        psiAnnotation: PsiAnnotation,
        targetMethod: String
    ): Set<PsiAnnotationMemberValue> {
        return getAnnotationMemberValues(psiAnnotation, setOf(targetMethod))
    }

    fun getAnnotationMemberValues(
        psiAnnotation: PsiAnnotation,
        targetMethods: Set<String>
    ): Set<PsiAnnotationMemberValue> {
        val annotationMemberValues = mutableListOf<PsiAnnotationMemberValue>()
        val annotationsToProceed = mutableListOf(psiAnnotation)

        while (annotationMemberValues.isEmpty() && annotationsToProceed.isNotEmpty()) {
            val currentPsiAnnotation = annotationsToProceed.removeFirst()
            val annotationFqn = currentPsiAnnotation.qualifiedName ?: return setOf()
            val annotationInfo = annotationByFqn[annotationFqn] ?: continue

            annotationsToProceed.addAll(annotationInfo.annotationType.annotations)

            annotationMemberValues += currentPsiAnnotation.attributes.asSequence()
                .filter {
                    isAttributeRelatedWith(
                        annotationFqn,
                        it.attributeName,
                        rootFqn,
                        targetMethods
                    )
                }
                .flatMap { currentPsiAnnotation.getMemberValues(it.attributeName) }

        }

        return annotationMemberValues.toSet()
    }

    fun getRootClassQualified() = rootFqn

    companion object {
        fun of(module: Module, parentFqn: String): MetaAnnotationsHolder {
            val annotationsToProceed = mutableListOf(parentFqn)
            val annotationByFqn = mutableMapOf<String, AnnotationInfo>()

            val annotationTypes = MetaAnnotationUtil
                .getAnnotationTypesWithChildren(module, parentFqn, false)
                .associateBy { it.qualifiedName }
                .toMutableMap()

            while (annotationsToProceed.isNotEmpty()) {
                val annotationToProceed = annotationsToProceed.removeFirst()
                val annotationType = annotationTypes.remove(annotationToProceed) ?: continue

                annotationsToProceed += annotationTypes.values.asSequence()
                    .filter { it.isValid }
                    .filter { it.isAnnotatedBy(annotationToProceed) }
                    .mapNotNull { it.qualifiedName }

                val annotationInfo = AnnotationInfo(
                    annotationToProceed,
                    annotationType,
                    annotationType.methods
                        .map { AttributeInfo.of(it, annotationToProceed) }
                )

                annotationByFqn[annotationToProceed] = annotationInfo

            }
            return MetaAnnotationsHolder(parentFqn, annotationByFqn)
        }
    }


    class AnnotationInfo(val qualifiedName: String, val annotationType: PsiClass, attributes: List<AttributeInfo>) {
        val attributeByName = attributes.associateBy { it.name }
    }

    class AttributeInfo private constructor(val name: String, val aliasInfo: AliasInfo? = null) {

        companion object {
            fun of(psiMethod: PsiMethod, annotationFqn: String): AttributeInfo {
                val alias = psiMethod.getAnnotation(SpringCoreClasses.ALIAS_FOR)
                    ?: return AttributeInfo(psiMethod.name)

                val aliasedMethod = AliasUtils.getAliasedMethodName(alias)
                    ?: psiMethod.name
                val aliasedClassFqn = AliasUtils.getAliasedClass(alias)
                    ?.qualifiedName
                    ?: annotationFqn

                val aliasInfo = AliasInfo(aliasedMethod, aliasedClassFqn)
                return AttributeInfo(psiMethod.name, aliasInfo)
            }
        }

    }

    class AliasInfo(val methodName: String, val annotationFqn: String)
}

object AliasUtils {

    fun getAliasedClass(alias: PsiAnnotation): PsiClass? {
        return alias.findAttributeValue("annotation")
            ?.let {
                getAliasedClassJava(it)
                    ?: getAliasedClassKt(it)
            } ?: alias.parentOfType<PsiClass>()
    }

    private fun getAliasedClassJava(memberValue: PsiAnnotationMemberValue): PsiClass? {
        return memberValue
            .childrenOfType<PsiTypeElement>()
            .firstOrNull { it.type.resolvedPsiClass?.qualifiedName != JavaCoreClasses.ANNOTATION }
            ?.type
            ?.resolvedPsiClass
    }

    private fun getAliasedClassKt(memberValue: PsiAnnotationMemberValue): PsiClass? {
        return memberValue
            .toUElement()
            ?.sourcePsi
            ?.children?.asSequence()
            ?.mapNotNull { it.reference }
            ?.mapNotNull { it.resolve() }
            ?.mapNotNull { it.toUElement() }
            ?.mapNotNull { it.javaPsi as? PsiClass }
            ?.firstOrNull()
    }

    fun getAliasedMethodName(alias: PsiAnnotation): String? {
        return listOf(
            alias.findAttributeValue("value"),
            alias.findAttributeValue("attribute")
        ).asSequence()
            .filterNotNull()
            .map { AnnotationUtil.getStringAttributeValue(it) }
            .filter { !it.isNullOrBlank() }
            .firstOrNull()
    }

}