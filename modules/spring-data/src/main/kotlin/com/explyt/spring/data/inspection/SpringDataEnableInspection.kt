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

package com.explyt.spring.data.inspection

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.inspections.quickfix.NavigateToFileQuickFix
import com.explyt.spring.data.SpringDataBundle.message
import com.explyt.spring.data.SpringDataProperties.JPA_REPOSITORY_ENABLE
import com.explyt.spring.data.service.SpringDataPackageScanService
import com.explyt.spring.data.util.SpringDataUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.MoveToPackageFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.uast.UClass


class SpringDataEnableInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        if (!SpringDataUtil.isRepository(uClass.javaPsi)) return emptyArray()

        val holder = ProblemsHolder(manager, uClass.javaPsi.containingFile, isOnTheFly)
        checkSpringDataEnableProperty(holder, uClass)
        checkBasePackage(holder, uClass)
        return holder.resultsArray
    }

    private fun checkSpringDataEnableProperty(holder: ProblemsHolder, uClass: UClass) {
        val sourcePsi = uClass.uastAnchor?.sourcePsi ?: return
        val module = ModuleUtilCore.findModuleForPsiElement(holder.file) ?: return
        val repositoriesEnabled = DefinedConfigurationPropertiesSearch.getInstance(holder.project)
            .findProperties(module, JPA_REPOSITORY_ENABLE).takeIf { it.isNotEmpty() } ?: return

        val enabledProperty = repositoriesEnabled.find { it.value.equals("false", true) }
        if (enabledProperty != null) {
            val settingNioPath = enabledProperty.psiElement?.containingFile?.virtualFile ?: return
            val fix = NavigateToFileQuickFix(settingNioPath, JPA_REPOSITORY_ENABLE)
            holder.registerProblem(sourcePsi, message("explyt.spring.data.inspection.enable.false"), fix)
        }
    }

    private fun checkBasePackage(holder: ProblemsHolder, uClass: UClass) {
        val module = ModuleUtilCore.findModuleForPsiElement(uClass.javaPsi) ?: return
        val packages = SpringDataPackageScanService.getInstance(holder.project)
            .getPackages(module).takeIf { it.isNotEmpty() } ?: return
        val qualifiedName = uClass.qualifiedName ?: return
        val sourcePsi = uClass.uastAnchor?.sourcePsi ?: return
        val nonePackage = packages.none { qualifiedName.startsWith(it) }
        if (nonePackage) {
            val fixes = packages.map { MoveToPackageFix(uClass.javaPsi.containingFile, it) }
            holder.registerProblem(
                sourcePsi, message("explyt.spring.data.inspection.enable.package"), *fixes.toTypedArray()
            )
        }
    }
}