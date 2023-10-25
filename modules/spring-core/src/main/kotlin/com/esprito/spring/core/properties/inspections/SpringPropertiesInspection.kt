package com.esprito.spring.core.properties.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringProperties.CLASS_REFERENCE
import com.esprito.spring.core.SpringProperties.SPRING_BEAN_REFERENCE
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

    private var properties = listOf<IProperty>()
    private var hints = listOf<PropertyHint>()

    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        if (!SpringCoreUtil.isConfigurationPropertyFile(file)) {
            return ProblemDescriptor.EMPTY_ARRAY
        }
        properties = (file as PropertiesFile).properties
        if (properties.isEmpty()) {
            return ProblemDescriptor.EMPTY_ARRAY
        }

        val module = ModuleUtilCore.findModuleForFile(file) ?: return ProblemDescriptor.EMPTY_ARRAY
        hints = SpringConfigurationPropertiesSearch.getInstance(module.project).getAllHints(module)
        if (hints.isEmpty()) {
            return ProblemDescriptor.EMPTY_ARRAY
        }

        val problems = mutableListOf<ProblemDescriptor>()
        problems += getProblemClassReference(module, manager, isOnTheFly)
        problems += getProblemBeanReferenceProperties(module, manager, isOnTheFly)
        problems += getProblemPropertyType(module, manager, isOnTheFly)

        return problems.toTypedArray()
    }

    private fun getProblemClassReference(
        module: Module,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): MutableList<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val classReferenceProperties = properties.filter { property ->
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
        val springBeanReferenceProperties = properties.filter { property ->
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
        for(property in properties) {
            val psiValue = property.psiElement.childrenOfType<PropertyValueImpl>().firstOrNull() ?: continue
            val key = property.key
            val value = property.value
            if (key.isNullOrBlank()) continue
            if (value.isNullOrBlank()) continue
            val configurationProperty = propertiesSearch.findProperty(module, key) ?: continue
            val propertyType = configurationProperty.type?.replace('$', '.') ?: continue
            val result = when (propertyType) {
                "java.lang.Boolean" -> !(value.contains("true") || value.contains("false"))
                "java.lang.Integer" -> value.toIntOrNull() == null
                "java.lang.Double" -> value.toDoubleOrNull() == null
                else -> false
            }
            if (result) {
                problems += manager.createProblemDescriptor(
                    psiValue,
                    SpringCoreBundle.message("esprito.spring.inspection.properties.spring.convert", value, propertyType),
                    isOnTheFly,
                    emptyArray(),
                    ProblemHighlightType.ERROR
                )
            }
        }
        return problems
    }
}