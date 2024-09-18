package com.esprito.spring.web.providers

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

        return if (specificationType is OpenApiSpecificationType.NONE) null else specificationType.icon
    }
}