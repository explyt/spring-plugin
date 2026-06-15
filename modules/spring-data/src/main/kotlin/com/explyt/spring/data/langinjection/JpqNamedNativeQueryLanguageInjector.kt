/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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