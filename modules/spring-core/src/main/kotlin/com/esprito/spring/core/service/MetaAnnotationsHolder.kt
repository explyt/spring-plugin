package com.esprito.spring.core.service

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.util.EspritoPsiUtil.isAnnotatedBy
import com.esprito.util.EspritoPsiUtil.resolvedPsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.*
import com.intellij.psi.util.childrenOfType

class MetaAnnotationsHolder private constructor(
    private val annotationByFqn: Map<String, AnnotationInfo>
) {

    fun contains(psi: PsiAnnotation) =
        annotationByFqn.contains(psi.qualifiedName)

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
                val annotation = annotationTypes.remove(annotationToProceed) ?: continue

                annotationsToProceed += annotationTypes.values.asSequence()
                    .filter { it.isAnnotatedBy(annotationToProceed) }
                    .mapNotNull { it.qualifiedName }

                val annotationInfo = AnnotationInfo(
                    annotationToProceed,
                    annotation.methods
                        .map { AttributeInfo.of(it, annotationToProceed) }
                )

                annotationByFqn[annotationToProceed] = annotationInfo

            }
            return MetaAnnotationsHolder(annotationByFqn)
        }
    }


    class AnnotationInfo(val qualifiedName: String, attributes: List<AttributeInfo>) {
        val attributeByName = attributes.associateBy { it.name }
    }

    class AttributeInfo private constructor(val name: String, val aliasInfo: AliasInfo? = null) {

        companion object {
            fun of(psiMethod: PsiMethod, annotationFqn: String): AttributeInfo {
                if (!psiMethod.isAnnotatedBy(SpringCoreClasses.ALIAS_FOR)) {
                    return AttributeInfo(psiMethod.name)
                }

                val alias = psiMethod.getAnnotation(SpringCoreClasses.ALIAS_FOR)
                val aliasedMethod = listOf(
                    alias?.findAttributeValue("value"),
                    alias?.findAttributeValue("attribute")
                ).asSequence()
                    .filterNotNull()
                    .map { AnnotationUtil.getStringAttributeValue(it) }
                    .filter { !it.isNullOrBlank() }
                    .firstOrNull() ?: psiMethod.name

                val aliasedClassFqn = alias?.findAttributeValue("annotation")
                    ?.childrenOfType<PsiTypeElement>()
                    ?.firstOrNull()
                    ?.type
                    ?.resolvedPsiClass
                    ?.qualifiedName
                    ?: annotationFqn

                val aliasInfo = AliasInfo(
                    aliasedMethod,
                    if (aliasedClassFqn == SpringCoreClasses.ANNOTATION) annotationFqn else aliasedClassFqn
                )
                return AttributeInfo(psiMethod.name, aliasInfo)
            }
        }

    }

    class AliasInfo(val methodName: String, val annotationFqn: String)
}
