package com.esprito.spring.core.util

import com.esprito.spring.core.SpringProperties
import com.esprito.spring.core.completion.properties.PropertyHint
import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.esprito.spring.core.references.FileReferenceSetWithPrefixSupport
import com.esprito.spring.core.references.ReferenceType
import com.esprito.util.ModuleUtil
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.search.GlobalSearchScope

object PropertyUtil {
    const val DOT = "."

    data class InfoPackage (
        val psiPackage: PsiPackage?,
        val range: TextRange,
        val name: String
    )

    fun getClassNameByQualifiedName(qualifiedNameClass: String?): String {
        return qualifiedNameClass?.substringAfterLast(DOT) ?: ""
    }

    fun getPackageNameByQualifiedName(qualifiedNameClass: String?): String {
        return qualifiedNameClass?.substringBeforeLast(DOT) ?: ""
    }

    fun getPackages(module: Module, qualifiedNameClass: String?): List<InfoPackage> {
        if (qualifiedNameClass.isNullOrBlank()) {
            return emptyList()
        }
        val infoPackages = mutableListOf<InfoPackage>()
        val qualifiedNameList = qualifiedNameClass.split(DOT)
        var qualifiedNamePackage = ""
        for (index in qualifiedNameList.indices) {
            if (index == qualifiedNameList.size - 1) continue
            if (qualifiedNamePackage.isNotBlank()) {
                qualifiedNamePackage += DOT
            }
            qualifiedNamePackage += qualifiedNameList[index]
            val psiPackage = JavaPsiFacade.getInstance(module.project).findPackage(qualifiedNamePackage)

            val firstIndex = qualifiedNameClass.indexOf(qualifiedNameList[index])
            val range = TextRange(firstIndex, firstIndex + qualifiedNameList[index].length)

            infoPackages += InfoPackage(psiPackage, range, qualifiedNameList[index])
        }
        return infoPackages
    }

    fun getPropertyHint(
        module: Module,
        propertyKey: String
    ): PropertyHint? {
        val propertyHint = SpringConfigurationPropertiesSearch.getInstance(module.project)
            .getAllHints(module).find { hint ->
                val hintName = hint.name
                if (hintName == propertyKey) {
                    return@find true
                }
                val valuesIdx = hintName.lastIndexOf(".values")
                if (valuesIdx == -1) {
                    return@find false
                }
                propertyKey.startsWith(hintName.substring(0, valuesIdx))
            }
        return propertyHint
    }

    fun getReferenceByFilePrefix(text: String,
                                 element: PsiElement,
                                 possibleFileTypes: Array<FileType>,
                                 provider: PsiReferenceProvider): Array<FileReference> {
        //("file:./application.properties") or ("file:application.properties") is right link to content root
        // if start with "/" like here @PropertySource("file:/application.properties") - this means it is an absolute path
        var range = ElementManipulators.getValueTextRange(element)
        val referenceType = if (text.startsWith("/")) ReferenceType.ABSOLUTE_PATH else ReferenceType.FILE
        val textWithoutFilePrefix = text.substring(SpringProperties.PREFIX_FILE.length)

        // if start with "/" this means it is an absolute path
        if (textWithoutFilePrefix.startsWith("/")) {
            val basePath = ModuleUtil.getContentRootFile(element)?.path + "/"
            if (textWithoutFilePrefix.startsWith(basePath)) {
                val lengthOfPrefix = SpringProperties.PREFIX_FILE.length + basePath.length
                range = TextRange(range.startOffset + lengthOfPrefix, range.endOffset)
            }
        } else {
            // file: can start with "./" or ""
            val lengthOfPrefix = SpringProperties.PREFIX_FILE.length + text.lengthPrefix("./")
            range = TextRange(range.startOffset + lengthOfPrefix, range.endOffset)
        }
        return FileReferenceSetWithPrefixSupport(
            textWithoutFilePrefix,
            element,
            range.startOffset,
            provider = provider,
            if (possibleFileTypes.isNotEmpty()) possibleFileTypes else null,
            referenceType,
            needHardFileTypeFilter = false
        ).allReferences
    }

    fun getReferenceByClasspathPrefix(text: String,
                                      prefix: String,
                                      element: PsiElement,
                                      possibleFileTypes: Array<FileType>,
                                      provider: PsiReferenceProvider): Array<FileReference> {
        var range = ElementManipulators.getValueTextRange(element)
        val textWithoutClassPathPrefix = text.substring(prefix.length)

        // classpath: can start with "./"  or "/" or ""
        val lengthOfPrefix = prefix.length +
                textWithoutClassPathPrefix.lengthPrefix("/") +
                textWithoutClassPathPrefix.lengthPrefix("./")
        range = TextRange(range.startOffset + lengthOfPrefix, range.endOffset)
        return FileReferenceSetWithPrefixSupport(
            textWithoutClassPathPrefix,
            element,
            range.startOffset,
            provider = provider,
            if (possibleFileTypes.isNotEmpty()) possibleFileTypes else null,
            ReferenceType.CLASSPATH,
            needHardFileTypeFilter = false
        ).allReferences
    }

    fun getReferenceWithoutPrefix(text: String,
                                  element: PsiElement,
                                  possibleFileTypes: Array<FileType>,
                                  provider: PsiReferenceProvider): Array<FileReference> {
        var range = ElementManipulators.getValueTextRange(element)
        val lengthOfPrefix = text.lengthPrefix("/") + text.lengthPrefix("./")
        range = TextRange(range.startOffset + lengthOfPrefix, range.endOffset)

        return FileReferenceSetWithPrefixSupport(
            text,
            element,
            range.startOffset,
            provider = provider,
            if (possibleFileTypes.isNotEmpty()) possibleFileTypes else null,
            ReferenceType.CLASSPATH,
            needHardFileTypeFilter = false
        ).allReferences
    }

    private fun String.lengthPrefix(prefix: String): Int = if (this.startsWith(prefix)) prefix.length else 0

    fun findSourceMember(propertyKey: String, sourceType: String, project: Project): PsiMember? {
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        var memberName = sourceType.substringAfterLast('#', "")
        var setterName = memberName
        if (memberName.isEmpty()) {
            val splitPropsName = propertyKey.substringAfterLast('.').split(Regex("[_\\-]"))
            val firstPropName = splitPropsName.firstOrNull() ?: return null
            memberName = firstPropName + splitPropsName.subList(1, splitPropsName.size).joinToString(separator = "") {
                StringUtil.capitalize(it.lowercase())
            }
            setterName = "set${StringUtil.capitalize(memberName)}"
        }

        @Suppress("NAME_SHADOWING")
        val sourceType = sourceType.substringBeforeLast('#').replace('$', '.')
        val foundClass = javaPsiFacade.findClass(sourceType, GlobalSearchScope.allScope(project)) ?: return null
        return findMember(foundClass, memberName, setterName) ?: foundClass
    }

    private fun findMember(
        foundClass: PsiClass,
        fieldName: String,
        setterName: String
    ): PsiMember? {
        return (foundClass.findMethodsByName(setterName, true).firstOrNull()
            ?: foundClass.findFieldByName(fieldName, true))
    }

    val VALUE_REGEX = """\$\{([^:]*):?(.*)?\}""".toRegex()

}
