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

package com.explyt.spring.core.providers

import com.explyt.spring.core.properties.dataRetriever.ConfigurationPropertyDataRetriever
import com.explyt.spring.core.properties.dataRetriever.ConfigurationPropertyDataRetrieverFactory
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.toUElement

class SpringConfigurationPropertyUsageProvider : ImplicitUsageProvider {
    override fun isImplicitUsage(element: PsiElement): Boolean {
        if (element !is PsiMethod) return false
        if (!SpringCoreUtil.isSpringProject(element.project)) return false
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return false

        val dataRetriever = ConfigurationPropertyDataRetrieverFactory.createFor(element.toUElement()) ?: return false
        val psiClass = dataRetriever.getContainingClass() ?: return false
        val memberName = dataRetriever.getMemberName() ?: return false
        val prefixValue = ConfigurationPropertyDataRetriever.getPrefixValue(psiClass)

        if (prefixValue.isBlank()) return false

        val properties = dataRetriever.getRelatedProperties(prefixValue, memberName, module)
        return properties.isNotEmpty()
    }

    override fun isImplicitRead(element: PsiElement): Boolean {
        return false
    }

    override fun isImplicitWrite(element: PsiElement): Boolean {
        return false
    }

}