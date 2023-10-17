package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.service.AliasUtils
import com.esprito.util.EspritoPsiUtil.fitsForReference
import com.esprito.util.EspritoPsiUtil.getHighlightRange
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.parentOfType


class SpringUnknownAliasMethodInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun checkMethod(
        method: PsiMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val parentClass = method.parentOfType<PsiClass>() ?: return null
        val aliasAnnotation = method.getAnnotation(SpringCoreClasses.ALIAS_FOR) ?: return null
        val aliasedClass = AliasUtils.getAliasedClass(aliasAnnotation) ?: return null
        val aliasedClassQn = aliasedClass.qualifiedName ?: return null

        val methodReferences = listOfNotNull(
            aliasAnnotation.findAttributeValue("value"),
            aliasAnnotation.findAttributeValue("attribute")
        )

        val isMetaAnnotationOmitted = parentClass.qualifiedName != aliasedClassQn
                && !parentClass.isMetaAnnotatedBy(aliasedClassQn)

        val problems: MutableList<ProblemDescriptor> = mutableListOf()

        for (member in methodReferences) {
            val methodName = AnnotationUtil.getStringAttributeValue(member)
            if (methodName.isNullOrBlank()) continue
            if (isMetaAnnotationOmitted ||
                aliasedClass.allMethods.none {
                    fitsForReference(it)
                            && it.name == methodName
                }
            ) {
                problems.add(
                    problemDescriptor(manager, member, isOnTheFly)
                )
            }
        }

        return problems.toTypedArray()
    }

    private fun problemDescriptor(
        manager: InspectionManager,
        member: PsiAnnotationMemberValue,
        isOnTheFly: Boolean
    ) = manager.createProblemDescriptor(
        member,
        member.getHighlightRange(),
        SpringCoreBundle.message("esprito.spring.inspection.alias.attribute"),
        ProblemHighlightType.GENERIC_ERROR,
        isOnTheFly
    )

}