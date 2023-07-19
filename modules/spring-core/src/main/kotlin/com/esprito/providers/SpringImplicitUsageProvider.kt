package com.esprito.providers

import com.esprito.spring.core.SpringCoreClasses.FIELD_ANNOTATIONS
import com.esprito.spring.core.SpringCoreClasses.TYPE_ANNOTATIONS
import com.esprito.util.PsiModifierListOwnerUtils.isAnnotatedBy
import com.esprito.util.PsiMethodUtils.isConfigurationMethods
import com.esprito.util.PsiMethodUtils.isControllerMethods
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.*

class SpringImplicitUsageProvider : ImplicitUsageProvider {
    override fun isImplicitWrite(element: PsiElement): Boolean {
        return element is PsiField && element.isAnnotatedBy(FIELD_ANNOTATIONS)
    }

    override fun isImplicitRead(element: PsiElement): Boolean {
        return false
    }

    override fun isImplicitUsage(element: PsiElement): Boolean {
        if (element is PsiClass) {
            return MetaAnnotationUtil.isMetaAnnotatedInHierarchy(element, TYPE_ANNOTATIONS)
        }
        if (element is PsiMethod) {
            return element.isControllerMethods() || element.isConfigurationMethods()
        }
        return false
    }
}


