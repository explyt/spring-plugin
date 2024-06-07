package com.esprito.spring.core.inspections

import com.esprito.spring.core.completion.properties.DefinedConfigurationProperty
import com.esprito.spring.core.completion.properties.PropertiesPropertySource
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.lang.properties.PropertiesQuickFixFactory
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.Property
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class SpringPropertiesInspection : SpringBasePropertyInspection() {

    override fun loadFileProperties(file: PsiFile): List<DefinedConfigurationProperty> {
        return (file as? PropertiesFile)?.let { PropertiesPropertySource(it).properties } ?: emptyList()
    }

    override fun getKeyPsiElement(property: DefinedConfigurationProperty): PsiElement? {
        return (property.psiElement as? Property)?.firstChild
    }

    override fun getRemoveKeyQuickFixes(property: DefinedConfigurationProperty): List<LocalQuickFix> {
        return (property.psiElement as? Property)
            ?.let { PropertiesQuickFixFactory.getInstance().createRemovePropertyLocalFix(it) }
            ?.let { listOf(it) } ?: emptyList()
    }
}