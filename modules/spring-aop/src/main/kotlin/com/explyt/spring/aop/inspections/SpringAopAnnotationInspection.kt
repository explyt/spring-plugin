package com.explyt.spring.aop.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.aop.SpringAopBundle
import com.explyt.spring.aop.SpringAopClasses
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.SpringSearchService
import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInsight.intention.impl.invokeAsAction
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.codeinsight.utils.findExistingEditor
import org.jetbrains.kotlin.idea.quickfix.AddAnnotationFix.Kind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.uast.*


class SpringAopAnnotationInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        uClass: UClass, manager: InspectionManager, isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val sourcePsiAspectAnnotation = uClass.uAnnotations
            .find { it.qualifiedName == SpringAopClasses.ASPECT }?.sourcePsi
            ?: return emptyArray()

        val psiElement = uClass.sourcePsi ?: return emptyArray()
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return emptyArray()

        val holderSpringBoot = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringCoreClasses.COMPONENT)

        val springBeanAnnotations = uClass.uAnnotations.filter { holderSpringBoot.contains(it) }
        if (springBeanAnnotations.isNotEmpty()) return emptyArray()
        val problemDescriptor = manager.createProblemDescriptor(
            sourcePsiAspectAnnotation, SpringAopBundle.message("explyt.spring.inspection.aop.component"),
            isOnTheFly, addClassAnnotationFix(uClass, SpringCoreClasses.COMPONENT), ProblemHighlightType.WARNING
        )
        return arrayOf(problemDescriptor)
    }

    override fun checkMethod(
        method: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val uClass = method.getContainingUClass() ?: return emptyArray()
        val sourcePsiAspectAnnotation = method.uAnnotations
            .find { it.qualifiedName?.startsWith(SpringAopClasses.BASE_PACKAGE) == true }?.sourcePsi
            ?: return emptyArray()

        val springBeanAnnotations = uClass.uAnnotations.filter { it.qualifiedName == SpringAopClasses.ASPECT }
        if (springBeanAnnotations.isNotEmpty()) return emptyArray()
        val problemDescriptor = manager.createProblemDescriptor(
            sourcePsiAspectAnnotation, SpringAopBundle.message("explyt.spring.inspection.aop.aspect"),
            isOnTheFly, addClassAnnotationFix(uClass, SpringAopClasses.ASPECT), ProblemHighlightType.WARNING
        )
        return arrayOf(problemDescriptor)
    }

    private fun addClassAnnotationFix(uClass: UClass, annotationFqName: String): Array<LocalQuickFix> {
        if (uClass.lang === KotlinLanguage.INSTANCE) {
            return arrayOf(AddClassAnnotationKotlinFix(annotationFqName))
        } else {
            return arrayOf(AddAnnotationFix(annotationFqName, uClass.javaPsi))
        }
    }
}

class AddClassAnnotationKotlinFix(private val annotationFqName: String) : LocalQuickFix {
    override fun getName() = SpringAopBundle.message(
        "explyt.spring.inspection.aop.fix.annotate",
        "@" + ClassId.topLevel(FqName(annotationFqName)).shortClassName
    )

    override fun getFamilyName() = name
    override fun startInWriteAction() = false

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {

        val uAnnotation = descriptor.psiElement.toUElement() as? UAnnotation ?: return
        val ktElement = uAnnotation.getContainingUClass()?.sourcePsi as? KtElement ?: return
        val editor = ktElement.findExistingEditor() ?: return
        val ktFile = ktElement.containingFile as? KtFile ?: return

        ApplicationManager.getApplication().runWriteAction {
            org.jetbrains.kotlin.idea.quickfix.AddAnnotationFix(
                ktElement, ClassId.topLevel(FqName(annotationFqName)), Kind.Self
            ).invokeAsAction(editor, ktFile)
        }
    }
}