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
import com.explyt.spring.core.PrimitiveTypes
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.SpringProperties.PLACEHOLDER_PREFIX
import com.explyt.spring.core.SpringProperties.PLACEHOLDER_SUFFIX
import com.explyt.spring.core.SpringProperties.POSTFIX_VALUES
import com.explyt.spring.core.completion.properties.*
import com.explyt.spring.core.completion.properties.ConfigurationPropertiesLoader.Companion.getKeyPsiClass
import com.explyt.spring.core.properties.dataRetriever.ConfigurationPropertyDataRetriever
import com.explyt.spring.core.properties.dataRetriever.ConfigurationPropertyDataRetrieverFactory
import com.explyt.spring.core.properties.providers.ConfigKeyPsiElement
import com.explyt.spring.core.references.FileReferenceSetWithPrefixSupport
import com.explyt.spring.core.references.ReferenceType
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.util.SpringCoreUtil.isCharsetTypeClass
import com.explyt.spring.core.util.SpringCoreUtil.isMimeTypeClass
import com.explyt.util.ExplytPsiUtil.isFinal
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isNonPrivate
import com.explyt.util.ExplytPsiUtil.isNonStatic
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.explyt.util.ModuleUtil
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.lang.properties.IProperty
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.lang.properties.psi.impl.PropertyValueImpl
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
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
import org.jetbrains.uast.*
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
        return FileReferenceSetWithPrefixSupport(
            text,
            element,
            textRange.startOffset,
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
            val thisKeyText = this.key?.text ?: return null
            if (yamlKey.endsWith(thisKeyText)) {
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

    fun isSameProperty(propertyName1: String, propertyName2: String, type: String? = null): Boolean {
        val property1 = toBooleanAlias(propertyName1, type)
        val property2 = toBooleanAlias(propertyName2, type)
        return toCommonPropertyForm(property1) == toCommonPropertyForm(property2)
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
        ranges.forEach {
            val rangeText = it.substring(from)
            // Check if a placeholder is within comment
            if (from.lastIndexOf(SpringProperties.PROPERTY_COMMENT).let { it >= 0 && it > from.lastIndexOf("\n") }) {
                return@forEach
            }
            val colonIndex = rangeText.indexOf(SpringProperties.COLON)

            val range = if (colonIndex != -1) TextRange(it.startOffset, it.startOffset + colonIndex) else it
            val textInRange = range.substring(from)
            result.add(mapping.invoke(textInRange, range))
        }
        return result
    }


    fun toCommonPropertyForm(propertyName: String): String {
        return propertyName.lowercase()
            .replace("-", "")
            .replace("_", "")
    }

    fun toSystemEnvironmentForm(propertyName: String): String {
        return toCommonPropertyForm(propertyName).uppercase().replace(".", "_")
    }

    fun toBooleanAlias(property: String, type: String?): String {
        return if (type == JavaCoreClasses.BOOLEAN || type == PrimitiveTypes.BOOLEAN) {
            val result = property.substringBeforeLast(".")
            var name = property.substringAfterLast(".", "")
            if (name.isNotBlank() && !name.startsWith("is-")) {
                name = "is-$name"
            }
            "$result.$name"
        } else {
            property
        }
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
        return properties.any { it.isMap() && key.startsWith(it.name) && key != it.name }
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

    fun collectConfigurationProperty(
        module: Module,
        ownerConfigurationClass: PsiClass,
        targetClass: PsiClass,
        prefix: String,
        result: MutableMap<String, ConfigurationProperty>,
        visitedClasses: MutableSet<String> = mutableSetOf(),
        depth: Int = 0
    ) {
        //check recursion depth
        if (depth > 10) return

        val qualifiedNameClass = targetClass.qualifiedName ?: return
        if (!visitedClasses.add("$prefix:$qualifiedNameClass")) return

        val nestedFields = getNestedPropertyWrappers(targetClass)
        val finalFields = getFieldPropertyWrappers(targetClass)
        val setterMethods = getMethodPropertyWrappers(module, targetClass, nestedFields)

        for (it in nestedFields) {
            val propertyTypeClass = it.psiType.resolvedPsiClass
            if (propertyTypeClass != null && propertyTypeClass.qualifiedName != qualifiedNameClass) {
                collectConfigurationProperty(
                    module,
                    ownerConfigurationClass,
                    propertyTypeClass,
                    "$prefix.${it.name}",
                    result,
                    visitedClasses,
                    depth + 1
                )
            }
        }

        val propertyWrappers = finalFields + setterMethods
        for (propertyWrapper in propertyWrappers) {
            val propertyName = propertyWrapper.name
            val psiType = propertyWrapper.psiType

            val name = if (prefix.isEmpty()) "$propertyName" else "$prefix.$propertyName"
            if (psiType is PsiClassType) {
                val propertyTypeClass = psiType.resolve()
                val javaFile = propertyTypeClass?.containingFile as? PsiJavaFile ?: continue

                if (javaFile.packageName != JavaCoreClasses.PACKAGE_JAVA_LANG
                    && javaFile.packageName != JavaCoreClasses.PACKAGE_JAVA_TIME
                    && javaFile.packageName != JavaCoreClasses.PACKAGE_KOTLIN
                    && propertyTypeClass.qualifiedName != qualifiedNameClass
                ) {
                    collectConfigurationProperty(
                        module,
                        ownerConfigurationClass,
                        propertyTypeClass,
                        name,
                        result,
                        visitedClasses,
                        depth + 1
                    )
                }
            }

            result[name] = ConfigurationProperty(
                name,
                ConfigurationPropertiesLoader.getPropertyType(propertyWrapper.psiType),
                propertyWrapper.type,
                propertyWrapper.sourceType,
                propertyWrapper.description,
                propertyWrapper.default,
                propertyWrapper.deprecation
            )
        }
    }

    fun getEnumProperties(project: Project, property: ConfigurationProperty): List<ConfigurationProperty> {
        if (property.propertyType != PropertyType.ENUM_MAP) return emptyList()
        val enumPsiClass = getKeyPsiClass(property.type, project) ?: return emptyList()
        val valuePsiClass = ConfigurationPropertiesLoader.getValuePsiClass(property.type, project)
        val uKeyClass = enumPsiClass.toUElement() as? UClass ?: return emptyList()
        val enumKeys = uKeyClass.uastDeclarations.filterIsInstance<UEnumConstant>()
        return enumKeys.map {
            val propName = "${property.name}.${it.name}"
            ConfigurationProperty(
                propName,
                PropertyType.ENUM_MAP,
                valuePsiClass?.qualifiedName,
                enumPsiClass.qualifiedName,
                null, null,
                getDeprecationInfo(it)
            )
        }
    }

    private fun getMethodPropertyWrappers(
        module: Module,
        targetClass: PsiClass,
        nestedFields: List<FieldPropertyWrapper>
    ): List<MethodPropertyWrapper> {
        val result = mutableListOf<MethodPropertyWrapper>()
        val setterMethods = getSetterMethods(targetClass, nestedFields)
        val getterMethods = getGetterMethods(targetClass, nestedFields)
        for (method in setterMethods) {
            val setterIdentifier = getIdentifierFromSetterMethod(method) ?: continue
            var methodPropertyWrapper: MethodPropertyWrapper? = null
            for (it in getterMethods) {
                val getterIdentifier = getIdentifierFromGetterMethod(it)
                if (getterIdentifier != null && getterIdentifier.text == setterIdentifier.text) {
                    val deprecation = getDeprecationInfo(module, it)
                    methodPropertyWrapper = MethodPropertyWrapper(method, deprecation)
                }
            }
            if (methodPropertyWrapper == null) {
                methodPropertyWrapper = MethodPropertyWrapper(method, null)
            }
            result += methodPropertyWrapper
        }
        return result
    }

    private fun getNestedPropertyWrappers(targetClass: PsiClass): List<FieldPropertyWrapper> {
        return targetClass.allFields
            .filter { it.isMetaAnnotatedBy(SpringCoreClasses.NESTED_CONFIGURATION_PROPERTIES) }
            .map { FieldPropertyWrapper(it) }
    }

    private fun getFieldPropertyWrappers(targetClass: PsiClass): List<FieldPropertyWrapper> {
        val fields = if (targetClass.isEnum || targetClass.isCharsetTypeClass() || targetClass.isMimeTypeClass()) {
            emptyList()
        } else {
            targetClass.fields.filter {
                it.isNonStatic
                        && it.isFinal
                        && !it.isMetaAnnotatedBy(SpringCoreClasses.NESTED_CONFIGURATION_PROPERTIES)
            }
        }
        return fields.map { FieldPropertyWrapper(it) }
    }

    private fun getSetterMethods(
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

    private fun getGetterMethods(
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

    private fun getIdentifierFromSetterMethod(psiMethod: PsiMethod): PsiIdentifier? {
        val codeBlock = psiMethod.childrenOfType<PsiCodeBlock>()
        if (codeBlock.isEmpty()) {
            return psiMethod.identifyingElement as? PsiIdentifier
        }
        val expressions = codeBlock[0].childrenOfType<PsiExpressionStatement>()
        val assignmentExpressions = expressions.asSequence()
            .map { it.childrenOfType<PsiAssignmentExpression>() }
            .filter { it.isNotEmpty() }
            .toList()
        return assignmentExpressions
            .asSequence()
            .flatMap { it.asSequence() }
            .filter { it.lastChild != null }
            .filter {
                it.lastChild.text == psiMethod.parameterList.parameters[0].lastChild?.text
                        || it.lastChild.text == psiMethod.parameterList.parameters[0].name
            }
            .map { it.firstChild }
            .filterIsInstance<PsiReferenceExpression>()
            .map { it.childrenOfType<PsiIdentifier>().firstOrNull() }
            .firstOrNull { it != null }
    }

    private fun getIdentifierFromGetterMethod(psiMethod: PsiMethod): PsiIdentifier? {
        val codeBlock = psiMethod.childrenOfType<PsiCodeBlock>()
        if (codeBlock.isEmpty()) {
            return psiMethod.identifyingElement as? PsiIdentifier
        }
        val expressions = codeBlock.flatMap { it.childrenOfType<PsiReturnStatement>() }

        return expressions.lastOrNull()
            ?.childrenOfType<PsiReferenceExpression>()?.lastOrNull()
            ?.childrenOfType<PsiIdentifier>()?.lastOrNull()
    }

    private fun getDeprecationInfo(module: Module, method: PsiMethod?): DeprecationInfo? {
        if (method == null || !method.isDeprecated) {
            return null
        }
        val metaHolder = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringCoreClasses.DEPRECATED_CONFIGURATION_PROPERTIES)
        val reasonAnnotation = metaHolder.getAnnotationMemberValues(method, setOf("reason")).firstOrNull()
        val replacementAnnotation = metaHolder.getAnnotationMemberValues(method, setOf("replacement")).firstOrNull()
        val reason = if (reasonAnnotation != null) AnnotationUtil.getStringAttributeValue(reasonAnnotation) else null
        val replacement =
            if (replacementAnnotation != null) AnnotationUtil.getStringAttributeValue(replacementAnnotation) else null
        return DeprecationInfo(DeprecationInfoLevel.WARNING, replacement, reason)
    }

    private fun findMember(
        foundClass: PsiClass,
        fieldName: String,
        setterName: String
    ): PsiMember? {
        return (foundClass.findMethodsByName(setterName, true).firstOrNull()
            ?: foundClass.findFieldByName(fieldName, true))
    }

    fun configurationProperty(module: Module, propertyKey: String): ConfigurationProperty? {
        val findProperty = SpringConfigurationPropertiesSearch.getInstance(module.project)
            .findProperty(module, propertyKey)
        if (findProperty == null) {
            return SpringConfigurationPropertiesSearch.getInstance(module.project).getAllProperties(module)
                .find { propertyKey.startsWith(it.name) }
        }
        return findProperty
    }

    fun resolveResults(sourceMember: PsiMember): Array<ResolveResult> {
        val uElement = sourceMember.toUElement() ?: return emptyArray()

        return if (uElement is UClass || uElement is UMethod || uElement is UField) {
            PsiElementResolveResult.createResults(ConfigKeyPsiElement(sourceMember))
        } else {
            emptyArray()
        }
    }

    fun getMethodsTypeByMap(module: Module, valueType: String, prefix: String): List<PsiMember> {
        val result = hashMapOf<String, ConfigurationProperty>()
        val project = module.project
        val qualifiedName = valueType.substringBeforeLast('#').replace('$', '.')
        val foundClass =
            JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.allScope(project))
                ?: return emptyList()

        collectConfigurationProperty(module, foundClass, foundClass, "", result)

        return result.asSequence()
            .filter {
                isSameProperty(it.key.substringAfter("."), prefix)
                        || isSameProperty(it.key, prefix)
            }
            .map { value -> value.value.sourceType?.let { findSourceMember(prefix, it, project) } }
            .filterNotNullTo(mutableListOf())
    }

    fun isNameSetMethod(name: String?, propertyMapValue: String): Boolean {
        return name?.lowercase() ==
                "set${
                    propertyMapValue
                        .substringAfterLast(".")
                        .replace("-", "")
                        .replace("_", "")
                        .lowercase()
                }"
    }

    fun findPropertyByConfigurationPropertyElement(element: PsiElement): PropertySearchResult? {
        if (!SpringCoreUtil.isSpringProject(element.project)) return null
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return null

        val dataRetriever = ConfigurationPropertyDataRetrieverFactory.createFor(element.toUElement()) ?: return null
        val psiClass = dataRetriever.getContainingClass() ?: return null
        val memberName = dataRetriever.getMemberName() ?: return null
        val prefixValue = ConfigurationPropertyDataRetriever.getPrefixValue(psiClass)

        if (prefixValue.isBlank()) return null

        val properties = dataRetriever.getRelatedProperties(prefixValue, memberName, module)
        return PropertySearchResult(prefixValue, properties)
    }

    private fun getDeprecationInfo(item: UEnumConstant): DeprecationInfo? {
        val annotationDeprecated = item.uAnnotations
            .find { it.qualifiedName?.contains(java.lang.Deprecated::class.java.simpleName) == true } ?: return null
        return DeprecationInfo(DeprecationInfoLevel.WARNING, reason = annotationDeprecated.asRenderString())
    }

    fun getKeyValuePair(propertyKey: String, foundProperty: ConfigurationProperty): Pair<String, String> {
        if (propertyKey == propertyKey.substringAfter("${foundProperty.name}.")) return Pair("", "")
        val propertyMapKey = propertyKey.substringAfter("${foundProperty.name}.").substringBefore(".")
        var propertyMapValue = propertyKey.substringAfter("$propertyMapKey.")
        if (propertyMapValue == propertyKey) propertyMapValue = ""
        return Pair(propertyMapKey, propertyMapValue)
    }

    val VALUE_REGEX = """\$\{([^:]*):?(.*)?\}""".toRegex()
    private val PROPERTY_WORDS_SEPARATOR_REGEX = """[_\-]""".toRegex()
    private val INT_REGEX = "[-+]?[0-9]+".toRegex()
    private val DOUBLE_REGEX = "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?".toRegex()

}

data class PropertySearchResult(
    val prefix: String,
    val properties: List<PsiElement>
)