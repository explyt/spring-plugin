package com.esprito.util

import com.esprito.util.EspritoPsiUtil.getMetaAnnotation
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.psi.*
import com.intellij.psi.util.childrenOfType
import org.apache.commons.lang3.StringUtils

object EspritoAnnotationUtil {

    fun getArrayAttributeAsPsiLiteral(annotation: PsiAnnotation, attributesName: Collection<String>): Collection<PsiLiteral> {
        return attributesName.flatMap { annotation.getArrayAttributeAsPsiLiteral(it) }
    }

    fun PsiMember.getMetaAnnotationMemberValues(
        targetAnnotation: String,
        attributeName: String = "value"
    ): Collection<PsiAnnotationMemberValue>? {
        if (!isMetaAnnotatedBy(targetAnnotation)) {
            return null
        }

        return getMetaAnnotation(targetAnnotation)
            .getMemberValues(attributeName)
    }

    fun PsiAnnotation?.getMemberValues(attributeName: String): Collection<PsiAnnotationMemberValue> {
        return when (val attributeValue = this?.findAttributeValue(attributeName)) {
            is PsiArrayInitializerMemberValue -> attributeValue.initializers.toList()
            is PsiAnnotationMemberValue -> listOf(attributeValue)
            else -> emptyList()
        }
    }

    fun PsiAnnotation?.getStringMemberValues(attributeName: String = "value"): Collection<String> {
        return getMemberValues(attributeName).mapNotNull { it.computeConstantExpression() as? String }
    }

    fun PsiAnnotationMemberValue.computeConstantExpression(): Any? {
        return JavaPsiFacade.getInstance(project)
            .constantEvaluationHelper.computeConstantExpression(this)
    }

    fun PsiAnnotationMemberValue.getBooleanValue(): Boolean? {
        return computeConstantExpression() as? Boolean
    }

    fun PsiAnnotationMemberValue.getStringValue(): String? {
        return computeConstantExpression() as? String
    }

    private fun PsiAnnotation?.getArrayAttributeAsPsiLiteral(attributeName: String?): Collection<PsiLiteral> {
        return when (val attributeValue = this?.findAttributeValue(attributeName)) {
            is PsiArrayInitializerMemberValue -> getArrayAttributeAsPsiLiteral(attributeValue)
            is PsiLiteral -> listOf(attributeValue)
            is PsiReferenceExpression -> getReferenceValue(attributeValue)
            else -> emptyList()
        }
    }

    fun getArrayAttributeAsPsiLiteral(attributeValue: PsiAnnotationMemberValue): List<PsiLiteral> {
        return (attributeValue as? PsiArrayInitializerMemberValue)?.initializers?.filterIsInstance<PsiLiteral>()?.toList() ?: emptyList()
    }

    fun getArrayValueAnnotations(
        annotation: PsiAnnotation,
        attrName: String
    ): Collection<PsiAnnotation> {
        return getArrayValueAnnotations(annotation, attrName, null)
    }

    fun getArrayValueAnnotations(
        annotation: PsiAnnotation,
        attrName: String,
        annotationFqn: String?
    ): Collection<PsiAnnotation> {
        val result: MutableList<PsiAnnotation> = java.util.ArrayList()
        val attributeValue = annotation.findAttributeValue(attrName)
        if (attributeValue is PsiAnnotation
            && (annotationFqn == null || annotationFqn == attributeValue.qualifiedName)
        ) {
            result.add(attributeValue)
        } else if (attributeValue is PsiArrayInitializerMemberValue) {
            for (initializer in attributeValue.initializers) {
                if (initializer is PsiAnnotation
                    && (annotationFqn == null || annotationFqn == initializer.qualifiedName)
                ) {
                    result.add(initializer)
                }
            }
        }
        return result
    }

    private fun processLiteralValue(value: PsiAnnotationMemberValue): List<String> {
        val result: MutableList<String> = ArrayList()
        if (value is PsiLiteral) {
            val literalValue = value.value
            if (literalValue is List<*>) {
                @Suppress("UNCHECKED_CAST")
                result.addAll(literalValue as Collection<String>)
            } else {
                val itemValue = literalValue as String?
                if (StringUtils.isNotBlank(itemValue)) {
                    result.add(itemValue!!)
                }
            }
        } else {
            val text = value.text
            if (StringUtils.isNotBlank(text)) {
                result.add(text)
            }
        }
        return result
    }

    fun getArrayAttributeValue(annotation: PsiAnnotation, attributesName: Collection<String>): Collection<String> {
        return attributesName.flatMap { getArrayAttributeValue(annotation, it) }
    }

    fun getArrayAttributeValue(annotation: PsiAnnotation, attributeName: String?): Collection<String> {
        return when (val attributeValue = annotation.findAttributeValue(attributeName)) {
            is PsiArrayInitializerMemberValue -> getArrayAttributeValue(attributeValue)
            is PsiLiteral -> processLiteralValue(attributeValue)
            else -> emptyList()
        }
    }

    fun getArrayAttributeValue(attributeValue: PsiAnnotationMemberValue): List<String> {
        return (attributeValue as? PsiArrayInitializerMemberValue)?.initializers
            ?.flatMap { processLiteralValue(it) } ?: emptyList()
    }

    fun getReferenceValue(attributeValue: PsiReferenceExpression): Collection<PsiLiteral> {
       return attributeValue.reference?.resolve()?.childrenOfType() ?: emptyList()
    }

    fun PsiAnnotation.getStringValue(): String? {
        return AnnotationUtil.getStringAttributeValue(this, "value")?.takeIf { it.isNotEmpty() }
    }

    fun PsiAnnotation.getLongValue(): Long? {
        return AnnotationUtil.getLongAttributeValue(this, "value")
    }

    fun PsiModifierListOwner.getMetaAnnotationValue(annotationName: String): String? {
        if (isMetaAnnotatedBy(annotationName)) {
            return getMetaAnnotation(annotationName)?.getStringValue()
        }
        return null
    }

    fun PsiModifierListOwner.getMetaAnnotationValue(annotationNames: Collection<String>): String? {
        if (isMetaAnnotatedBy(annotationNames)) {
            return getMetaAnnotation(annotationNames)?.getStringValue()
        }
        return null
    }

    /** Edited version of [com.intellij.codeInsight.AnnotationUtil.equal] */
    fun equal(a: PsiAnnotation?, b: PsiAnnotation?): Boolean {
        if (a == null) {
            return b == null
        }
        if (b == null) {
            return false
        }
        val name = a.qualifiedName
        if (name == null || name != b.qualifiedName) {
            return false
        }
        val valueMap1: MutableMap<String, PsiAnnotationMemberValue?> = HashMap(2)
        val valueMap2: MutableMap<String, PsiAnnotationMemberValue?> = HashMap(2)
        if (!fillValueMap(a.parameterList, valueMap1) || !fillValueMap(b.parameterList, valueMap2) || valueMap1.size != valueMap2.size) {
            return false
        }
        for ((key, value) in valueMap1) {
            if (!equal(value, valueMap2[key])) {
                return false
            }
        }
        return true
    }

    private fun fillValueMap(parameterList: PsiAnnotationParameterList, valueMap: MutableMap<String, PsiAnnotationMemberValue?>): Boolean {
        val attributes1 = parameterList.attributes
        for (attribute in attributes1) {
            val reference = attribute.reference ?: return false
            val target = reference.resolve() as? PsiAnnotationMethod ?: return false
            val defaultValue = target.defaultValue
            val value = attribute.value
            if (equal(value, defaultValue)) {
                continue
            }
            val name1 = attribute.name
            valueMap[name1 ?: PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME] = value
        }
        return true
    }

    private fun equal(value1: PsiAnnotationMemberValue?, value2: PsiAnnotationMemberValue?): Boolean {
        if (value1 == value2) {
            return true
        }
        if (value1 is PsiReference && value2 is PsiReference
            && value1.resolve() == value2.resolve()) {
            return true
        }
        if (value1 is PsiArrayInitializerMemberValue && value2 is PsiArrayInitializerMemberValue) {
            val initializers1 = value1.initializers
            val initializers2 = value2.initializers
            return if (initializers1.size != initializers2.size) {
                false
            } else {
                initializers1.zip(initializers2).all { (a, b) -> equal(a, b) }
            }
        }
        if (value1 != null && value2 != null) {
            val constantEvaluationHelper = JavaPsiFacade.getInstance(value1.project).constantEvaluationHelper
            val const1 = constantEvaluationHelper.computeConstantExpression(value1)
            val const2 = constantEvaluationHelper.computeConstantExpression(value2)
            return const1 != null && const1 == const2
        }
        return false
    }

}