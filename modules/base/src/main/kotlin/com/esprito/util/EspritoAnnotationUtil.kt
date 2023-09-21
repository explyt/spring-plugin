package com.esprito.util

import com.esprito.util.EspritoPsiUtil.getMetaAnnotation
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.intellij.psi.*
import com.intellij.psi.util.childrenOfType
import org.apache.commons.lang.StringUtils

object EspritoAnnotationUtil {

    fun getArrayAttributeAsPsiLiteral(annotation: PsiAnnotation, attributesName: Collection<String>): Collection<PsiLiteral> {
        return attributesName.flatMap { annotation.getArrayAttributeAsPsiLiteral(it) }
    }

    fun getAnnotationMemberValues(
        member: PsiMember?,
        targetAnnotation: String,
        attributeName: String = "value"
    ): Collection<PsiAnnotationMemberValue>? {
        if (member == null || !member.isMetaAnnotatedBy(targetAnnotation)) {
            return null
        }

        return member.getMetaAnnotation(targetAnnotation)
            .getMemberValues(attributeName)
    }

    fun PsiAnnotation?.getMemberValues(attributeName: String?): Collection<PsiAnnotationMemberValue> {
        return when (val attributeValue = this?.findAttributeValue(attributeName)) {
            is PsiArrayInitializerMemberValue -> attributeValue.initializers.toList()
            is PsiAnnotationMemberValue -> listOf(attributeValue)
            else -> emptyList()
        }
    }

    fun PsiAnnotation?.getArrayAttributeAsPsiLiteral(attributeName: String?): Collection<PsiLiteral> {
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
}