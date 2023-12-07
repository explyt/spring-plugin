package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.service.AliasUtils
import com.esprito.util.EspritoPsiUtil.fitsForReference
import com.esprito.util.EspritoPsiUtil.getHighlightRange
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.toSourcePsi
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.uast.UMethod


class SpringUnknownAliasMethodInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val method = uMethod.javaPsi
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
            val memberSourcePsi = member.toSourcePsi() ?: continue
            val methodName = AnnotationUtil.getStringAttributeValue(member)
            if (methodName.isNullOrBlank()) continue

            if (isMetaAnnotationOmitted ||
                aliasedClass.allMethods.none {
                    fitsForReference(it)
                            && it.name == methodName
                }
            ) {
                problems += problemDescriptor(manager, memberSourcePsi, isOnTheFly)
            }
        }

        return problems.toTypedArray()
    }

    private fun problemDescriptor(
        manager: InspectionManager,
        member: PsiElement,
        isOnTheFly: Boolean
    ) = manager.createProblemDescriptor(
        member,
        member.getHighlightRange(),
        SpringCoreBundle.message("esprito.spring.inspection.alias.attribute"),
        ProblemHighlightType.GENERIC_ERROR,
        isOnTheFly
    )

}