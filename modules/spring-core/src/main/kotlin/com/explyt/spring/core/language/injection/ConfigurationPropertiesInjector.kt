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

package com.explyt.spring.core.language.injection

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.lang.injection.general.Injection
import com.intellij.lang.injection.general.LanguageInjectionContributor
import com.intellij.lang.injection.general.SimpleInjection
import com.intellij.lang.properties.PropertiesLanguage
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.*
import org.jetbrains.uast.expressions.UInjectionHost

class ConfigurationPropertiesInjector : LanguageInjectionContributor {

    companion object {
        fun isValidPlace(uElement: UElement): Boolean {
            val uAnnotation = uElement.getParentOfType<UAnnotation>()
            if (uAnnotation != null && arrayOf(SpringCoreClasses.SPRING_BOOT_TEST, SpringCoreClasses.TEST_PROPERTY_SOURCE).contains(
                    uAnnotation.qualifiedName
                )
            ) {
                val queryAttribute = uAnnotation.findAttributeValue("properties")
                return uElement.isUastChildOf(queryAttribute)
            }

            val psiMethod = uElement.getParentOfType<UQualifiedReferenceExpression>()?.tryResolve()
            return psiMethod is PsiMethod
                    && psiMethod.name == "of"
                    && psiMethod.containingClass?.qualifiedName == SpringCoreClasses.TEST_PROPERTY_VALUES
        }
    }

    override fun getInjection(context: PsiElement): Injection? {
        if (!SpringCoreUtil.isSpringBootProject(context.project)) {
            return null
        }

        val uElement = context.toUElementOfExpectedTypes(
            UInjectionHost::class.java,
            UPolyadicExpression::class.java
        ) ?: return null

        if (!isValidPlace(uElement)) return null

        return SimpleInjection(PropertiesLanguage.INSTANCE, "", "", null)
    }
}
