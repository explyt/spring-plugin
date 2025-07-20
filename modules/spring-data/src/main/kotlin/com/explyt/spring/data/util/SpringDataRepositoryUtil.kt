/*
 * Copyright © 2024 Explyt Ltd
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

package com.explyt.spring.data.util

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.util.PsiAnnotationUtils
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.explyt.spring.data.SpringDataClasses
import com.explyt.spring.data.SpringDataClasses.REPOSITORY
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil

object SpringDataRepositoryUtil {

    fun getGenericTypes(repositoryClass: PsiClass): RepositoryTypes? {
        if (repositoryClass.isMetaAnnotatedBy(SpringDataClasses.REPOSITORY_ANNOTATION)) {
            return getGenericTypesForRepositoryDefinition(repositoryClass)
        }

        val psiClassType = JavaPsiFacade.getInstance(repositoryClass.project).elementFactory.createType(repositoryClass)

        val psiType = PsiUtil.substituteTypeParameter(psiClassType, REPOSITORY, 0, false) ?: return null
        val idPsiType = PsiUtil.substituteTypeParameter(psiClassType, REPOSITORY, 1, false) ?: return null
        val psiClass = (psiType as? PsiClassType)?.resolve() ?: return null
        return RepositoryTypes(psiClass, idPsiType)
    }

    fun isNumberType(psiType: PsiType?): Boolean {
        if (isNumberTypeInner(psiType)) return true
        if ((psiType as? PsiClassType)?.hasParameters() == false) return false
        val resolvedPsiClass = psiType?.resolvedPsiClass ?: return false
        return isNumberTypeInner(PsiUtil.substituteTypeParameter(psiType, resolvedPsiClass, 0, false))
    }

    private fun isNumberTypeInner(psiType: PsiType?) =
        psiType?.isAssignableFrom(PsiTypes.intType()) == true
                || psiType?.isAssignableFrom(PsiTypes.longType()) == true
                || psiType?.isAssignableFrom(PsiTypes.byteType()) == true
                || psiType?.isAssignableFrom(PsiTypes.shortType()) == true

    fun isVoidType(psiType: PsiType?): Boolean {
        if (psiType?.isAssignableFrom(PsiTypes.voidType()) == true) return true
        if ((psiType as? PsiClassType)?.hasParameters() == false) return false
        val resolvedPsiClass = psiType?.resolvedPsiClass ?: return false
        val substituteTypeParameter = PsiUtil.substituteTypeParameter(psiType, resolvedPsiClass, 0, false)
            ?: return false
        return substituteTypeParameter.isAssignableFrom(PsiTypes.voidType())
                || substituteTypeParameter.canonicalText == Void::class.java.canonicalName
    }

    fun isBooleanType(psiType: PsiType?): Boolean {
        if (psiType?.isAssignableFrom(PsiTypes.booleanType()) == true) return true
        if ((psiType as? PsiClassType)?.hasParameters() == false) return false
        val resolvedPsiClass = psiType?.resolvedPsiClass ?: return false
        return PsiUtil.substituteTypeParameter(psiType, resolvedPsiClass, 0, false)
            ?.isAssignableFrom(PsiTypes.booleanType()) == true
    }

    private fun getGenericTypesForRepositoryDefinition(repositoryClass: PsiClass): RepositoryTypes? {
        val module = ModuleUtilCore.findModuleForPsiElement(repositoryClass) ?: return null
        val metaAnnotationsHolder = MetaAnnotationsHolder.of(module, SpringDataClasses.REPOSITORY_ANNOTATION)
        val annotation = repositoryClass.annotations.find { metaAnnotationsHolder.contains(it) } ?: return null
        val domainValues = metaAnnotationsHolder.getAnnotationMemberValues(annotation, setOf("domainClass"))
        val idValues = metaAnnotationsHolder.getAnnotationMemberValues(annotation, setOf("idClass"))
        val classDomain = PsiAnnotationUtils.getPsiTypes(domainValues).map { it.resolveBeanPsiClass }.firstOrNull()
        val typeId = PsiAnnotationUtils.getPsiTypes(idValues).firstOrNull()
        return if (classDomain != null && typeId != null) RepositoryTypes(classDomain, typeId) else null
    }
}

data class RepositoryTypes(val psiClass: PsiClass, val psiType: PsiType)