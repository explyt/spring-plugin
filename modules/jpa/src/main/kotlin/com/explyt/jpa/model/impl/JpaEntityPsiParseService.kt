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

package com.explyt.jpa.model.impl

import com.explyt.jpa.JpaClasses
import com.explyt.jpa.model.JpaEntityAttribute
import com.explyt.jpa.service.JpaService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.toUElementOfType

@Service(Service.Level.PROJECT)
class JpaEntityPsiParseService(
    private val project: Project
) {
    private val jpaService = JpaService.getInstance(project)

    fun computeName(psiElement: PsiElement): String? {
        val uClass = psiElement.toUElementOfType<UClass>()
            ?: return null

        val entityAnnotation = JpaClasses.entity.allFqns.asSequence()
            .mapNotNull { uClass.findAnnotation(it) }
            .firstOrNull()
            ?: return uClass.qualifiedName?.substringAfterLast('.')

        val nameAttribute = entityAnnotation.findAttributeValue("name")

        return nameAttribute?.evaluateString()
            ?: return uClass.qualifiedName?.substringAfterLast('.')
    }

    fun computeAttributes(psiElement: PsiElement): List<JpaEntityAttribute> {
        val uClass = psiElement.toUElementOfType<UClass>()
            ?: return emptyList()

        return loadPossibleEntityAttributeFields(uClass)
            .asSequence()
            .filter { isEntityAttribute(it) }
            .mapNotNull { JpaEntityAttributePsi(it) }
            .toList()
    }

    private fun loadPossibleEntityAttributeFields(
        uClass: UClass?,
        visited: MutableSet<UClass> = mutableSetOf()
    ): Array<UField> {
        if (uClass == null)
            return emptyArray()

        if (uClass in visited)
            return emptyArray()

        visited.add(uClass)

        val superClass = uClass.javaPsi.superClass
            ?.toUElementOfType<UClass>()

        return uClass.fields + loadPossibleEntityAttributeFields(superClass, visited)
    }

    private fun isEntityAttribute(uField: UField): Boolean {
        val psiField = uField.javaPsi as? PsiField ?: return false

        return jpaService.isJpaEntityAttribute(psiField)
    }

    fun isPersistent(psiElement: PsiClass): Boolean = JpaClasses.entity.allFqns.any {
        psiElement.hasAnnotation(it)
    }

    companion object {
        fun getInstance(project: Project): JpaEntityPsiParseService = project.service()
    }

}