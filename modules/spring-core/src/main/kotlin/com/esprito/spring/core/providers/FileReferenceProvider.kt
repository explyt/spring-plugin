package com.esprito.spring.core.providers

import com.esprito.spring.core.SpringProperties
import com.esprito.spring.core.references.PrefixReference
import com.esprito.spring.core.references.PrefixReferenceType
import com.esprito.spring.core.util.PropertyUtil
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.UExpression

class FileReferenceProvider(private val possibleFileTypes: Array<FileType> = emptyArray()) :
    UastInjectionHostReferenceProvider() {
    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val text = ElementManipulators.getValueText(host)
        val psiElement = uExpression.sourcePsi ?: return PsiReference.EMPTY_ARRAY

        val references = mutableListOf<PsiReference>()
        when {
            text.startsWith(SpringProperties.PREFIX_FILE) -> {
                references += PropertyUtil.getReferenceByFilePrefix(text, psiElement, possibleFileTypes, null)
            }

            text.startsWith(SpringProperties.PREFIX_CLASSPATH) -> {
                references += PropertyUtil
                    .getReferenceByClasspathPrefix(text,
                        SpringProperties.PREFIX_CLASSPATH, psiElement, possibleFileTypes, null)
            }

            else -> {
                references += PropertyUtil.getReferenceWithoutPrefix(text, psiElement, possibleFileTypes, null)
                references += PrefixReference(psiElement, PrefixReferenceType.ANNOTATION_PROPERTY)
            }
        }
        return references.toTypedArray()
    }
}