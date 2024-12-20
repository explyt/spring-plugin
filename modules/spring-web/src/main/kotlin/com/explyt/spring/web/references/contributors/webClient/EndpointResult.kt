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

package com.explyt.spring.web.references.contributors.webClient

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.web.SpringWebClasses
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiType
import com.intellij.psi.PsiWildcardType
import com.intellij.psi.impl.source.PsiClassReferenceType

data class EndpointResult(
    val wrapperName: String?,
    val typeReferencePresentable: String,
    val typeReferenceCanonical: String,
    val returnType: String,
    val isRaw: Boolean
) {
    companion object {
        fun of(returnType: PsiClassReferenceType?, language: Language, asTypeRef: Boolean): EndpointResult? {
            if (returnType == null) return null

            val wrapperName = getWrapperName(returnType)

            val genericParameter = if (wrapperName == null) {
                returnType
            } else {
                returnType.parameters.firstOrNull() ?: return null
            }
            val classRefName = getClassRefName(genericParameter, language, asTypeRef) ?: return null
            val typeReferencePreview = classRefName.presentable
            val typeReferenceCanonical = classRefName.canonical

            return EndpointResult(
                wrapperName = wrapperName,
                typeReferencePresentable = typeReferencePreview,
                typeReferenceCanonical = typeReferenceCanonical,
                returnType = returnType.presentableText,
                classRefName.raw
            )
        }

        private fun getWrapperName(psiType: PsiClassReferenceType): String? {
            return when (psiType.resolve()?.qualifiedName) {
                SpringWebClasses.MONO -> "Mono"
                SpringWebClasses.FLUX -> "Flux"
                SpringWebClasses.FLOW -> "Flux"
                else -> null
            }
        }

        private fun getClassRefName(psiType: PsiType, language: Language, asTypeRef: Boolean): ClassRefName? {
            if (psiType is PsiClassReferenceType) {
                if (psiType.hasParameters()) {
                    val wrapper = psiType.resolve() ?: return null
                    val wrapperQn = wrapper.qualifiedName ?: return null
                    val parameter = psiType.parameters.first()

                    if (wrapperQn == SpringWebClasses.RESPONSE_ENTITY) {
                        return getClassRefName(parameter, language, asTypeRef)
                    }

                    if (asTypeRef) {
                        val (parameterNameCanonical, parameterNamePresentable) = getParameterClassRefName(parameter)

                        return if (language == JavaLanguage.INSTANCE) {
                            ClassRefName(
                                "new ${SpringCoreClasses.PARAMETERIZED_TYPE_REFERENCE}<$wrapperQn<$parameterNameCanonical>>(){}",
                                "new ParameterizedTypeReference<${wrapper.name}<$parameterNamePresentable>>(){}",
                                false
                            )
                        } else {
                            ClassRefName(
                                "object : ${SpringCoreClasses.PARAMETERIZED_TYPE_REFERENCE}<$wrapperQn<$parameterNameCanonical>>(){}",
                                "object : ParameterizedTypeReference<${wrapper.name}<$parameterNamePresentable>>(){}",
                                false
                            )
                        }
                    }
                }
            }

            return getParameterClassRefName(psiType)
        }


        private fun getParameterClassRefName(psiType: PsiType): ClassRefName {
            val typeToGetName = if (psiType is PsiWildcardType) psiType.extendsBound else psiType

            return ClassRefName.of(typeToGetName)
        }

        data class ClassRefName(val canonical: String, val presentable: String, val raw: Boolean = true) {
            companion object {
                fun of(psiType: PsiType): ClassRefName {
                    if (!psiType.canonicalText.contains(".")) {
                        return ClassRefName(CommonClassNames.JAVA_LANG_OBJECT, "Object")
                    }

                    return ClassRefName(psiType.canonicalText, psiType.presentableText)
                }
            }
        }
    }
}
