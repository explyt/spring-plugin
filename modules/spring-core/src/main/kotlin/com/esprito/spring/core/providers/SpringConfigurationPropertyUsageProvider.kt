package com.esprito.spring.core.providers

import com.esprito.spring.core.properties.dataRetriever.ConfigurationPropertyDataRetriever
import com.esprito.spring.core.properties.dataRetriever.ConfigurationPropertyDataRetrieverFactory
import com.esprito.spring.core.util.SpringCoreUtil
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