package com.esprito.spring.web.providers

import com.esprito.spring.web.SpringWebIcons
import com.esprito.spring.web.model.OpenApiSpecificationDetection
import com.esprito.spring.web.model.OpenApiSpecificationType
import com.intellij.ide.IconProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import javax.swing.Icon

class OpenApiFileIconProvider : IconProvider() {
    override fun getIcon(element: PsiElement, flags: Int): Icon? {
        if (element !is PsiFile) return null

        val virtualFile = element.virtualFile ?: return null
        val specificationType = OpenApiSpecificationDetection.detectSpecificationType(virtualFile, element)
        val extension = virtualFile.extension ?: return null

        return if (specificationType is OpenApiSpecificationType.NONE) null
        else getIconForExtension(extension)
    }

    private fun getIconForExtension(extension: String): Icon? {
        if (extension.isBlank()) return null
        if (extension == "yaml" || extension == "yml") {
            return SpringWebIcons.OpenApiYaml
        } else if (extension == "json") {
            return SpringWebIcons.OpenApiJson
        }
        return null
    }
}