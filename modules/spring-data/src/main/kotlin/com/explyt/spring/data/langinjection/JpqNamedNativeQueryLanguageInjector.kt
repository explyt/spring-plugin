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

package com.explyt.spring.data.langinjection

import com.explyt.jpa.JpaClasses
import com.explyt.jpa.langinjection.JpqlInjectorBase
import com.explyt.plugin.PluginSqlLanguage
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.isUastChildOf

class JpqNamedNativeQueryLanguageInjector : JpqlInjectorBase() {
    override fun isValidPlace(uElement: UElement): Boolean {
        if (PluginSqlLanguage.SQL.isEnabled()) return false
        if (PluginSqlLanguage.JPAQL.isEnabled()) return false

        val uAnnotation = uElement.getParentOfType<UAnnotation>() ?: return false

        if (!JpaClasses.namedNativeQuery.check(uAnnotation.qualifiedName))
            return false

        val queryAttribute = uAnnotation.findAttributeValue("query")

        return uElement.isUastChildOf(queryAttribute)
    }

    override fun getSqlLanguage(sourcePsi: PsiElement?): Language {
        return SqlNativeSpringQueryLanguageInjector.getSqlLanguage()
    }
}