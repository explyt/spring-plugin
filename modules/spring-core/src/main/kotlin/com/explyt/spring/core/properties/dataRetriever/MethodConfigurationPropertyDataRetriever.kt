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

import com.explyt.util.ExplytPsiUtil.isNonAbstract
import com.explyt.util.ExplytPsiUtil.isSetter
import com.explyt.util.ExplytPsiUtil.returnPsiClass
import com.explyt.util.ExplytPsiUtil.returnPsiType
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UMethod

class MethodConfigurationPropertyDataRetriever private constructor(
    private val uMethod: UMethod,
) : ConfigurationPropertyDataRetriever() {

    val psiMethod = uMethod.javaPsi

    override fun getContainingClass(): PsiClass? {
        val psiClass = psiMethod.containingClass ?: return null
        return if (!psiClass.isInterface && psiClass.isNonAbstract) psiClass else null
    }

    override fun getMemberName(): String? {
        return if (isSetter(psiMethod)) {
            toPascalFormat(psiMethod.name)
        } else {
            return null
        }
    }

    override fun getNameElementPsi(): PsiElement? {
        return uMethod.uastAnchor?.sourcePsi
    }

    override fun isMap(): Boolean {
        val psiType = uMethod.returnPsiClass?.returnPsiType ?: return false
        return InheritanceUtil.isInheritor(psiType, Iterable::class.java.name)
    }

    override fun isCollection(): Boolean {
        val psiType = uMethod.returnPsiClass?.returnPsiType ?: return false
        return InheritanceUtil.isInheritor(psiType, Iterable::class.java.name) || psiType is PsiArrayType
    }

    companion object {
        fun create(uMethod: UMethod): ConfigurationPropertyDataRetriever {
            return MethodConfigurationPropertyDataRetriever(uMethod)
        }
    }

}