package com.esprito.spring.core.inspections

import ai.grazie.nlp.utils.dropLastWhitespaces
import ai.grazie.nlp.utils.dropWhitespaces
import com.esprito.spring.core.JavaCoreClasses
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringProperties.ANY
import com.esprito.spring.core.SpringProperties.CLASS_REFERENCE
import com.esprito.spring.core.SpringProperties.HANDLE_AS
import com.esprito.spring.core.SpringProperties.PLACEHOLDER_PREFIX
import com.esprito.spring.core.SpringProperties.PLACEHOLDER_SUFFIX
import com.esprito.spring.core.SpringProperties.SPRING_BEAN_REFERENCE
import com.esprito.spring.core.completion.properties.ConfigurationProperty
import com.esprito.spring.core.completion.properties.DeprecationInfoLevel
import com.esprito.spring.core.completion.properties.PropertyHint
import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.esprito.spring.core.inspections.quickfix.ReplacementKeyQuickFix
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.esprito.spring.core.util.PropertyUtil
import com.esprito.spring.core.util.PropertyUtil.DOT
import com.esprito.spring.core.util.SpringCoreUtil
import com.esprito.util.CacheKeyStore
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.properties.IProperty
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.lang.properties.psi.impl.PropertyValueImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.childrenOfType
import com.intellij.xml.util.documentation.MimeTypeDictionary
import java.nio.charset.Charset
import java.text.DateFormat

class SpringPropertiesInspection : LocalInspectionTool() {

    private var fileProperties = listOf<IProperty>()
    private var configurationProperties = listOf<ConfigurationProperty>()
    private var hints = listOf<PropertyHint>()

    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): Array<ProblemDescriptor>? {
        if (!SpringCoreUtil.isConfigurationPropertyFile(file)) {
            return ProblemDescriptor.EMPTY_ARRAY
        }
        fileProperties = (file as PropertiesFile).properties
        if (fileProperties.isEmpty()) {
            return ProblemDescriptor.EMPTY_ARRAY
        }

        val module = ModuleUtilCore.findModuleForFile(file) ?: return ProblemDescriptor.EMPTY_ARRAY

        val problems = mutableListOf<ProblemDescriptor>()
        problems += checkKey(module, file, manager, isOnTheFly)
        problems += checkValue(module, manager, isOnTheFly)
        return problems.toTypedArray()
    }

    private fun checkKey(
        module: Module,
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): MutableList<ProblemDescriptor> {
        configurationProperties = SpringConfigurationPropertiesSearch.getInstance(module.project).getAllProperties(module)
        if (configurationProperties.isEmpty()) {
            return mutableListOf()
        }
        val problems = mutableListOf<ProblemDescriptor>()
        problems += getProblemKey(manager, file, isOnTheFly)
        problems += getProblemPropertyDeprecated(manager, isOnTheFly)
        return problems
    }

    private fun getProblemKey(
        manager: InspectionManager,
        file: PsiFile,
        isOnTheFly: Boolean,
    ): List<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val module = ModuleUtilCore.findModuleForPsiElement(file) ?: return emptyList()

        val properties = SpringConfigurationPropertiesSearch.getInstance(module.project)
            .getAllProperties(module)

        for (fileProperty in fileProperties) {
            val psiKey = fileProperty.psiElement.childrenOfType<PropertyKeyImpl>().firstOrNull() ?: continue

            val findProperties = properties.filter { it.name == fileProperty.key }
            if (findProperties.isEmpty() && !isPropertyMapKey(fileProperty, properties) && !isPropertyListKey(
                    fileProperty,
                    properties
                )
            ) {
                val psiReferences = SpringSearchService.getInstance(fileProperty.psiElement.project)
                    .getAllReferencesToElement(fileProperty.psiElement)
                if (psiReferences.isEmpty()) {
                    val key = fileProperty.key ?: continue
                    problems += manager.createProblemDescriptor(
                        psiKey,
                        SpringCoreBundle.message("esprito.spring.inspection.properties.key.unresolved", key),
                        isOnTheFly,
                        emptyArray(),
                        ProblemHighlightType.WARNING
                    )
                }
            } else {
                for (it in findProperties) {
                    if (it.isMap()) {
                        problems += manager.createProblemDescriptor(
                            psiKey,
                            SpringCoreBundle.message("esprito.spring.inspection.properties.key.map.not.found.key"),
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

    private fun checkValue(
        module: Module,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): MutableList<ProblemDescriptor> {
        hints = SpringConfigurationPropertiesSearch.getInstance(module.project).getAllHints(module)
        val problems = mutableListOf<ProblemDescriptor>()

        problems += getProblemValues(manager, isOnTheFly)
        problems += getProblemClassReference(module, manager, isOnTheFly)
        problems += getProblemHandleAs(module, manager, isOnTheFly)
        problems += getProblemBeanReferenceProperties(module, manager, isOnTheFly)
        problems += getProblemPropertyType(module, manager, isOnTheFly)

        return problems
    }

    private fun getProblemPropertyDeprecated(
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): MutableList<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val deprecationProperties = configurationProperties.filter { property ->
            fileProperties.any { fileProperty -> fileProperty.key == property.name && property.deprecation != null }
        }
        if (deprecationProperties.isEmpty()) {
            return problems
        }
        for (property in deprecationProperties) {
            val psiElement = fileProperties.firstOrNull { it.key == property.name }?.psiElement
            val level = property.deprecation?.level ?: continue
            val reason = property.deprecation?.reason ?: " "
            val replacement = property.deprecation?.replacement ?: " "
            if (psiElement != null) {
                problems += manager.createProblemDescriptor(
                    psiElement,
                    SpringCoreBundle.message("esprito.spring.inspection.properties.key.deprecated", reason),
                    isOnTheFly,
                    arrayOf(ReplacementKeyQuickFix(replacement, psiElement)),
                    if (level == DeprecationInfoLevel.ERROR) ProblemHighlightType.GENERIC_ERROR else ProblemHighlightType.LIKE_DEPRECATED
                )
            }
        }

        return problems
    }

    private fun getProblemValues(
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): MutableList<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val findInFileProperties = fileProperties.filter { property ->
            hints.any { hint ->
                (property.key == hint.name || property.key?.substringBeforeLast(".") + ".keys" == hint.name)
                        && hint.values.isNotEmpty()
                        && (hint.providers.isEmpty() || hint.providers.any { it.name != ANY })
            }
        }
        if (findInFileProperties.isEmpty()) {
            return problems
        }
        for (fileProperty in findInFileProperties) {
            val psiValue = fileProperty.psiElement.childrenOfType<PropertyValueImpl>().firstOrNull() ?: continue
            val key = fileProperty.key?.dropLastWhitespaces() ?: continue
            val value = fileProperty.value?.dropLastWhitespaces() ?: continue
            val hintValues = hints.asSequence()
                .filter { it.name == key || it.name == key.substringBeforeLast(".") + ".values" }
                .distinctBy { it.name }
                .flatMap { it.values }
                .map { it.value }
                .toList()

            if (value !in hintValues) {
                problems += manager.createProblemDescriptor(
                    psiValue,
                    SpringCoreBundle.message("esprito.spring.inspection.properties.value.unresolved.static", value, hintValues),
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
    ): MutableList<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val classReferenceProperties = fileProperties.filter { property ->
            hints.any { hint -> property.key == hint.name && hint.providers.any { it.name == CLASS_REFERENCE } }
        }
        if (classReferenceProperties.isEmpty()) {
            return problems
        }

        for (property in classReferenceProperties) {
            val psiValue = property.psiElement.childrenOfType<PropertyValueImpl>().firstOrNull() ?: continue
            val qualifiedNameClass = property.value ?: continue

            problems += getProblemPackages(qualifiedNameClass, module, manager, psiValue, isOnTheFly)

            val psiClass = JavaPsiFacade.getInstance(module.project)
                .findClass(qualifiedNameClass, GlobalSearchScope.allScope(module.project))
            if (psiClass == null) {
                val afterLastDotIndex = qualifiedNameClass.lastIndexOf(DOT) + 1
                val shortName = PropertyUtil.getClassNameByQualifiedName(qualifiedNameClass)
                val range = TextRange(afterLastDotIndex, afterLastDotIndex + shortName.length)
                problems += manager.createProblemDescriptor(
                    psiValue,
                    range,
                    SpringCoreBundle.message(
                        "esprito.spring.inspection.properties.value.unresolved",
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
        psiValue: PropertyValueImpl,
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
                        "esprito.spring.inspection.properties.value.unresolved",
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
    ): MutableList<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val handleAsProperties = fileProperties.filter { property ->
            hints.any { hint -> property.key == hint.name && hint.providers.any { it.name == HANDLE_AS } }
        }
        if (handleAsProperties.isEmpty()) {
            return problems
        }

        val propertiesSearch = SpringConfigurationPropertiesSearch.getInstance(module.project)
        for (handleAsProperty in handleAsProperties) {
            val psiValue = handleAsProperty.psiElement.childrenOfType<PropertyValueImpl>().firstOrNull() ?: continue
            val key = handleAsProperty.key?.dropLastWhitespaces() ?: continue
            val value = handleAsProperty.value?.dropLastWhitespaces() ?: continue
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
                            SpringCoreBundle.message("esprito.spring.inspection.properties.value.unknown.encoding", value),
                            isOnTheFly,
                            emptyArray(),
                            ProblemHighlightType.ERROR
                        )
                    }
            }

            for (provider in providerHints) {
                val targetClassFqn = provider.parameters?.target ?: continue
                if (isProblemEnum(module, targetClassFqn, value)) {
                    problems += manager.createProblemDescriptor(
                        psiValue,
                        SpringCoreBundle.message("esprito.spring.inspection.properties.value.unresolved.enum", value, targetClassFqn),
                        isOnTheFly,
                        emptyArray(),
                        ProblemHighlightType.ERROR
                    )
                }
            }
        }
        return problems
    }

    private fun getProblemBeanReferenceProperties(
        module: Module,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): MutableList<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val springBeanReferenceProperties = fileProperties.filter { property ->
            hints.any { hint -> property.key == hint.name && hint.providers.any { it.name == SPRING_BEAN_REFERENCE } }
        }
        if (springBeanReferenceProperties.isEmpty()) {
            return problems
        }

        val springSearchService = SpringSearchService.getInstance(module.project)
        val foundActiveBeans = springSearchService.getActiveBeansClasses(module)

        springBeanReferenceProperties.forEach { property ->
            val psiValue = property.psiElement.childrenOfType<PropertyValueImpl>().firstOrNull()
            val value = property.value?.dropLastWhitespaces()
            if (!foundActiveBeans.any { it.name == property.value } && psiValue != null && value != null) {
                problems += manager.createProblemDescriptor(
                    psiValue,
                    SpringCoreBundle.message("esprito.spring.inspection.properties.value.unresolved", "bean", value),
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
    ): MutableList<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        for (property in fileProperties) {
            val psiValue = property.psiElement.childrenOfType<PropertyValueImpl>().firstOrNull() ?: continue
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
                                "esprito.spring.inspection.properties.value.spring.convert"
                            } else {
                                "esprito.spring.inspection.properties.value.unknown.encoding"
                            },
                            value,
                            propertyType
                        ),
                        isOnTheFly,
                        emptyArray(),
                        ProblemHighlightType.ERROR
                    )
                }
            }
        }
        return problems
    }

    private fun tryConvert(propertyType: String, value: String): Boolean {
        return when (propertyType) {
            JavaCoreClasses.BOOLEAN, "boolean" -> value.toBooleanStrictOrNull() == null
            JavaCoreClasses.BYTE, "byte" -> value.toByteOrNull() == null
            JavaCoreClasses.INTEGER, "int" -> value.toIntOrNull() == null
            JavaCoreClasses.LONG, "long" -> value.toLongOrNull() == null
            JavaCoreClasses.SHORT, "short" -> value.toShortOrNull() == null
            JavaCoreClasses.DOUBLE, "double",
            JavaCoreClasses.NUMBER,
            -> value.toDoubleOrNull() == null

            JavaCoreClasses.FLOAT, "float" -> value.toFloatOrNull() == null
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
        if (configurationProperty.isList()) {
            return configurationProperty.type?.substringAfter("<")?.substringBefore(">") ?: ""
        } else if (configurationProperty.isMap()) {
            return configurationProperty.type?.substringAfter(",")?.substringBefore(">") ?: ""
        } else if (configurationProperty.isArray()) {
            return configurationProperty.type?.substringBefore("[]") ?: ""
        }
        return configurationProperty.type?.replace('$', '.')
    }


    private fun isProblemPropertyType(propertyType: String, value: String): Boolean {
        return when (propertyType) {
            JavaCoreClasses.LOCALE -> !getLocales().any { it == value }
            JavaCoreClasses.CHARSET -> !Charset.availableCharsets().any { it.key == value }
            SpringCoreClasses.MIME_TYPE -> !MimeTypeDictionary.HTML_CONTENT_TYPES.any { it == value }
            JavaCoreClasses.BOOLEAN, "boolean" -> value.toBooleanStrictOrNull() == null
            else -> false
        }
    }

    private fun getPropertyValue(property: IProperty, configurationProperty: ConfigurationProperty): List<String> {
        val value = property.value?.dropLastWhitespaces()
        if (value.isNullOrBlank()) {
            return emptyList()
        }
        return if (configurationProperty.isArray() || configurationProperty.isList()) {
            value.split(",").map { it.dropWhitespaces() }
        } else if (value.contains(PLACEHOLDER_PREFIX) && value.contains(PLACEHOLDER_SUFFIX)) {
            emptyList()
        } else {
            listOf(value)
        }
    }

    private fun getConfigurationProperty(module: Module, property: IProperty): ConfigurationProperty? {
        val key = property.key?.dropLastWhitespaces()
        val propertiesSearch = SpringConfigurationPropertiesSearch.getInstance(module.project)
        if (key == null) {
            return null
        }

        val findProperty = propertiesSearch.findProperty(module, key)
        if (findProperty != null) {
            return findProperty
        }
        val properties = SpringConfigurationPropertiesSearch.getInstance(module.project).getAllProperties(module)
        if (isPropertyListKey(property, properties)) {
            return getConfigurationListProperties(property, properties).firstOrNull()
        }
        if (isPropertyMapKey(property, properties)) {
            return getConfigurationMapProperties(property, properties).firstOrNull()
        }
        return null
    }

    private fun isProblemEnum(module: Module, propertyType: String, value: String): Boolean {
        val propertyTypeClass = getCachedPropertyTypeClass(module, propertyType)
        if (propertyTypeClass?.isEnum == true) {
            return !propertyTypeClass.fields.map { it.name.lowercase() }.any { it == value.lowercase() }
        }
        return false
    }

    private fun getCachedPropertyTypeClass(module: Module, propertyType: String): PsiClass? {
        val project = module.project
        val key = CacheKeyStore.getInstance(module.project).getKey<PsiClass?>(propertyType)
        return CachedValuesManager
            .getManager(project)
            .getCachedValue(module, key, {
                CachedValueProvider.Result.create(
                    JavaPsiFacade.getInstance(project).findClass(propertyType, GlobalSearchScope.allScope(project)),
                    ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
                )
            }, false)
    }

    private fun getLocales() = DateFormat.getAvailableLocales()
        .map {
            val country = it.country
            it.language + if (country.isNullOrBlank()) "" else "_$country"
        }.distinct()

    private fun isPropertyMapKey(fileProperty: IProperty, properties: List<ConfigurationProperty>): Boolean {
        val property = configurationTypeProperties(fileProperty, properties, ".")
        if (property.isEmpty()) {
            return false
        }
        return property.any { it.isMap() }
    }

    private fun isPropertyListKey(fileProperty: IProperty, properties: List<ConfigurationProperty>): Boolean {
        val property = configurationTypeProperties(fileProperty, properties, "[")
        if (property.isEmpty()) {
            return false
        }
        return property.any { it.isList() }
    }

    private fun getConfigurationListProperties(
        fileProperty: IProperty,
        properties: List<ConfigurationProperty>
    ): List<ConfigurationProperty> {
        return configurationTypeProperties(fileProperty, properties, "[")
    }

    private fun getConfigurationMapProperties(
        fileProperty: IProperty,
        properties: List<ConfigurationProperty>
    ): List<ConfigurationProperty> {
        return configurationTypeProperties(fileProperty, properties, ".")
    }

    private fun configurationTypeProperties(
        fileProperty: IProperty,
        properties: List<ConfigurationProperty>,
        separator: String
    ): List<ConfigurationProperty> {
        val key = fileProperty.key?.substringBeforeLast(separator) ?: false
        return properties.filter { it.name == key }
    }
}