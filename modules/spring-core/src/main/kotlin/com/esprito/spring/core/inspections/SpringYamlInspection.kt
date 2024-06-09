package com.esprito.spring.core.inspections

import com.esprito.spring.core.completion.properties.DefinedConfigurationProperty
import com.esprito.spring.core.completion.properties.YamlPropertySource
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.Nls
import org.jetbrains.yaml.YAMLBundle
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue

class SpringYamlInspection : SpringBasePropertyInspection() {

    override fun loadFileProperties(file: PsiFile): List<DefinedConfigurationProperty> {
        return (file as? YAMLFile)?.let { YamlPropertySource(it).properties } ?: emptyList()
    }

    override fun getKeyPsiElement(property: DefinedConfigurationProperty): PsiElement? {
        return (property.psiElement as? YAMLKeyValue)?.key
    }

    override fun getRemoveKeyQuickFixes(property: DefinedConfigurationProperty): List<LocalQuickFix> {
        return listOf(RemoveDuplicatedKeyQuickFix())
    }
}

/**
 * Copied from org.jetbrains.yaml.inspections.YAMLDuplicatedKeysInspection
 */
private class RemoveDuplicatedKeyQuickFix : LocalQuickFix {
    override fun getFamilyName(): @Nls String {
        return YAMLBundle.message("YAMLDuplicatedKeysInspection.remove.key.quickfix.name")
    }

    override fun availableInBatchMode() = false

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val keyVal = descriptor.psiElement.parent as? YAMLKeyValue ?: return
        keyVal.parentMapping?.deleteKeyValue(keyVal)
    }
}