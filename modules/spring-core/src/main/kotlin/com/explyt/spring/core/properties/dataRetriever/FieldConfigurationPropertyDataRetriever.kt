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
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UField
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.getContainingUClass

class FieldConfigurationPropertyDataRetriever private constructor(
    private val uField: UField
) : ConfigurationPropertyDataRetriever() {


    override fun getContainingClass(): PsiClass? {
        val uClass = uField.getContainingUClass() ?: return null
        return if (!uClass.isInterface && uClass.isNonAbstract) uClass.javaPsi else null
    }

    override fun getMemberName(): String? {
        return (uField.uastAnchor as? UIdentifier)?.name
    }

    override fun getNameElementPsi(): PsiElement? {
        return uField.uastAnchor?.sourcePsi
    }

    override fun isMap(): Boolean {
        val psiType = uField.typeReference?.type ?: return false
        return InheritanceUtil.isInheritor(psiType, Map::class.java.name)
    }

    override fun isCollection(): Boolean {
        val psiType = uField.typeReference?.type ?: return false
        return InheritanceUtil.isInheritor(psiType, Iterable::class.java.name) || psiType is PsiArrayType
    }

    companion object {
        fun create(uField: UField): ConfigurationPropertyDataRetriever {
            return FieldConfigurationPropertyDataRetriever(uField)
        }
    }

}
