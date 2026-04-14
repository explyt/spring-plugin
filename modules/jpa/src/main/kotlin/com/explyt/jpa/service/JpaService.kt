/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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