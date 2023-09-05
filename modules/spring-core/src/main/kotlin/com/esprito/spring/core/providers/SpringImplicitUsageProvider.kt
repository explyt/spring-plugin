package com.esprito.spring.core.providers

import com.esprito.spring.core.JavaEeClasses
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.util.EspritoPsiUtil.isAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isOrdinaryClass
import com.esprito.util.EspritoPsiUtil.isStatic
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod

// TODO: add statemachine annotations
// TODO: add jackson annotations
class SpringImplicitUsageProvider : ImplicitUsageProvider {
    override fun isImplicitUsage(element: PsiElement): Boolean {
        if (element is PsiClass) {
            if (!element.isOrdinaryClass) {
                return false
            }
            return element.isMetaAnnotatedBy(IMPLICIT_CLASS_ANNOTATIONS)
        }
        if (element is PsiMethod) {
            if (element.isConstructor) {
                return element.isMetaAnnotatedBy(IMPLICIT_CONSTRUCTOR_ANNOTATIONS)
            }
            if (element.isStatic) {
                return isDynamicPropertySource(element)
            }
            return element.isMetaAnnotatedBy(IMPLICIT_METHOD_ANNOTATIONS)
        }
        return false
    }

    // assigned but not used
    override fun isImplicitRead(element: PsiElement): Boolean {
        return false
    }

    // referenced but never assigned
    override fun isImplicitWrite(element: PsiElement): Boolean {
        if (element is PsiField) {
            return element.isAnnotatedBy(IMPLICIT_FIELD_ANNOTATIONS)
        }
        return false
    }

    private fun isDynamicPropertySource(element: PsiMethod): Boolean {
        return element.isStatic && element.isAnnotatedBy("org.springframework.test.context.DynamicPropertySource")
    }

    companion object {
        private val IMPLICIT_CLASS_ANNOTATIONS = setOf(
            SpringCoreClasses.COMPONENT, // meta: + annotation_type
        ) + JavaEeClasses.RESOURCE.allFqns

        private val IMPLICIT_FIELD_ANNOTATIONS = setOf(
            SpringCoreClasses.AUTOWIRED, // meta: + annotation_type
            SpringCoreClasses.VALUE, // meta: + annotation_type
            // "org.springframework.beans.factory.annotation.Required", // deprecated
        ) + JavaEeClasses.INJECT.allFqns +
                JavaEeClasses.RESOURCE.allFqns

        private val IMPLICIT_CONSTRUCTOR_ANNOTATIONS = setOf(
            SpringCoreClasses.AUTOWIRED,
        ) + JavaEeClasses.INJECT.allFqns

        private val IMPLICIT_METHOD_ANNOTATIONS = setOf(
            SpringCoreClasses.BEAN, // meta: + annotation_type
            SpringCoreClasses.AUTOWIRED, // meta: + annotation_type
            SpringCoreClasses.VALUE, // meta: + annotation_type
            "org.springframework.context.event.EventListener", // meta: + annotation_type
            "org.springframework.jmx.export.annotation.ManagedOperation",
            "org.springframework.jmx.export.annotation.ManagedAttribute",
            "org.springframework.scheduling.annotation.Scheduled", // meta: + annotation_type
            "org.springframework.scheduling.annotation.Schedules", // meta: + annotation_type
            "org.springframework.test.context.transaction.BeforeTransaction", // meta: + annotation_type
            "org.springframework.test.context.transaction.AfterTransaction", // meta: + annotation_type
        ) + JavaEeClasses.INJECT.allFqns +
                JavaEeClasses.RESOURCE.allFqns +
                JavaEeClasses.POST_CONSTRUCT.allFqns +
                JavaEeClasses.PRE_DESTROY.allFqns
    }

}


