package com.esprito.spring.core.inspections

import com.esprito.inspection.SpringBaseUastLocalInspectionTool
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringCoreClasses.ENTITY
import com.esprito.spring.core.SpringCoreClasses.ENTITY_X
import com.esprito.spring.core.service.PackageScanService
import com.esprito.spring.core.service.SpringEntityPackageScanService
import com.intellij.codeInsight.intention.AddAnnotationFix
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
                SpringCoreBundle.message("esprito.spring.inspection.entity.scan.problem", packageName),
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
                listOf(AddAnnotationFix(SpringCoreClasses.ENTITY_SCAN, psiClass, attributes))
            }

            else -> {
                emptyList()
            }
        }
    }
}