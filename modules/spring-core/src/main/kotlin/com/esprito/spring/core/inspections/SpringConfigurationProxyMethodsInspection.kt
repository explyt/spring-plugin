package com.esprito.spring.core.inspections

import com.esprito.inspection.SpringBaseUastLocalInspectionTool
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.inspections.quickfix.AddAsMethodArgQuickFix
import com.esprito.util.EspritoAnnotationUtil.getUMetaAnnotation
import com.esprito.util.EspritoPsiUtil.findChildrenOfType
import com.esprito.util.EspritoPsiUtil.getHighlightRange
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.toSourcePsi
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.*

class SpringConfigurationProxyMethodsInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val method = uMethod.javaPsi
        val uClass = uMethod.getParentOfType<UClass>() ?: return null
        val surroundingClass: PsiClass = uClass.javaPsi
        if (uClass.isStatic) return null
        if (!method.isMetaAnnotatedBy(SpringCoreClasses.BEAN)) return null
        var topClass: PsiClass? = surroundingClass

        while (topClass != null) {
            val proxyBeanMethodsValue = uClass.getUMetaAnnotation(SpringCoreClasses.CONFIGURATION)
                ?.javaPsi?.let {
                    AnnotationUtil.getBooleanAttributeValue(it, "proxyBeanMethods")
                }

            if (proxyBeanMethodsValue == false) {
                return findCallsToLocalBeans(method, topClass).asSequence()
                    .mapNotNull {
                        createProblemDescriptor(
                            manager,
                            SpringCoreBundle.message("esprito.spring.inspection.configuration.proxy.incorrect"),
                            it,
                            isOnTheFly
                        )
                    }.toList().toTypedArray()
            } else if (proxyBeanMethodsValue == null //not metaAnnotated by Configuration
                && topClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)
            ) {
                return findCallsToLocalBeans(method, topClass).asSequence()
                    .mapNotNull {
                        createProblemDescriptor(
                            manager,
                            SpringCoreBundle.message("esprito.spring.inspection.configuration.light-bean.incorrect"),
                            it,
                            isOnTheFly
                        )
                    }.toList().toTypedArray()
            }
            topClass = topClass.containingClass
        }
        return null
    }

    private fun createProblemDescriptor(
        manager: InspectionManager,
        message: String,
        callExpression: UCallExpression,
        isOnTheFly: Boolean
    ): ProblemDescriptor? {
        val identifier = callExpression.methodIdentifier?.sourcePsi ?: return null
        val fixes = listOfNotNull(
            (identifier as? PsiIdentifier)
                ?.let { AddAsMethodArgQuickFix(it) }
        ).toTypedArray()

        return manager.createProblemDescriptor(
            identifier,
            identifier.getHighlightRange(),
            message,
            ProblemHighlightType.GENERIC_ERROR,
            isOnTheFly,
            *fixes
        )
    }

    private fun findCallsToLocalBeans(psiMethod: PsiMethod, surroundingClass: PsiClass): List<UCallExpression> {
        val beanMethods = surroundingClass.methods
            .filter { it.isMetaAnnotatedBy(SpringCoreClasses.BEAN) }
            .toSet()

        return psiMethod.toSourcePsi()?.findChildrenOfType<PsiElement>()?.asSequence()
            ?.mapNotNull { it.toUElement() as? UCallExpression }
            ?.filter { beanMethods.contains(it.resolve()) }
            ?.toList()
            ?: emptyList()
    }

}