package com.esprito.spring.core.providers

import com.esprito.spring.core.SpringProperties
import com.esprito.spring.core.references.PrefixReference
import com.esprito.spring.core.references.PrefixReferenceType
import com.esprito.spring.core.util.PropertyUtil
import com.esprito.spring.core.util.PsiAnnotationUtils.getParentAnnotationForPsiLiteralParameter
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.*
import com.intellij.util.ProcessingContext

class FileReferenceInAnnotationProvider(private val possibleAnnotations: Set<String>) : PsiReferenceProvider() {

    constructor(possibleAnnotations: Set<String>, possibleFileTypes: Array<FileType>) : this(possibleAnnotations) {
        this.possibleFileTypes = possibleFileTypes
    }

    private var possibleFileTypes: Array<FileType> = emptyArray()
    override fun getReferencesByElement(
        psiElement: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        if (!acceptPsiElement(psiElement)) return PsiReference.EMPTY_ARRAY
        if (psiElement !is PsiLiteral) return PsiReference.EMPTY_ARRAY

        val text = psiElement.value.toString().substringBefore(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)

        val references = mutableListOf<PsiReference>()
        when {
            text.startsWith(SpringProperties.PREFIX_FILE) -> {
                references += PropertyUtil.getReferenceByFilePrefix(text, psiElement, possibleFileTypes, this)
            }
            text.startsWith(SpringProperties.PREFIX_CLASSPATH) -> {
                references += PropertyUtil.getReferenceByClasspathPrefix(text, SpringProperties.PREFIX_CLASSPATH, psiElement, possibleFileTypes, this)
            }
            else -> {
                references += PropertyUtil.getReferenceWithoutPrefix(text, psiElement, possibleFileTypes, this)
                references += PrefixReference(psiElement, PrefixReferenceType.ANNOTATION_PROPERTY)
            }
        }
        return references.toTypedArray()

        // TODO - add quickFix
        //fileReferenceSet.allReferences.forEach { fileReference ->
        //  PsiFileReferenceHelper.getInstance().registerFixes(fileReference)
        //}
    }

    private fun acceptPsiElement(psiElement: PsiElement): Boolean {
        return possibleAnnotations.contains(getParentAnnotationForPsiLiteralParameter(psiElement)?.qualifiedName)
    }

}