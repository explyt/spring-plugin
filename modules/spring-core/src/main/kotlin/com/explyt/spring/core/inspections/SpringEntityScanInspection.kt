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

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringCoreClasses.ENTITY
import com.explyt.spring.core.SpringCoreClasses.ENTITY_X
import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.service.SpringEntityPackageScanService
import com.intellij.codeInsight.intention.AddAnnotationModCommandAction
import com.intellij.codeInspection.*
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.search.GlobalSearchScope.projectScope
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElement


class SpringEntityScanInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        if (uClass.uAnnotations.none { it.qualifiedName == ENTITY || it.qualifiedName == ENTITY_X }) return emptyArray()

        val holder = ProblemsHolder(manager, uClass.javaPsi.containingFile, isOnTheFly)
        checkBasePackage(holder, uClass)
        return holder.resultsArray
    }

    private fun checkBasePackage(holder: ProblemsHolder, uClass: UClass) {
        val module = ModuleUtilCore.findModuleForPsiElement(uClass.javaPsi) ?: return
        val packages = SpringEntityPackageScanService.getInstance(holder.project)
            .getPackages(module).takeIf { it.isNotEmpty() } ?: return
        val qualifiedName = uClass.qualifiedName ?: return
        val sourcePsi = uClass.uastAnchor?.sourcePsi ?: return
        val nonePackage = packages.none { qualifiedName.startsWith(it) }
        val packageName = (sourcePsi.containingFile as? PsiJavaFile)?.packageName ?: ""
        if (nonePackage) {
            val fixes = getFixes(packages, uClass, packageName)
            holder.registerProblem(
                sourcePsi,
                SpringCoreBundle.message("explyt.spring.inspection.entity.scan.problem", packageName),
                *fixes
            )
        }
    }

    private fun getFixes(
        packages: Set<String>, uClass: UClass, packageName: String
    ): Array<LocalQuickFix> {
        val moveToPackageFixes = packages.map { MoveToPackageFix(uClass.javaPsi.containingFile, it) }
        val fix = getAnnotationFix(uClass, packageName)
        return (moveToPackageFixes + fix).toTypedArray()
    }

    private fun getAnnotationFix(uClass: UClass, packageName: String): List<LocalQuickFix> {
        val project = uClass.javaPsi.project
        val rootComponentQualifiedSet = PackageScanService.getInstance(project)
            .getAllPackages().rootComponentQualified
        val rootComponent = rootComponentQualifiedSet.firstOrNull() ?: return emptyList()
        val psiClass = JavaPsiFacade.getInstance(project)
            .findClass(rootComponent, projectScope(project)) ?: return emptyList()
        val uElement = psiClass.toUElement() ?: return emptyList()
        return when (uElement.lang) {
            JavaLanguage.INSTANCE -> {
                val createAnnotationFromText = JavaPsiFacade.getInstance(project).elementFactory
                    .createAnnotationFromText("@${SpringCoreClasses.ENTITY_SCAN}(value=\"$packageName\")", null)
                val attributes = createAnnotationFromText.parameterList.attributes
                val fixAction = AddAnnotationModCommandAction(SpringCoreClasses.ENTITY_SCAN, psiClass, attributes)
                LocalQuickFix.from(fixAction)?.let { listOf(it) } ?: return emptyList()
            }

            else -> {
                emptyList()
            }
        }
    }
}