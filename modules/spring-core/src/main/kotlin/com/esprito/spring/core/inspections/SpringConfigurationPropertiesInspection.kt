package com.esprito.spring.core.inspections

import com.esprito.inspection.SpringBaseUastLocalInspectionTool
import com.esprito.spring.core.SpringCoreBundle.message
import com.esprito.spring.core.SpringCoreClasses.CONFIGURATION_PROPERTIES
import com.esprito.spring.core.completion.properties.utils.ProjectConfigurationPropertiesUtil.extractConfigurationPropertyPrefix
import com.esprito.spring.core.completion.properties.utils.ProjectConfigurationPropertiesUtil.getAnnotatedElements
import com.esprito.spring.core.service.SpringSearchUtils
import com.intellij.codeInsight.navigation.getPsiElementPopup
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.ide.DataManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import java.util.regex.Pattern


class SpringConfigurationPropertiesInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        uClass: UClass, manager: InspectionManager, isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val file = uClass.javaPsi.containingFile ?: return emptyArray()
        return problemDescriptors(file, uClass.uAnnotations, manager, isOnTheFly)
    }

    override fun checkMethod(
        method: UMethod, manager: InspectionManager, isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val file = method.javaPsi.containingFile ?: return emptyArray()
        return problemDescriptors(file, method.uAnnotations, manager, isOnTheFly)

    }

    private fun problemDescriptors(
        file: PsiFile,
        uAnnotations: List<UAnnotation>,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val module = ModuleUtilCore.findModuleForFile(file) ?: return emptyArray()
        val uAnnotation = SpringSearchUtils.findUAnnotation(module, uAnnotations, CONFIGURATION_PROPERTIES)
            ?: return emptyArray()
        return collectProblems(uAnnotation, module, manager, isOnTheFly)
    }

    private fun collectProblems(
        uAnnotation: UAnnotation, module: Module, manager: InspectionManager, isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val javaPsiAnnotation = uAnnotation.javaPsi ?: return emptyArray()
        val sourcePsi = uAnnotation.sourcePsi ?: return emptyArray()

        val valueText = extractConfigurationPropertyPrefix(module, javaPsiAnnotation)
        val holder = ProblemsHolder(manager, sourcePsi.containingFile, isOnTheFly)
        if (valueText.isNullOrEmpty()) {
            holder.registerProblem(sourcePsi, message("esprito.spring.inspection.config.prefix.empty"))
        } else if (isNotKebabCase(valueText)) {
            holder.registerProblem(sourcePsi, message("esprito.spring.inspection.config.prefix.kebab"))
        }

        val duplicateElements = findDuplicate(module, valueText)
        if (duplicateElements.size > 1) {
            registerDuplicateProblemWithQuickFix(holder, sourcePsi, module, valueText)
        }
        return holder.resultsArray
    }

    private fun registerDuplicateProblemWithQuickFix(
        holder: ProblemsHolder,
        sourcePsi: PsiElement,
        module: Module,
        valueText: String?
    ) {
        holder.registerProblem(
            sourcePsi, message("esprito.spring.inspection.config.prefix.duplicate"),
            object : LocalQuickFix {

                override fun startInWriteAction(): Boolean = false

                override fun getFamilyName(): String = message("esprito.spring.inspection.config.prefix.duplicate.show")

                override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                    val result = findDuplicate(module, valueText).toTypedArray()
                    DataManager.getInstance().dataContextFromFocusAsync.onSuccess { context ->
                        when {
                            result.size > 1 -> getPsiElementPopup(result, null)
                            else -> JBPopupFactory.getInstance()
                                .createMessage(message("esprito.spring.inspection.config.prefix.duplicate.no"))
                        }.showInBestPositionFor(context)
                    }
                }
            })
    }

    private fun findDuplicate(module: Module, valueText: String?): List<PsiElement> {
        val annotatedElements = getAnnotatedElements(module)
        val duplicateElement = ArrayList<PsiElement>()
        for (psiElement in annotatedElements) {
            val prefix = extractConfigurationPropertyPrefix(module, psiElement) ?: continue
            if (prefix == valueText) {
                duplicateElement.add(psiElement)
            }
        }
        return duplicateElement
    }

    private fun isNotKebabCase(value: String): Boolean {
        return !KEBAB_PATTERN.matcher(value).matches()
    }

    companion object {
        private val KEBAB_PATTERN = Pattern.compile("[a-z]([a-z]|-[a-z]|\\d|\\.[a-z])*")
    }
}