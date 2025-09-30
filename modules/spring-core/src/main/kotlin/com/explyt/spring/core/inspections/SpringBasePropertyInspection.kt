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

package com.explyt.spring.core.inspections

import ai.grazie.nlp.utils.dropLastWhitespaces
import ai.grazie.nlp.utils.dropWhitespaces
import com.explyt.inspection.SpringBaseLocalInspectionTool
import com.explyt.spring.core.*
import com.explyt.spring.core.SpringCoreClasses.IO_RESOURCE
import com.explyt.spring.core.SpringProperties.PLACEHOLDER_PREFIX
import com.explyt.spring.core.SpringProperties.PLACEHOLDER_SUFFIX
import com.explyt.spring.core.SpringProperties.POSTFIX_KEYS
import com.explyt.spring.core.SpringProperties.POSTFIX_VALUES
import com.explyt.spring.core.completion.properties.*
import com.explyt.spring.core.inspections.quickfix.ReplacementKeyQuickFix
import com.explyt.spring.core.inspections.utils.ResourceFileInspectionUtil
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.service.SpringSearchUtils
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.PropertyUtil.propertyKey
import com.explyt.spring.core.util.PropertyUtil.propertyKeyPsiElement
import com.explyt.spring.core.util.PropertyUtil.propertyValue
import com.explyt.spring.core.util.PropertyUtil.propertyValuePsiElement
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.util.CacheKeyStore
import com.intellij.codeInspection.*
import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR
import com.intellij.lang.properties.PropertiesBundle
import com.intellij.lang.properties.PropertiesFileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.uast.UastModificationTracker
import com.intellij.xml.util.documentation.MimeTypeDictionary
import java.nio.charset.Charset
import java.text.DateFormat

abstract class SpringBasePropertyInspection : SpringBaseLocalInspectionTool() {

    protected abstract fun loadFileProperties(file: PsiFile): List<DefinedConfigurationProperty>

    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): Array<ProblemDescriptor>? {
        if (!SpringCoreUtil.isConfigurationPropertyFile(file)) {
            return ProblemDescriptor.EMPTY_ARRAY
        }

        val fileProperties = loadFileProperties(file)
        if (fileProperties.isEmpty()) {
            return ProblemDescriptor.EMPTY_ARRAY
        }

        val module = ModuleUtilCore.findModuleForFile(file) ?: return ProblemDescriptor.EMPTY_ARRAY

        val problems = mutableListOf<ProblemDescriptor>()
        problems += checkKey(module, file, manager, isOnTheFly, fileProperties)
        problems += checkValue(module, manager, isOnTheFly, fileProperties)
        problems += checkDuplicateKeys(manager, file, isOnTheFly, fileProperties)
        return problems.toTypedArray()

    }

    private fun checkKey(
        module: Module,
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean,
        fileProperties: List<DefinedConfigurationProperty>,
    ): MutableList<ProblemDescriptor> {
        val configurationProperties =
            SpringConfigurationPropertiesSearch.getInstance(module.project).getAllProperties(module)
        if (configurationProperties.isEmpty()) {
            return mutableListOf()
        }
        val problems = mutableListOf<ProblemDescriptor>()
        problems += getProblemKey(manager, file, isOnTheFly, fileProperties)
        problems += getProblemPropertyDeprecated(manager, isOnTheFly, configurationProperties, fileProperties)
        return problems
    }

    private fun getProblemKey(
        manager: InspectionManager,
        file: PsiFile,
        isOnTheFly: Boolean,
        fileProperties: List<DefinedConfigurationProperty>,
    ): List<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val module = ModuleUtilCore.findModuleForPsiElement(file) ?: return emptyList()

        val properties = SpringConfigurationPropertiesSearch.getInstance(module.project)
            .getAllProperties(module)

        for (fileProperty in fileProperties) {
            val elementFileProperty = fileProperty.psiElement ?: continue
            val psiKey = elementFileProperty.propertyKeyPsiElement() ?: continue

            if (fileProperty.key in PROHIBITED_IN_PROFILE_PROPERTIES
                && PROFILE_PROPERTIES_FILE_MASK.matches(file.name)
            ) {
                problems += manager.createProblemDescriptor(
                    psiKey,
                    SpringCoreBundle.message("explyt.spring.inspection.properties.key.prohibited", fileProperty.key),
                    isOnTheFly,
                    emptyArray(),
                    GENERIC_ERROR
                )
            }

            val key = fileProperty.key
            if (PropertyUtil.isNotKebabCase(key) && !PropertyUtil.isKebabCaseInMapKey(key, properties)) {
                problems += keyShouldBeKebabProblemDescriptor(manager, psiKey, isOnTheFly, key)
            }

            val foundProperties = properties.filter { PropertyUtil.isSameProperty(it.name, key, it.type) }
            val placeholders = DefinedConfigurationPropertiesSearch.getInstance(module.project)
                .getAllPlaceholders(module)

            if (foundProperties.isEmpty()
                && getMapKeys(fileProperty, properties).isEmpty()
                && getListKeys(fileProperty, properties).isEmpty()
                && placeholders.none { PropertyUtil.isSameProperty(key, it) }
            ) {
                val baseKeyPrefix = key.substringBefore(".")
                if (properties.none { it.name.startsWith(baseKeyPrefix) }) {
                    //this means that the property is defined directly in the file
                    continue
                }
                val psiReferences = SpringSearchUtils.getAllReferencesToElement(elementFileProperty)
                if (psiReferences.isEmpty()) {
                    problems += manager.createProblemDescriptor(
                        psiKey,
                        SpringCoreBundle.message("explyt.spring.inspection.properties.key.unresolved", key),
                        isOnTheFly,
                        emptyArray(),
                        ProblemHighlightType.WARNING
                    )
                }
            } else {
                for (it in foundProperties) {
                    if (it.isMap()) {
                        problems += manager.createProblemDescriptor(
                            psiKey,
                            SpringCoreBundle.message("explyt.spring.inspection.properties.key.map.not.found.key"),
                            isOnTheFly,
                            emptyArray(),
                            ProblemHighlightType.ERROR
                        )
                    }
                }
            }
        }
        return problems
    }

    abstract fun keyShouldBeKebabProblemDescriptor(
        manager: InspectionManager,
        psiKey: PsiElement,
        isOnTheFly: Boolean,
        key: String
    ): ProblemDescriptor

    private fun getProblemPropertyDeprecated(
        manager: InspectionManager,
        isOnTheFly: Boolean,
        configurationProperties: List<ConfigurationProperty>,
        fileProperties: List<DefinedConfigurationProperty>,
    ): MutableList<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val deprecationProperties = configurationProperties.filter { property ->
            fileProperties.any { fileProperty -> fileProperty.key == property.name && property.deprecation != null }
        }
        if (deprecationProperties.isEmpty()) {
            return problems
        }
        for (property in deprecationProperties) {
            val psiElement = fileProperties.firstOrNull { it.key == property.name }?.psiElement ?: continue
            val psiKey = psiElement.propertyKeyPsiElement() ?: continue
            val level = property.deprecation?.level ?: continue
            val replacement = property.deprecation?.replacement ?: " "
            val reason = property.deprecation?.reason
                ?: if (replacement.isNotEmpty()) "replacement to \"$replacement\""
                else "reason unknown"
            problems += manager.createProblemDescriptor(
                psiKey,
                SpringCoreBundle.message("explyt.spring.inspection.properties.key.deprecated", reason),
                isOnTheFly,
                arrayOf(ReplacementKeyQuickFix(replacement, psiElement)),
                if (level == DeprecationInfoLevel.ERROR) GENERIC_ERROR else ProblemHighlightType.LIKE_DEPRECATED
            )
        }

        return problems
    }

    private fun checkValue(
        module: Module,
        manager: InspectionManager,
        isOnTheFly: Boolean,
        fileProperties: List<DefinedConfigurationProperty>,
    ): MutableList<ProblemDescriptor> {
        val hints = SpringConfigurationPropertiesSearch.getInstance(module.project).getAllHints(module)
        val problems = mutableListOf<ProblemDescriptor>()
        problems += getProblemValues(manager, isOnTheFly, hints, fileProperties)
        problems += getProblemClassReference(module, manager, isOnTheFly, hints, fileProperties)
        problems += getProblemHandleAs(module, manager, isOnTheFly, hints, fileProperties)
        problems += getProblemBeanReferenceProperties(module, manager, isOnTheFly, hints, fileProperties)
        problems += getProblemPropertyType(module, manager, isOnTheFly, fileProperties)
        problems += getProblemResource(manager, isOnTheFly, hints, fileProperties)
        return problems
    }

    private fun getProblemValues(
        manager: InspectionManager,
        isOnTheFly: Boolean,
        hints: List<PropertyHint>,
        fileProperties: List<DefinedConfigurationProperty>,
    ): MutableList<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val findInFileProperties = fileProperties.filter { property ->
            hints.any { hint ->
                (property.key == hint.name || property.key.substringBeforeLast(".") + POSTFIX_KEYS == hint.name)
                        && hint.values.isNotEmpty()
                        && (hint.providers.isEmpty()
                        || hint.providers.filter { it.name != null }.any { it.name != SpringProperties.ANY })
            }
        }
        if (findInFileProperties.isEmpty()) {
            return problems
        }
        for (fileProperty in findInFileProperties) {
            val elementFileProperty = fileProperty.psiElement ?: continue
            val psiValue = elementFileProperty.propertyValuePsiElement() ?: continue
            val key = elementFileProperty.propertyKey() ?: continue
            val value = elementFileProperty.propertyValue() ?: continue
            if (value.startsWith(PLACEHOLDER_PREFIX) && value.endsWith(PLACEHOLDER_SUFFIX)) {
                continue
            }

            val hintValues = hints.asSequence()
                .filter { it.name == key || it.name == key.substringBeforeLast(".") + POSTFIX_VALUES }
                .distinctBy { it.name }
                .flatMap { it.values }
                .mapNotNull { it.value }
                .toList()

            if (value !in hintValues) {
                problems += manager.createProblemDescriptor(
                    psiValue,
                    SpringCoreBundle.message(
                        "explyt.spring.inspection.properties.value.unresolved.static",
                        value,
                        hintValues
                    ),
                    isOnTheFly,
                    emptyArray(),
                    ProblemHighlightType.ERROR
                )
            }
        }
        return problems
    }

    private fun getProblemClassReference(
        module: Module,
        manager: InspectionManager,
        isOnTheFly: Boolean,
        hints: List<PropertyHint>,
        fileProperties: List<DefinedConfigurationProperty>,
    ): MutableList<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val classReferenceProperties = fileProperties.filter { property ->
            hints.any { hint ->
                property.key == hint.name
                        && hint.providers.filter { it.name != null }.any { it.name == SpringProperties.CLASS_REFERENCE }
            }
        }
        if (classReferenceProperties.isEmpty()) {
            return problems
        }

        for (property in classReferenceProperties) {
            val elementFileProperty = property.psiElement ?: continue
            val psiValue = elementFileProperty.propertyValuePsiElement() ?: continue
            val qualifiedNameClass = property.value ?: continue

            problems += getProblemPackages(qualifiedNameClass, module, manager, psiValue, isOnTheFly)

            val psiClass = JavaPsiFacade.getInstance(module.project)
                .findClass(qualifiedNameClass, GlobalSearchScope.allScope(module.project))
            if (psiClass == null) {
                val afterLastDotIndex = qualifiedNameClass.lastIndexOf(PropertyUtil.DOT) + 1
                val shortName = PropertyUtil.getClassNameByQualifiedName(qualifiedNameClass)
                val range = TextRange(afterLastDotIndex, afterLastDotIndex + shortName.length)
                problems += manager.createProblemDescriptor(
                    psiValue,
                    range,
                    SpringCoreBundle.message(
                        "explyt.spring.inspection.properties.value.unresolved",
                        "class",
                        shortName
                    ),
                    ProblemHighlightType.ERROR,
                    isOnTheFly
                )
            }
        }
        return problems
    }

    private fun getProblemPackages(
        qualifiedNameClass: String,
        module: Module,
        manager: InspectionManager,
        psiValue: PsiElement,
        isOnTheFly: Boolean,
    ): List<ProblemDescriptor> {
        val infoPackages = PropertyUtil.getPackages(module, qualifiedNameClass)
        return infoPackages.asSequence()
            .filter { it.psiPackage == null }
            .map {
                manager.createProblemDescriptor(
                    psiValue,
                    it.range,
                    SpringCoreBundle.message(
                        "explyt.spring.inspection.properties.value.unresolved",
                        "class or package",
                        it.name
                    ),
                    ProblemHighlightType.ERROR,
                    isOnTheFly
                )
            }.toList()
    }

    private fun getProblemHandleAs(
        module: Module,
        manager: InspectionManager,
        isOnTheFly: Boolean,
        hints: List<PropertyHint>,
        fileProperties: List<DefinedConfigurationProperty>,
    ): MutableList<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val handleAsProperties = fileProperties.filter { property ->
            hints.any { hint ->
                property.key == hint.name
                        && hint.providers.filter { it.name != null }.any { it.name == SpringProperties.HANDLE_AS }
            }
        }
        if (handleAsProperties.isEmpty()) {
            return problems
        }

        val propertiesSearch = SpringConfigurationPropertiesSearch.getInstance(module.project)
        for (handleAsProperty in handleAsProperties) {
            val elementFileProperty = handleAsProperty.psiElement ?: continue
            val psiValue = elementFileProperty.propertyValuePsiElement() ?: continue
            val key = elementFileProperty.propertyKey() ?: continue
            val value = elementFileProperty.propertyValue() ?: continue

            val providerHints = hints.asSequence()
                .filter { it.name == key }
                .distinctBy { it.name }
                .flatMap { it.providers }
                .toList()

            val configurationProperty = propertiesSearch.findProperty(module, key)
            val propertyType = configurationProperty?.type?.replace('$', '.')
            if (propertyType != null && propertyType == "java.lang.String") {
                providerHints.asSequence()
                    .mapNotNull { it.parameters?.target }
                    .filter { isProblemPropertyType(it, value) }
                    .forEach { _ ->
                        problems += manager.createProblemDescriptor(
                            psiValue,
                            SpringCoreBundle.message(
                                "explyt.spring.inspection.properties.value.unknown.encoding",
                                value
                            ),
                            isOnTheFly,
                            emptyArray(),
                            ProblemHighlightType.ERROR
                        )
                    }
            }

            for (provider in providerHints) {
                val targetClassFqn = provider.parameters?.target ?: continue
                problems += getProblemEnum(module, manager, psiValue, isOnTheFly, targetClassFqn, value)
            }
        }
        return problems
    }

    private fun getProblemBeanReferenceProperties(
        module: Module,
        manager: InspectionManager,
        isOnTheFly: Boolean,
        hints: List<PropertyHint>,
        fileProperties: List<DefinedConfigurationProperty>,
    ): MutableList<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val springBeanReferenceProperties = fileProperties.filter { property ->
            hints.any { hint ->
                property.key == hint.name
                        && hint.providers.filter { it.name != null }
                    .any { it.name == SpringProperties.SPRING_BEAN_REFERENCE }
            }
        }
        if (springBeanReferenceProperties.isEmpty()) {
            return problems
        }

        val springSearchService = SpringSearchServiceFacade.getInstance(module.project)
        val foundActiveBeans = springSearchService.getAllActiveBeans(module)

        for (property in springBeanReferenceProperties) {
            val elementFileProperty = property.psiElement ?: continue
            val psiValue = elementFileProperty.propertyValuePsiElement() ?: continue
            val value = elementFileProperty.propertyValue() ?: continue
            if (!foundActiveBeans.any { it.name == property.value }) {
                problems += manager.createProblemDescriptor(
                    psiValue,
                    SpringCoreBundle.message("explyt.spring.inspection.properties.value.unresolved", "bean", value),
                    isOnTheFly,
                    emptyArray(),
                    ProblemHighlightType.ERROR
                )
            }
        }
        return problems
    }

    private fun getProblemPropertyType(
        module: Module,
        manager: InspectionManager,
        isOnTheFly: Boolean,
        fileProperties: List<DefinedConfigurationProperty>,
    ): MutableList<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        for (property in fileProperties) {
            val elementFileProperty = property.psiElement ?: continue
            val psiValue = elementFileProperty.propertyValuePsiElement() ?: continue
            val configurationProperty = getConfigurationProperty(module, property) ?: continue
            val propertyType = getPropertyType(configurationProperty) ?: continue
            val values = getPropertyValue(property, configurationProperty)
            for (value in values) {
                val resultConvert = tryConvert(propertyType, value)
                val resultEncoding = tryEncoding(propertyType, value)
                if (resultConvert || resultEncoding) {
                    problems += manager.createProblemDescriptor(
                        psiValue,
                        SpringCoreBundle.message(
                            if (resultConvert) {
                                "explyt.spring.inspection.properties.value.spring.convert"
                            } else {
                                "explyt.spring.inspection.properties.value.unknown.encoding"
                            },
                            value,
                            propertyType
                        ),
                        isOnTheFly,
                        emptyArray(),
                        ProblemHighlightType.ERROR
                    )
                }
                problems += getProblemEnum(module, manager, psiValue, isOnTheFly, propertyType, value)
            }
        }
        return problems
    }

    private fun getProblemResource(
        manager: InspectionManager,
        isOnTheFly: Boolean,
        hints: List<PropertyHint>,
        fileProperties: List<DefinedConfigurationProperty>,
    ): MutableList<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val resources = fileProperties.filter { property ->
            hints.any { hint ->
                property.key == hint.name
                        && hint.providers.filter { it.name != null }.any { it.name == SpringProperties.HANDLE_AS }
                        && hint.providers.any { it.parameters?.target == IO_RESOURCE }
            }
        }
        if (resources.isEmpty()) {
            return problems
        }
        for (property in resources) {
            val value = property.value ?: continue
            val elementFileProperty = property.psiElement ?: continue
            val psiValue = elementFileProperty.propertyValuePsiElement() ?: continue
            problems += ResourceFileInspectionUtil.getPathProblemsClasspath(
                PropertiesFileType.INSTANCE,
                value,
                psiValue,
                manager,
                isOnTheFly
            )
        }
        return problems
    }

    private fun tryConvert(propertyType: String, value: String): Boolean {
        return when (propertyType) {
            JavaCoreClasses.BOOLEAN, PrimitiveTypes.BOOLEAN -> value.toBooleanStrictOrNull() == null
            JavaCoreClasses.BYTE, PrimitiveTypes.BYTE -> value.toByteOrNull() == null
            JavaCoreClasses.INTEGER, PrimitiveTypes.INT -> value.toIntOrNull() == null
            JavaCoreClasses.LONG, PrimitiveTypes.LONG -> value.toLongOrNull() == null
            JavaCoreClasses.SHORT, PrimitiveTypes.SHORT -> value.toShortOrNull() == null
            JavaCoreClasses.DOUBLE, PrimitiveTypes.DOUBLE,
            JavaCoreClasses.NUMBER,
                -> value.toDoubleOrNull() == null

            JavaCoreClasses.FLOAT, PrimitiveTypes.FLOAT -> value.toFloatOrNull() == null
            else -> false
        }
    }

    private fun tryEncoding(propertyType: String, value: String): Boolean {
        val resultEncoding = when (propertyType) {
            JavaCoreClasses.LOCALE -> !getLocales().any { it == value }
            JavaCoreClasses.CHARSET -> !Charset.availableCharsets().any { it.key == value }
            SpringCoreClasses.MIME_TYPE -> !MimeTypeDictionary.HTML_CONTENT_TYPES.any { it == value }
            else -> false
        }
        return resultEncoding
    }

    private fun getPropertyType(configurationProperty: ConfigurationProperty): String? {
        val propertyType = configurationProperty.type ?: return null
        return when {
            configurationProperty.isList() ->
                propertyType.substringAfter("<").substringBefore(">")

            configurationProperty.isMap() ->
                propertyType.substringAfter(",").substringBefore(">")

            configurationProperty.isArray() ->
                propertyType.substringBefore("[]")

            else -> propertyType.replace('$', '.')
        }
    }

    private fun isProblemPropertyType(propertyType: String, value: String): Boolean {
        return when (propertyType) {
            JavaCoreClasses.LOCALE -> !getLocales().any { it == value }
            JavaCoreClasses.CHARSET -> !Charset.availableCharsets().any { it.key == value }
            SpringCoreClasses.MIME_TYPE -> !MimeTypeDictionary.HTML_CONTENT_TYPES.any { it == value }
            JavaCoreClasses.BOOLEAN, PrimitiveTypes.BOOLEAN -> value.toBooleanStrictOrNull() == null
            else -> false
        }
    }

    private fun getPropertyValue(
        property: DefinedConfigurationProperty,
        configurationProperty: ConfigurationProperty
    ): List<String> {
        val value = property.value?.dropLastWhitespaces()
        if (value.isNullOrBlank()) {
            return emptyList()
        }
        return if (
            (configurationProperty.isArray() || configurationProperty.isList())
            && !property.key.endsWith("]")
        ) {
            value.split(",").map { it.dropWhitespaces() }
        } else if (value.contains(PLACEHOLDER_PREFIX) && value.contains(PLACEHOLDER_SUFFIX)) {
            emptyList()
        } else {
            listOf(value)
        }
    }

    private fun getConfigurationProperty(
        module: Module,
        property: DefinedConfigurationProperty
    ): ConfigurationProperty? {
        val key = property.key.dropLastWhitespaces()
        val propertiesSearch = SpringConfigurationPropertiesSearch.getInstance(module.project)

        val findProperty = propertiesSearch.findProperty(module, key)
        if (findProperty != null) {
            return findProperty
        }
        val properties = SpringConfigurationPropertiesSearch.getInstance(module.project).getAllProperties(module)
        val listKey = getListKeys(property, properties)
        if (listKey.isNotEmpty()) {
            return listKey.firstOrNull()
        }
        val mapKey = getMapKeys(property, properties)
        if (mapKey.isNotEmpty()) {
            return mapKey.firstOrNull()
        }
        return null
    }

    private fun getProblemEnum(
        module: Module,
        manager: InspectionManager,
        psiElement: PsiElement,
        isOnTheFly: Boolean,
        propertyType: String,
        value: String
    ): List<ProblemDescriptor> {
        val propertyTypeClass = getCachedPropertyTypeClass(module, propertyType) ?: return emptyList()
        if (propertyTypeClass.isEnum
            && !propertyTypeClass.fields.map { it.name.lowercase() }.any { it == value.lowercase() }
        ) {
            return listOf(
                manager.createProblemDescriptor(
                    psiElement,
                    SpringCoreBundle.message(
                        "explyt.spring.inspection.properties.value.unresolved.enum",
                        value,
                        propertyType
                    ),
                    isOnTheFly,
                    emptyArray(),
                    ProblemHighlightType.ERROR
                )
            )
        }
        return emptyList()
    }

    private fun getCachedPropertyTypeClass(module: Module, propertyType: String): PsiClass? {
        val project = module.project
        val key = CacheKeyStore.getInstance(module.project).getKey<PsiClass?>(propertyType)
        return CachedValuesManager
            .getManager(project)
            .getCachedValue(module, key, {
                CachedValueProvider.Result.create(
                    JavaPsiFacade.getInstance(project).findClass(propertyType, GlobalSearchScope.allScope(project)),
                    UastModificationTracker.getInstance(module.project)
                )
            }, false)
    }

    private fun getLocales() = DateFormat.getAvailableLocales()
        .map {
            val country = it.country
            it.language + if (country.isNullOrBlank()) "" else "_$country"
        }.distinct()


    private fun getMapKeys(
        fileProperty: DefinedConfigurationProperty,
        properties: List<ConfigurationProperty>
    ): List<ConfigurationProperty> {
        return properties.asSequence().filter { it.isMap() }.filter { fileProperty.key.startsWith(it.name) }.toList()
    }

    private fun getListKeys(
        fileProperty: DefinedConfigurationProperty,
        properties: List<ConfigurationProperty>
    ): List<ConfigurationProperty> {
        return properties.asSequence().filter { it.isList() || it.isArray() }
            .filter { fileProperty.key.startsWith(it.name) }.toList()
    }

    private fun checkDuplicateKeys(
        manager: InspectionManager,
        file: PsiFile,
        isOnTheFly: Boolean,
        fileProperties: List<DefinedConfigurationProperty>,
    ): List<ProblemDescriptor> {
        val duplicateKeyMap = fileProperties
            .groupBy { PropertyUtil.toCommonPropertyForm(it.key) }
            .filter { it.value.size > 1 }
            .takeIf { it.isNotEmpty() } ?: return emptyList()

        val problemsHolder = ProblemsHolder(manager, file, isOnTheFly)
        duplicateKeyMap.forEach { processDuplicate(it.value, problemsHolder) }
        return problemsHolder.results
    }

    private fun processDuplicate(properties: List<DefinedConfigurationProperty>, problemsHolder: ProblemsHolder) {
        val duplicateProperties = properties
            .associateBy { it.key }
            .values
            .takeIf { it.size > 1 } ?: return

        val duplicateKeys = duplicateProperties.joinToString(", ") { it.key }
        for (property in duplicateProperties) {
            val psiElementKey = getKeyPsiElement(property) ?: continue
            val fixes = getRemoveKeyQuickFixes(property)
            val message = PropertiesBundle.message("duplicate.property.key.error.message")
            problemsHolder.registerProblem(
                psiElementKey, "$message: $duplicateKeys", GENERIC_ERROR, *fixes.toTypedArray()
            )
        }
    }

    abstract fun getKeyPsiElement(property: DefinedConfigurationProperty): PsiElement?

    abstract fun getRemoveKeyQuickFixes(property: DefinedConfigurationProperty): List<LocalQuickFix>

    companion object {
        val PROHIBITED_IN_PROFILE_PROPERTIES =
            setOf("spring.profiles.include", "spring.profiles.active", "spring.profiles.default")
        val PROFILE_PROPERTIES_FILE_MASK = Regex("application-.*\\.(properties|yml|yaml)")
    }

}