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

package com.explyt.spring.core.properties.dataRetriever

import ai.grazie.utils.capitalize
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParameter
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter

class ConstructorParameterConfigurationPropertyDataRetriever(
    private val uParameter: UParameter,
    private val containingClass: PsiClass,
    private val name: String
) : ConfigurationPropertyDataRetriever() {

    override fun getContainingClass(): PsiClass = containingClass

    override fun getMemberName(): String = name

    override fun getNameElementPsi(): PsiElement? {
        return uParameter.uastAnchor?.sourcePsi
    }

    override fun isMap(): Boolean {
        val psiType = uParameter.typeReference?.type ?: return false
        return InheritanceUtil.isInheritor(psiType, Map::class.java.name)
    }

    override fun isCollection(): Boolean {
        val psiType = uParameter.typeReference?.type ?: return false
        return InheritanceUtil.isInheritor(psiType, Iterable::class.java.name) || psiType is PsiArrayType
    }

    companion object {
        fun create(uParameter: UParameter): ConfigurationPropertyDataRetriever? {
            val psiParameter = (uParameter.javaPsi as? PsiParameter) ?: return null
            if (psiParameter.language != KotlinLanguage.INSTANCE) return null
            val uMethod = (uParameter.uastParent as? UMethod) ?: return null
            if (!uMethod.isConstructor) return null
            val containingClass = uMethod.javaPsi.containingClass ?: return null

            return ConstructorParameterConfigurationPropertyDataRetriever(
                uParameter,
                containingClass,
                psiParameter.name.capitalize()
            )
        }
    }

}