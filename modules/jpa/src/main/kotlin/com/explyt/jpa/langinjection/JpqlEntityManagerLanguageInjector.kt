/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.jpa.langinjection

import com.explyt.jpa.JpaClasses
import com.explyt.plugin.PluginSqlLanguage
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.*

class JpqlEntityManagerLanguageInjector : JpqlInjectorBase() {
    override fun isValidPlace(uElement: UElement): Boolean {
        if (PluginSqlLanguage.JPAQL.isEnabled()) return false

        val uCallExpression = uElement.getParentOfType<UCallExpression>() ?: return false

        val firstParameter = uCallExpression.valueArguments.getOrNull(0) ?: return false

        if (!uElement.isUastChildOf(firstParameter))
            return false

        val method = uCallExpression.tryResolve() as? PsiMethod
            ?: return false

        if (!JpaClasses.entityManager.check(method.containingClass?.qualifiedName))
            return false

        return method.name == "createQuery"
    }
}