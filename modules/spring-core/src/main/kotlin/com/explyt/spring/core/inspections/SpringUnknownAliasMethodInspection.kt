package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.AliasUtils
import com.explyt.util.ExplytPsiUtil.fitsForReference
import com.explyt.util.ExplytPsiUtil.getHighlightRange
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.toSourcePsi
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.uast.UMethod


class SpringUnknownAliasMethodInspection : SpringBaseUastLocalInspectionTool() {

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
        SpringCoreBundle.message("explyt.spring.inspection.alias.attribute"),
        ProblemHighlightType.GENERIC_ERROR,
        isOnTheFly
    )

}