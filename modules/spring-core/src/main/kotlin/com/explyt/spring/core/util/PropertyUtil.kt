/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.util

import com.explyt.spring.core.JavaCoreClasses
import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.SpringProperties.PLACEHOLDER_PREFIX
import com.explyt.spring.core.SpringProperties.PLACEHOLDER_SUFFIX
import com.explyt.spring.core.SpringProperties.POSTFIX_VALUES
import com.explyt.spring.core.completion.properties.*
import com.explyt.spring.core.references.FileReferenceSetWithPrefixSupport
import com.explyt.spring.core.references.ReferenceType
import com.explyt.util.ExplytPsiUtil.isNonPrivate
import com.explyt.util.ExplytPsiUtil.isNonStatic
import com.explyt.util.ModuleUtil
import com.intellij.lang.properties.IProperty
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.lang.properties.psi.impl.PropertyValueImpl
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.parentOfType
import com.intellij.util.text.PlaceholderTextRanges
import com.intellij.webSymbols.utils.NameCaseUtils
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLValue

object PropertyUtil {
    const val DOT = "."

    data class InfoPackage(
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
                val valuesIdx = hintName.lastIndexOf(POSTFIX_VALUES)
                if (valuesIdx == -1) {
                    return@find false
                }
                propertyKey.startsWith(hintName.substring(0, valuesIdx))
            }
        return propertyHint
    }

    fun getReferenceByFilePrefix(
        text: String,
        element: PsiElement,
        textRange: TextRange,
        possibleFileTypes: Array<FileType>,
        provider: PsiReferenceProvider?
    ): Array<FileReference> {
        // ("file:./application.properties") or ("file:application.properties") is right link to content root
        // if start with "/" like here @PropertySource("file:/application.properties") - this means it is an absolute path
        var range = textRange
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

    fun getReferenceByClasspathPrefix(
        text: String,
        prefix: String,
        element: PsiElement,
        textRange: TextRange,
        possibleFileTypes: Array<FileType>,
        provider: PsiReferenceProvider?
    ): Array<FileReference> {
        val textWithoutClassPathPrefix = text.substring(prefix.length)

        // classpath: can start with "./"  or "/" or ""
        val lengthOfPrefix = prefix.length +
                textWithoutClassPathPrefix.lengthPrefix("/") +
                textWithoutClassPathPrefix.lengthPrefix("./")
        val range = TextRange(textRange.startOffset + lengthOfPrefix, textRange.endOffset)
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

    fun getReferenceWithoutPrefix(
        text: String,
        element: PsiElement,
        textRange: TextRange,
        possibleFileTypes: Array<FileType>,
        provider: PsiReferenceProvider?
    ): Array<FileReference> {
        val lengthOfPrefix = text.lengthPrefix("/") + text.lengthPrefix("./")
        val range = TextRange(textRange.startOffset + lengthOfPrefix, textRange.endOffset)

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

    fun prefixValue(prefix: String?): String {
        return when {
            prefix.isNullOrBlank() -> ""
            prefix.endsWith('.') -> prefix
            else -> "$prefix."
        }
    }


    fun findSourceMember(propertyKey: String, sourceType: String, project: Project): PsiMember? {
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        var memberName = sourceType.substringAfterLast('#', "")
        var setterName = memberName
        if (memberName.isEmpty()) {
            val splitPropsName = propertyKey
                .substringAfterLast('.')
                .split(PROPERTY_WORDS_SEPARATOR_REGEX)
            val firstPropName = splitPropsName.firstOrNull() ?: return null
            memberName = firstPropName + splitPropsName.subList(1, splitPropsName.size).joinToString(separator = "") {
                StringUtil.capitalize(it.lowercase())
            }
            setterName = "set${StringUtil.capitalize(memberName)}"
        }

        val qualifiedName = sourceType.substringBeforeLast('#').replace('$', '.')
        val foundClass = javaPsiFacade.findClass(qualifiedName, GlobalSearchScope.allScope(project)) ?: return null
        return findMember(foundClass, memberName, setterName) ?: foundClass
    }

    fun PsiElement.propertyKey(): String? {
        if (this is IProperty) {
            return this.key
        }
        val property = this.parentOfType<PropertyImpl>()
        if (property != null) {
            return property.key
        }
        if (this is YAMLKeyValue) {
            return YAMLUtil.getConfigFullName(this)
        }
        return null
    }

    fun PsiElement.propertyValue(): String? {
        if (this is IProperty) {
            return this.value
        }
        val property = this.parentOfType<PropertyImpl>()
        if (property != null) {
            return property.value
        }
        if (this is YAMLKeyValue) {
            return this.value?.text
        }
        return null
    }

    fun PsiElement.propertyKeyPsiElement(): PsiElement? {
        if (this is IProperty) {
            return this.childrenOfType<PropertyKeyImpl>().firstOrNull()
        }
        if (this is PropertyKeyImpl) {
            return this
        }
        if (this is YAMLKeyValue) {
            val yamlKey = YAMLUtil.getConfigFullName(this)
            if (yamlKey.substringAfterLast(DOT) == this.key?.text) {
                return this.key
            }
        }
        return null
    }

    fun PsiElement.propertyValuePsiElement(): PsiElement? {
        if (this is IProperty) {
            return this.childrenOfType<PropertyValueImpl>().firstOrNull()
        }
        if (this is PropertyValueImpl) {
            return this
        }
        if (this is YAMLKeyValue) {
            val yamlValue = PsiTreeUtil.collectElementsOfType(this, YAMLValue::class.java).lastOrNull()
            if (yamlValue != null && yamlValue.text == this.value?.text) {
                return this.value
            }
        }
        return null
    }

    fun PsiElement.textRangePropertyKeyMap(prefixLength: Int, valueHint: ValueHint): TextRange? {
        if (valueHint.value == null) {
            return null
        }
        if (this is PropertyKeyImpl) {
            return TextRange.from(0, prefixLength + 1 + valueHint.value.length)
        }
        if (this is YAMLKeyValue) {
            return TextRange.from(prefixLength, valueHint.value.length)
        }
        return null
    }

    fun isSameProperty(propertyName1: String, propertyName2: String): Boolean {
        return toCommonPropertyForm(propertyName1) == toCommonPropertyForm(propertyName2)
    }

    fun guessTypeFromValue(value: String?): String {
        return when {
            value == null -> CommonClassNames.JAVA_LANG_STRING
            StringUtil.equalsIgnoreCase(value, "true") -> CommonClassNames.JAVA_LANG_BOOLEAN
            StringUtil.equalsIgnoreCase(value, "false") -> CommonClassNames.JAVA_LANG_BOOLEAN
            INT_REGEX.matches(value) -> CommonClassNames.JAVA_LANG_INTEGER
            DOUBLE_REGEX.matches(value) -> CommonClassNames.JAVA_LANG_DOUBLE
            else -> CommonClassNames.JAVA_LANG_STRING
        }
    }

    fun <T> getPlaceholders(from: String, mapping: (placeholder: String, range: TextRange) -> T): List<T> {
        val ranges = PlaceholderTextRanges.getPlaceholderRanges(
            from,
            PLACEHOLDER_PREFIX,
            PLACEHOLDER_SUFFIX
        )
        val result = mutableListOf<T>()
        for (range in ranges) {
            val index = range.substring(from).indexOf(SpringProperties.COLON)
            val textInRange =
                if (index == -1) range.substring(from) else range.substring(from)
                    .substringBefore(SpringProperties.COLON)

            result.add(mapping.invoke(textInRange, range))
        }
        return result
    }


    fun toCommonPropertyForm(propertyName: String): String {
        return propertyName.lowercase()
            .replace("-", "")
            .replace("_", "")
    }

    fun isNotKebabCase(placeholder: String): Boolean {
        return placeholder.any { it.isUpperCase() || it == '_' }
    }

    fun toKebabCase(from: String): String {
        return from.splitToSequence('.')
            .map { NameCaseUtils.toKebabCase(it) }
            .joinToString(".")
    }

    fun isKebabCaseInMapKey(key: String, properties: List<ConfigurationProperty>): Boolean {
        val parts = key.split(".")

        val keyVariant = buildString {
            for (part in parts) {
                if (part.any { it.isUpperCase() }) break
                if (isNotEmpty()) append(".")
                append(part)
            }
        }
        val property = properties.firstOrNull { isSameProperty(it.name, keyVariant) } ?: return false
        return property.isMap()
    }

    fun getValueClassNameInMap(propertyType: String?): String? {
        if (propertyType == null) return null
        if (propertyType.substringBefore("<") == JavaCoreClasses.MAP) {
            return getClassNameFromInnerTypeInMap(propertyType)
        }
        return null
    }

    private fun getClassNameFromInnerTypeInMap(propertyType: String): String? {
        val keyType = propertyType.substringAfter(",").substringBeforeLast(">")
        if (keyType != propertyType) {
            return keyType
        }
        return null
    }

    fun getSetterMethods(
        targetClass: PsiClass,
        nestedFields: List<FieldPropertyWrapper>
    ): List<PsiMethod> {
        return targetClass.allMethods.filter {
            it.isNonStatic
                    && it.isNonPrivate
                    && it.parameterList.parametersCount == 1
                    && isPrefixedJavaIdentifier(it.name, "set")
                    && it.parameterList.parameters[0].type !in nestedFields.map { field -> field.psiType }
        }.filterNotNull()
    }

    fun getGetterMethods(
        targetClass: PsiClass,
        nestedFields: List<FieldPropertyWrapper>
    ): List<PsiMethod> {
        return targetClass.allMethods.filter {
            it.isNonStatic
                    && it.isNonPrivate
                    && it.parameterList.parametersCount == 0
                    && (isPrefixedJavaIdentifier(it.name, "get") || isPrefixedJavaIdentifier(it.name, "is"))
                    && it.returnType !in nestedFields.map { field -> field.psiType }
        }.filterNotNull()
    }

    private fun isPrefixedJavaIdentifier(name: String, prefix: String): Boolean {
        return name.startsWith(prefix) && name.length > prefix.length
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
    private val PROPERTY_WORDS_SEPARATOR_REGEX = """[_\-]""".toRegex()
    private val INT_REGEX = "[-+]?[0-9]+".toRegex()
    private val DOUBLE_REGEX = "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?".toRegex()

}
