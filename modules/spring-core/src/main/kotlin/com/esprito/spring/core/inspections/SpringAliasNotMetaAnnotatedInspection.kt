package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.service.AliasUtils
import com.esprito.util.EspritoPsiUtil.getHighlightRange
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.parentOfType


class SpringAliasNotMetaAnnotatedInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun checkMethod(
        method: PsiMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val aliasAnnotation = method.getAnnotation(SpringCoreClasses.ALIAS_FOR) ?: return null
        val aliasedClassQn =
            AliasUtils.getAliasedClass(aliasAnnotation)
                ?.takeIf { it.isAnnotationType }
                ?.qualifiedName
                ?: return null

        val parentClass = method.parentOfType<PsiClass>() ?: return null
        val isMetaAnnotationFound = parentClass.qualifiedName == aliasedClassQn
                || parentClass.isMetaAnnotatedBy(aliasedClassQn)

        if (isMetaAnnotationFound) return null

        val annotationMemberValue = aliasAnnotation.findAttributeValue("annotation") ?: return null
        return arrayOf(
            manager.createProblemDescriptor(
                annotationMemberValue,
                annotationMemberValue.getHighlightRange(),
                SpringCoreBundle.message("esprito.spring.inspection.alias.annotation"),
                ProblemHighlightType.GENERIC_ERROR,
                isOnTheFly,
                AddAnnotationFix(aliasedClassQn, parentClass)
            )
        )
    }

}