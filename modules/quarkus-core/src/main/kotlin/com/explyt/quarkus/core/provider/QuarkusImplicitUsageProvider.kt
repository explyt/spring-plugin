/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.quarkus.core.provider

import com.explyt.quarkus.core.QuarkusCoreClasses
import com.explyt.spring.core.JavaEeClasses
import com.explyt.util.ExplytPsiUtil.isAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isOrdinaryClass
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod

class QuarkusImplicitUsageProvider : ImplicitUsageProvider {
    override fun isImplicitUsage(element: PsiElement): Boolean {
        if (element is PsiClass) {
            if (!element.isOrdinaryClass) {
                return false
            }
            return element.isMetaAnnotatedBy(QuarkusCoreClasses.COMPONENTS_ANNO)
        }
        if (element is PsiMethod) {
            if (element.isConstructor) {
                return element.isMetaAnnotatedBy(JavaEeClasses.INJECT.allFqns)
                        || element.containingClass?.let {
                    it.isMetaAnnotatedBy(JavaEeClasses.INJECT.allFqns)
                            && it.constructors.size == 1
                } ?: false
            }

            return element.isMetaAnnotatedBy(IMPLICIT_METHOD_ANNOTATIONS)
        }
        if (element is PsiField) {
            return element.isMetaAnnotatedBy(QuarkusCoreClasses.PRODUCES.allFqns)
        }
        return false
    }

    // assigned but not used
    override fun isImplicitRead(element: PsiElement): Boolean {
        if (element is PsiField) {
            return element.isAnnotatedBy(QuarkusCoreClasses.PRODUCES.allFqns)
        }
        return false
    }

    // referenced but never assigned
    override fun isImplicitWrite(element: PsiElement): Boolean {
        if (element is PsiField) {
            return element.isAnnotatedBy(IMPLICIT_FIELD_ANNOTATIONS)
        }
        return false
    }

    companion object {

        private val IMPLICIT_FIELD_ANNOTATIONS = JavaEeClasses.INJECT.allFqns +
                JavaEeClasses.RESOURCE.allFqns + QuarkusCoreClasses.PRODUCES.allFqns

        private val IMPLICIT_METHOD_ANNOTATIONS = QuarkusCoreClasses.PRODUCES.allFqns +
                QuarkusCoreClasses.HTTP_METHOD.allFqns +
                QuarkusCoreClasses.AROUNT_TIMEOUT.allFqns +
                QuarkusCoreClasses.AROUNT_CONSTRUCT.allFqns +
                QuarkusCoreClasses.AROUNT_INVOKE.allFqns +
                JavaEeClasses.INJECT.allFqns +
                JavaEeClasses.RESOURCE.allFqns +
                JavaEeClasses.POST_CONSTRUCT.allFqns +
                JavaEeClasses.PRE_DESTROY.allFqns
    }

}


