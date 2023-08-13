package com.esprito.spring.core.providers

import com.esprito.spring.core.SpringCoreClasses.CLASSPATH_PREFIX
import com.esprito.spring.core.SpringCoreClasses.FILE_PREFIX
import com.esprito.spring.core.references.FileReferenceSetWithPrefixSupport
import com.esprito.spring.core.references.REFERENCE_TYPE
import com.esprito.spring.core.util.PsiAnnotationUtils.getParentAnnotationForPsiLiteralParameter
import com.esprito.util.ModuleUtil
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.TextRange
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

        val references = mutableListOf<PsiReference>()

        var range = ElementManipulators.getValueTextRange(psiElement)
        val text = psiElement.value.toString()

        val referenceType: REFERENCE_TYPE
        //("file:./application.properties") or ("file:application.properties") is right link to content root
        // if start with "/" like here @PropertySource("file:/application.properties") - it mean absolute path
        if (text.startsWith(FILE_PREFIX)) {
            val textWithoutFilePrefix = text.substring(FILE_PREFIX.length)
            // if start with "/" it mean that it is absolute path
            if (textWithoutFilePrefix.startsWith("/")) {
                referenceType = REFERENCE_TYPE.ABSOLUTE_PATH

                val basePath = ModuleUtil.getContentRootFile(psiElement)?.path + "/"
                if (textWithoutFilePrefix.startsWith(basePath)) {
                    val lengthOfPrefix = FILE_PREFIX.length + basePath.length
                    range = TextRange(
                        range.startOffset + lengthOfPrefix,
                        range.endOffset
                    )
                }
            } else {
                // file: can start with "./" or ""
                referenceType = REFERENCE_TYPE.FILE

                val lengthOfPrefix = FILE_PREFIX.length +
                        (if (textWithoutFilePrefix.startsWith("./")) "./".length else 0)
                range = TextRange(range.startOffset + lengthOfPrefix, range.endOffset)
            }
        } else {
            // classpath: can start with "./"  or "/" or ""
            referenceType = REFERENCE_TYPE.CLASSPATH

            if (text.startsWith(CLASSPATH_PREFIX)) {
                val textWithoutClassPathPrefix = text.substring(CLASSPATH_PREFIX.length)
                val lengthOfPrefix = CLASSPATH_PREFIX.length +
                        (if (textWithoutClassPathPrefix.startsWith("/")) "/".length else 0) +
                        (if (textWithoutClassPathPrefix.startsWith("./")) "./".length else 0)
                range = TextRange(range.startOffset + lengthOfPrefix, range.endOffset)
            } else {
                val lengthOfPrefix =
                    (if (text.startsWith("/")) "/".length else 0) +
                            (if (text.startsWith("./")) "./".length else 0)
                range = TextRange(range.startOffset + lengthOfPrefix, range.endOffset)
            }
        }

        val fileReferenceSet = FileReferenceSetWithPrefixSupport(
            range.substring(psiElement.text),
            psiElement,
            range.startOffset,
            provider = this,
            isCaseSensitive = false,
            endingSlashNotAllowed = false,
            if (possibleFileTypes.isNotEmpty()) possibleFileTypes else null,
            init = true,
            referenceType,
            needHardFileTypeFilter = false
        )

        // TODO - add quickFix
        //fileReferenceSet.allReferences.forEach { fileReference ->
        //  PsiFileReferenceHelper.getInstance().registerFixes(fileReference)
        //}

        references.addAll(fileReferenceSet.allReferences)//.filter { it?.resolve() != null })

        return references.toTypedArray()
    }

    private fun acceptPsiElement(psiElement: PsiElement): Boolean {
        return possibleAnnotations.contains(getParentAnnotationForPsiLiteralParameter(psiElement)?.qualifiedName)
    }

}