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

package com.explyt.jpa.service

import com.explyt.jpa.JpaClasses
import com.explyt.util.ExplytPsiUtil.isStatic
import com.explyt.util.ExplytPsiUtil.isTransient
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement

@Service(Service.Level.PROJECT)
class JpaService {

    fun isJpaEntity(psiFile: PsiFile): Boolean {
        val uFile = psiFile.toUElement() as? UFile ?: return false
        return uFile.classes.any { isJpaEntity(it.javaPsi) }
    }

    fun isJpaEntity(psiClass: PsiClass): Boolean {
        val jpaAnnotations = listOf(
            JpaClasses.entity,
            JpaClasses.mappedSuperclass,
            JpaClasses.embeddable
        ).flatMap {
            it.allFqns
        }

        return psiClass.annotations.any { it.qualifiedName in jpaAnnotations }
    }

    fun isJpaEntityAttribute(psiField: PsiField): Boolean {
        if (psiField.isStatic)
            return false

        if (psiField.isTransient)
            return false

        val psiClass = psiField.containingClass ?: return false

        return isJpaEntity(psiClass)
    }

    companion object {
        fun getInstance(project: Project): JpaService = project.service()
    }
}