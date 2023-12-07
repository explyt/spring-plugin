package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.service.AliasUtils
import com.esprito.util.EspritoPsiUtil.getHighlightRange
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.toSourcePsi
import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiClass
import com.intellij.psi.util.parentOfType
import org.jetbrains.uast.UMethod


class SpringAliasNotMetaAnnotatedInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val method = uMethod.javaPsi
        val uAliasAnnotation = uMethod.uAnnotations
            .firstOrNull { it.qualifiedName == SpringCoreClasses.ALIAS_FOR } ?: return null
        val aliasAnnotation = uAliasAnnotation.javaPsi ?: return null
        val aliasedClassQn =
            AliasUtils.getAliasedClass(aliasAnnotation)
                ?.takeIf { it.isAnnotationType }
                ?.qualifiedName
                ?: return null

        val parentClass = method.parentOfType<PsiClass>() ?: return null
        val isMetaAnnotationFound = parentClass.qualifiedName == aliasedClassQn
                || parentClass.isMetaAnnotatedBy(aliasedClassQn)

        if (isMetaAnnotationFound) return null

        val annotationMemberValue = aliasAnnotation.findAttributeValue("annotation")
            .toSourcePsi() ?: return null
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