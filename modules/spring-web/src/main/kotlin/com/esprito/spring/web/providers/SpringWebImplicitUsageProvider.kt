package com.esprito.spring.web.providers

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.web.SpringWebClasses
import com.esprito.util.EspritoPsiUtil.inClassMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isStatic
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter

class SpringWebImplicitUsageProvider : ImplicitUsageProvider {
    override fun isImplicitWrite(element: PsiElement): Boolean {
        return element is PsiParameter && isImplicitParameter(element)
    }

    override fun isImplicitRead(element: PsiElement): Boolean {
        return false
    }

    override fun isImplicitUsage(element: PsiElement): Boolean {
        return when (element) {
            is PsiClass -> {
                return element.isAnnotatedBy(IMPLICIT_CLASS_ANNOTATIONS)
            }

            is PsiMethod -> {
                return !element.isStatic && !element.isConstructor
                        && element.inClassMetaAnnotatedBy(SpringCoreClasses.COMPONENT) // or bean
                        && element.isMetaAnnotatedBy(IMPLICIT_METHOD_ANNOTATIONS)
            }

            else -> false
        }
    }

    private fun isImplicitParameter(element: PsiParameter): Boolean {
        return element.isAnnotatedBy(IMPLICIT_PARAMETER_ANNOTATIONS)
                || element.isAnnotatedBy(SpringWebClasses.MODEL_ATTRIBUTE)
    }

    companion object {
        val IMPLICIT_CLASS_ANNOTATIONS = setOf(
            "jakarta.servlet.annotation.WebFilter",
            "jakarta.servlet.annotation.WebListener",
            "jakarta.servlet.annotation.WebServlet",
            "javax.servlet.annotation.WebFilter",
            "javax.servlet.annotation.WebListener",
            "javax.servlet.annotation.WebServlet",
        )
        val IMPLICIT_PARAMETER_ANNOTATIONS: Collection<String> = listOf(
            "org.springframework.web.bind.annotation.PathVariable",
            "org.springframework.web.bind.annotation.RequestParam"
        )
        val IMPLICIT_METHOD_ANNOTATIONS = setOf(
            "org.springframework.web.bind.annotation.ExceptionHandler",
            "org.springframework.web.bind.annotation.InitBinder",
            "org.springframework.web.bind.annotation.RequestMapping",
            SpringWebClasses.MODEL_ATTRIBUTE,
            "org.springframework.graphql.data.method.annotation.QueryMapping",
            "org.springframework.graphql.data.method.annotation.MutationMapping",
            "org.springframework.graphql.data.method.annotation.BatchMapping",
            "org.springframework.graphql.data.method.annotation.SubscriptionMapping",
            "org.springframework.graphql.data.method.annotation.SchemaMapping", // + type
        )

    }

}


