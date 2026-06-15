/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.data.langinjection

import com.explyt.jpa.langinjection.JpqlInjectorBase
import com.explyt.plugin.PluginSqlLanguage
import com.explyt.spring.data.SpringDataClasses
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getParentOfType

class JpqlSpringDataQueryLanguageInjector : JpqlInjectorBase() {
    override fun isValidPlace(uElement: UElement): Boolean {
        if (PluginSqlLanguage.SPRING_QL.isEnabled()) return false

        val parentAnnotation = uElement.getParentOfType<UAnnotation>()
            ?.takeIf { it.qualifiedName == SpringDataClasses.QUERY }
            ?: return false

        return parentAnnotation
            .findAttributeValue("nativeQuery")
            ?.evaluate() != true
    }
}