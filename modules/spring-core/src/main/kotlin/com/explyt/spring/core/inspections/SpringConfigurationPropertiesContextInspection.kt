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

import com.explyt.base.LibraryClassCache
import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses.COMPONENT
import com.explyt.spring.core.SpringCoreClasses.CONFIGURATION_PROPERTIES
import com.explyt.spring.core.SpringCoreClasses.CONFIGURATION_PROPERTIES_SCAN
import com.explyt.spring.core.SpringCoreClasses.ENABLE_CONFIGURATION_PROPERTIES
import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.service.PackageScanService.Companion.getPackages
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.service.SpringSearchUtils
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType


class SpringConfigurationPropertiesContextInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        uClass: UClass, manager: InspectionManager, isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val file = uClass.javaPsi.containingFile ?: return emptyArray()

        val module = ModuleUtilCore.findModuleForFile(file) ?: return emptyArray()
        val uAnnotation = SpringSearchUtils.findUAnnotation(module, uClass.uAnnotations, CONFIGURATION_PROPERTIES)
            ?: return emptyArray()
        val problemsHolder = ProblemsHolder(manager, file, isOnTheFly)
        collectContextProblems(uAnnotation, uClass, module, problemsHolder)
        return problemsHolder.resultsArray
    }

    private fun collectContextProblems(
        uAnnotation: UAnnotation,
        configPropertyClass: UClass,
        module: Module,
        problemsHolder: ProblemsHolder
    ) {
        val sourcePsi = uAnnotation.sourcePsi ?: return
        if (SpringSearchServiceFacade.isExternalProjectExist(module.project)) return
        val metaAnnotationComponent = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, COMPONENT)
        if (configPropertyClass.uAnnotations.any { metaAnnotationComponent.contains(it) }) return

        val enabledConfigurationPropertiesClasses = findEnabledConfigurationPropertiesClasses(module)
        if (enabledConfigurationPropertiesClasses.contains(configPropertyClass.javaPsi)) return

        val propertiesScanPackages = findPropertiesScanPackages(module)
        val qualifiedName = configPropertyClass.qualifiedName ?: return
        if (propertiesScanPackages.any { qualifiedName.startsWith(it) }) return

        problemsHolder.registerProblem(sourcePsi, SpringCoreBundle.message("explyt.spring.inspection.config.context"))
    }

    private fun findEnabledConfigurationPropertiesClasses(module: Module): Set<PsiClass> {
        val configurationPropertiesClass = LibraryClassCache
            .searchForLibraryClass(module.project, ENABLE_CONFIGURATION_PROPERTIES) ?: return emptySet()
        val scope = module.moduleWithDependentsScope
        return AnnotatedElementsSearch.searchPsiClasses(configurationPropertiesClass, scope)
            .flatMapTo(mutableSetOf()) { mapToPsiClasses(it) }
    }

    private fun mapToPsiClasses(psiClass: PsiClass): List<PsiClass> {
        val uClass = psiClass.toUElementOfType<UClass>() ?: return emptyList()

        val enableConfigPropertiesAnnotations = uClass.uAnnotations
            .find { it.qualifiedName == ENABLE_CONFIGURATION_PROPERTIES } ?: return emptyList()
        return enableConfigPropertiesAnnotations.attributeValues
            .flatMap { PackageScanService.getPsiClasses(it.expression) }
    }

    private fun findPropertiesScanPackages(module: Module): Set<String> {
        val configurationPropertiesScanClass = LibraryClassCache
            .searchForLibraryClass(module.project, CONFIGURATION_PROPERTIES_SCAN) ?: return emptySet()
        val scope = module.moduleWithDependentsScope
        return AnnotatedElementsSearch.searchPsiClasses(configurationPropertiesScanClass, scope)
            .flatMapTo(mutableSetOf()) { mapToPackages(it, module) }
    }

    private fun mapToPackages(psiClass: PsiClass, module: Module): List<String> {
        val uClass = psiClass.toUElementOfType<UClass>() ?: return emptyList()
        val holderScan = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, CONFIGURATION_PROPERTIES_SCAN)
        return uClass.uAnnotations.asSequence()
            .filter { holderScan.contains(it) }
            .flatMap { getPackages(it, holderScan) }
            .toList()

    }
}