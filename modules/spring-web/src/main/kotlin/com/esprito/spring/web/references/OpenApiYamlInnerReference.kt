package com.esprito.spring.web.references

import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.kotlin.utils.addToStdlib.lastIndexOfOrNull
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLScalar

class OpenApiYamlInnerReference(element: PsiElement) : PsiReferenceBase<PsiElement>(element), HighlightedReference {

    override fun resolve(): PsiElement? {
        val yamlScalar = yamlScalar() ?: return null
        val yamlFile = yamlScalar.containingFile as? YAMLFile ?: return null

        val keyToFind = yamlScalar.textValue //value example: #/some/element/PathInsideFile
            .substring(2)

        return YAMLUtil.getQualifiedKeyInFile(
            yamlFile,
            keyToFind.split('/')
        )?.navigationElement
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val originalValue = yamlScalar()?.textValue ?: return super.handleElementRename(newElementName)
        val lastElementPos = originalValue.lastIndexOfOrNull('/') ?: return super.handleElementRename(newElementName)
        val prefix = originalValue.substring(0, lastElementPos)

        return super.handleElementRename("$prefix/$newElementName")
    }

    private fun yamlScalar(): YAMLScalar? =
        element as? YAMLScalar

}