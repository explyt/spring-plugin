package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.inspections.quickfix.AddAsMethodArgQuickFix
import com.esprito.util.EspritoAnnotationUtil.getBooleanValue
import com.esprito.util.EspritoAnnotationUtil.getMetaAnnotationMemberValues
import com.esprito.util.EspritoPsiUtil.findChildrenOfType
import com.esprito.util.EspritoPsiUtil.getHighlightRange
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.*
import com.intellij.psi.util.childrenOfType
import org.jetbrains.uast.UClass
import org.jetbrains.uast.getUastParentOfType

class SpringConfigurationProxyMethodsInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun checkMethod(
        method: PsiMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val surroundingClass: PsiClass = method.getUastParentOfType<UClass>()?.javaPsi ?: return null
        if (surroundingClass.hasModifierProperty(PsiModifier.STATIC)) return null
        if (!method.isMetaAnnotatedBy(SpringCoreClasses.BEAN)) return null
        var topClass: PsiClass? = surroundingClass

        while (topClass != null) {
            val proxyBeanMethodsValue = topClass.getMetaAnnotationMemberValues(
                SpringCoreClasses.CONFIGURATION,
                "proxyBeanMethods"
            )?.firstOrNull()
                ?.getBooleanValue()

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
            } else if (proxyBeanMethodsValue == null //hot metaAnnotated by Configuration
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
        callExpression: PsiMethodCallExpression,
        isOnTheFly: Boolean
    ): ProblemDescriptor? {
        val identifier = callExpression.childrenOfType<PsiReferenceExpression>().firstOrNull()
            ?.childrenOfType<PsiIdentifier>()?.firstOrNull() ?: return null

        return manager.createProblemDescriptor(
            identifier,
            identifier.getHighlightRange(),
            message,
            ProblemHighlightType.GENERIC_ERROR,
            isOnTheFly,
            AddAsMethodArgQuickFix(identifier)
        )
    }

    private fun findCallsToLocalBeans(psiMethod: PsiMethod, surroundingClass: PsiClass): List<PsiMethodCallExpression> {
        val beanMethods = surroundingClass.methods
            .filter { it.isMetaAnnotatedBy(SpringCoreClasses.BEAN) }
            .toSet()
        val methodCallExpressions = psiMethod.findChildrenOfType<PsiMethodCallExpression>()

        return methodCallExpressions.filter {
            beanMethods.contains(it.resolveMethod())
        }.toList()
    }

}