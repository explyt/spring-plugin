package com.esprito.util

import com.esprito.spring.core.SpringCoreClasses.CONFIGURATION
import com.esprito.spring.core.SpringCoreClasses.CONFIGURATION_METHOD_ANNOTATIONS
import com.esprito.spring.core.SpringCoreClasses.CONTROLLER
import com.esprito.spring.core.SpringCoreClasses.CONTROLLER_METHOD_ANNOTATIONS
import com.esprito.util.PsiModifierListOwnerUtils.isAnnotatedBy
import com.esprito.util.PsiModifierListOwnerUtils.isNonStatic
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod

object PsiMethodUtils {
    fun PsiMethod.inClassAnnotatedBy(annotation: String, flags: Int = 0) =
        this.containingClass?.let {
            it.qualifiedName != null && it.isAnnotatedBy(annotation, flags)
        } ?: false

    fun PsiMethod.isControllerMethods() =
        this.isNonStatic()
                && MetaAnnotationUtil.isMetaAnnotatedInHierarchy(this, CONTROLLER_METHOD_ANNOTATIONS)
                && this.inClassAnnotatedBy(CONTROLLER)

    fun PsiMethod.isConfigurationMethods() =
        this.isNonStatic()
                && MetaAnnotationUtil.isMetaAnnotatedInHierarchy(this, CONFIGURATION_METHOD_ANNOTATIONS)
                && this.inClassAnnotatedBy(CONFIGURATION)

    fun PsiMethod.getAnnotationByParentAnnotationNameInHierarchy(parentAnnotationNameInHierarchy: String): PsiAnnotation? =
        this.annotations.first { psiAnnotation ->
            psiAnnotation.qualifiedName == parentAnnotationNameInHierarchy ||
                    psiAnnotation.resolveAnnotationType()?.let { psiClass ->
                        MetaAnnotationUtil.isMetaAnnotatedInHierarchy(
                            psiClass,
                            setOf(parentAnnotationNameInHierarchy)
                        )
                    } ?: false
        }
}