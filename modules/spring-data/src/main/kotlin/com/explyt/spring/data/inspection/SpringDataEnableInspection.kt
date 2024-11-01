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