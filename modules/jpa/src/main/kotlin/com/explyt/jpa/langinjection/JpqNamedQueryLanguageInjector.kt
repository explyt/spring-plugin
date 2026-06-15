/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.jpa.langinjection

import com.explyt.jpa.JpaClasses
import com.explyt.plugin.PluginSqlLanguage
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.isUastChildOf

class JpqNamedQueryLanguageInjector : JpqlInjectorBase() {
    override fun isValidPlace(uElement: UElement): Boolean {
        if (PluginSqlLanguage.JPAQL.isEnabled()) return false
        if (PluginSqlLanguage.SPRING_QL.isEnabled()) return false

        val uAnnotation = uElement.getParentOfType<UAnnotation>() ?: return false

        if (!JpaClasses.namedQuery.check(uAnnotation.qualifiedName))
            return false

        val queryAttribute = uAnnotation.findAttributeValue("query")

        return uElement.isUastChildOf(queryAttribute)
    }
}