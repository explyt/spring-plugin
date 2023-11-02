package com.esprito.spring.core.properties.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringProperties.CLASS_REFERENCE
import com.esprito.spring.core.SpringProperties.SPRING_BEAN_REFERENCE
import com.esprito.spring.core.completion.properties.ConfigurationProperty
import com.esprito.spring.core.completion.properties.DeprecationInfoLevel
import com.esprito.spring.core.completion.properties.PropertyHint
import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.util.PropertyUtil
import com.esprito.spring.core.util.PropertyUtil.DOT
import com.esprito.spring.core.util.SpringCoreUtil
import com.intellij.codeInspection.*
import com.intellij.lang.properties.IProperty
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.impl.PropertyValueImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.childrenOfType

class SpringPropertiesInspection : LocalInspectionTool() {

    private var fileProperties = listOf<IProperty>()
    private var configurationProperties = listOf<ConfigurationProperty>()
    private var hints = listOf<PropertyHint>()

    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean
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
        problems += checkKey(module, manager, isOnTheFly)
        problems += checkValue(module, manager, isOnTheFly)
        return problems.toTypedArray()
    }

    private fun checkKey(
        module: Module,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): MutableList<ProblemDescriptor> {
        configurationProperties = SpringConfigurationPropertiesSearch.getInstance(module.project).getAllProperties(module)
        if (configurationProperties.isEmpty()) {
            return mutableListOf()
        }
        val problems = mutableListOf<ProblemDescriptor>()
        problems += getProblemPropertyDeprecated(manager, isOnTheFly)
        return problems
    }

    private fun checkValue(
        module: Module,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): MutableList<ProblemDescriptor> {
        hints = SpringConfigurationPropertiesSearch.getInstance(module.project).getAllHints(module)

        val problems = mutableListOf<ProblemDescriptor>()
        problems += getProblemClassReference(module, manager, isOnTheFly)
        problems += getProblemBeanReferenceProperties(module, manager, isOnTheFly)
        problems += getProblemPropertyType(module, manager, isOnTheFly)
        return problems
    }

    private fun getProblemPropertyDeprecated(
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): MutableList<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val deprecationProperties = configurationProperties.filter { property ->
            fileProperties.any { fileProperty -> fileProperty.key == property.name && property.deprecation != null }
        }
        if (deprecationProperties.isEmpty()) {
            return problems
        }
        for (property in deprecationProperties) {
            val psiElement = fileProperties.firstOrNull { it.key==property.name }?.psiElement
            val level = property.deprecation?.level ?: continue
            val reason = property.deprecation?.reason ?: " "
            val replacement = property.deprecation?.replacement ?: " "
            if (psiElement != null) {
                problems += manager.createProblemDescriptor(
                    psiElement,
                    SpringCoreBundle.message("esprito.spring.inspection.properties.key.deprecated", reason),
                    ReplacementKeyQuickFix(replacement, psiElement),
                    if (level == DeprecationInfoLevel.ERROR) ProblemHighlightType.GENERIC_ERROR else ProblemHighlightType.LIKE_DEPRECATED,
                    isOnTheFly
                )
            }
        }

        return problems
    }

    private fun getProblemClassReference(
        module: Module,
        manager: InspectionManager,
        isOnTheFly: Boolean
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
                    psiValue, range,
                    SpringCoreBundle.message(
                        "esprito.spring.inspection.properties.spring.reference",
                        "class",
                        shortName
                    ),
                    ProblemHighlightType.ERROR, isOnTheFly
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
        isOnTheFly: Boolean
    ): List<ProblemDescriptor> {
        val infoPackages = PropertyUtil.getPackages(module, qualifiedNameClass)
        return infoPackages.asSequence()
            .filter { it.psiPackage == null }
            .map {
                manager.createProblemDescriptor(
                    psiValue, it.range,
                    SpringCoreBundle.message(
                        "esprito.spring.inspection.properties.spring.reference",
                        "class or package",
                        it.name
                    ),
                    ProblemHighlightType.ERROR, isOnTheFly
                )
            }.toList()
    }

    private fun getProblemBeanReferenceProperties(
        module: Module,
        manager: InspectionManager,
        isOnTheFly: Boolean
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
            val value = property.value
            if (!foundActiveBeans.any { it.name == property.value } && psiValue != null && value != null) {
                problems += manager.createProblemDescriptor(
                    psiValue,
                    SpringCoreBundle.message("esprito.spring.inspection.properties.spring.reference", "bean", value),
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
        isOnTheFly: Boolean
    ): MutableList<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val propertiesSearch = SpringConfigurationPropertiesSearch.getInstance(module.project)
        for (property in fileProperties) {
            val psiValue = property.psiElement.childrenOfType<PropertyValueImpl>().firstOrNull() ?: continue
            val key = property.key
            val value = property.value
            if (key.isNullOrBlank()) continue
            if (value.isNullOrBlank()) continue
            val configurationProperty = propertiesSearch.findProperty(module, key) ?: continue
            val propertyType = configurationProperty.type?.replace('$', '.') ?: continue
            val result = when (propertyType) {
                "java.lang.Boolean", "boolean" -> !(value == "true" || value == "false")
                "java.lang.Byte", "byte"  -> value.toByteOrNull() == null
                "java.lang.Integer", "int"  -> value.toIntOrNull() == null
                "java.lang.Long", "long"  -> value.toLongOrNull() == null
                "java.lang.Short", "short"  -> value.toShortOrNull() == null
                "java.lang.Double", "double", "java.lang.Number"  -> value.toDoubleOrNull() == null
                "java.lang.Float", "float"  -> value.toFloatOrNull() == null
                else -> false
            }
            if (result) {
                problems += manager.createProblemDescriptor(
                    psiValue,
                    SpringCoreBundle.message(
                        "esprito.spring.inspection.properties.spring.convert",
                        value,
                        propertyType
                    ),
                    isOnTheFly,
                    emptyArray(),
                    ProblemHighlightType.ERROR
                )
            }
        }
        return problems
    }
}