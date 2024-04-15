package com.esprito.spring.core.inspections

import com.esprito.inspection.SpringBaseUastLocalInspectionTool
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses.RETENTION
import com.esprito.spring.core.SpringCoreClasses.RETENTION_POLICY
import com.esprito.spring.core.inspections.quickfix.RewriteAnnotationQuickFix
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.uast.UClass


class SpringMetaAnnotationWithoutRuntimeInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        aClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        //TODO: deep scan?
        if (!aClass.isAnnotationType) return null
        val psiClass = aClass.javaPsi
        val identity = getIdentifyingElement(aClass) ?: return null

        if (psiClass
                .annotations.none {
                    it.qualifiedName
                        ?.startsWith(SPRING_PREFIX) == true
                }
        ) return null

        val retention = psiClass.getAnnotation(RETENTION)
            ?: return createProblemDescriptor(identity, psiClass, manager, isOnTheFly)

        val retentionPolicy = AnnotationUtil.findDeclaredAttribute(retention, "value")?.value
            ?: return null

        return if (retentionPolicy.reference?.canonicalText?.contains(RETENTION_POLICY_RUNTIME) == true) {
            null
        } else {
            createProblemDescriptor(identity, psiClass, manager, isOnTheFly)
        }
    }

    private fun createProblemDescriptor(
        psiElement: PsiElement,
        psiClass: PsiClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        return arrayOf(
            manager.createProblemDescriptor(
                psiElement,
                SpringCoreBundle.message("esprito.spring.inspection.retention.incorrect"),
                RewriteAnnotationQuickFix(
                    "$RETENTION($RETENTION_POLICY.RUNTIME)",
                    psiClass,
                    RETENTION
                ),
                ProblemHighlightType.GENERIC_ERROR,
                isOnTheFly
            )
        )
    }

    private fun getIdentifyingElement(aClass: UClass): PsiElement? {
        return (aClass.sourcePsi as? PsiNameIdentifierOwner)
            ?.identifyingElement
            ?.navigationElement
    }

    companion object {
        const val SPRING_PREFIX = "org.springframework."
        const val RETENTION_POLICY_RUNTIME = "RetentionPolicy.RUNTIME"
    }

}