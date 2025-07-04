/*
 * Copyright © 2025 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
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


