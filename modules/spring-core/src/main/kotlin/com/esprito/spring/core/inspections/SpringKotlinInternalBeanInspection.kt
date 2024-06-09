package com.esprito.spring.core.inspections

import com.esprito.inspection.SpringBaseUastLocalInspectionTool
import com.esprito.spring.core.SpringCoreBundle.message
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.inspections.quickfix.AddAnnotationParameterKotlinFix
import com.esprito.spring.core.service.SpringSearchService
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType.WARNING
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UMethod

class SpringKotlinInternalBeanInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        method: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val ktNamedFunction = method.sourcePsi as? KtNamedFunction ?: return emptyArray()
        ktNamedFunction.modifierList?.getModifier(KtTokens.INTERNAL_KEYWORD) ?: return emptyArray()
        method.findAnnotation(SpringCoreClasses.BEAN) ?: return emptyArray()

        val problems = ProblemsHolder(manager, method.javaPsi.containingFile, isOnTheFly)
        checkMethodAnnotations(method, problems)
        return problems.resultsArray
    }

    private fun checkMethodAnnotations(method: UMethod, problems: ProblemsHolder) {
        val jvmNameAnnotation = method.findAnnotation("kotlin.jvm.JvmName")
        val hasJvmName = jvmNameAnnotation?.attributeValues?.mapNotNull { it.evaluate() }?.any()
        if (hasJvmName == true) return

        if (hasBeanNameValue(method)) return
        val psiElement = method.uastAnchor?.sourcePsi ?: return
        val methodName = psiElement.text ?: return
        problems.registerProblem(
            psiElement, message("esprito.spring.inspection.kotlin.internal.warning"), WARNING,
            AddAnnotationParameterKotlinFix(psiElement, SpringCoreClasses.BEAN, methodName)
        )
    }

    private fun hasBeanNameValue(method: UMethod): Boolean {
        val beanAnnotation = method.findAnnotation(SpringCoreClasses.BEAN) ?: return false
        val annotationPsiElement = beanAnnotation.javaPsi ?: return false
        val module = ModuleUtilCore.findModuleForPsiElement(annotationPsiElement) ?: return false
        val metaHolder = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringCoreClasses.BEAN)

        return metaHolder.getAnnotationMemberValues(annotationPsiElement, setOf("name"))
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .any()
    }
}