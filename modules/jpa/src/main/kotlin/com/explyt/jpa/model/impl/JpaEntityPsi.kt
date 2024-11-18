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

import com.explyt.jpa.model.JpaEntity
import com.explyt.jpa.model.JpaEntityAttribute
import com.intellij.psi.PsiClass
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

class JpaEntityPsi private constructor(
    psiClass: PsiClass
) : JpaEntity {
    private val psiElementPointer = SmartPointerManager.createPointer(psiClass)

    private val psiParseService = JpaEntityPsiParseService.getInstance(psiElementPointer.project)

    override val psiElement: PsiClass?
        get() = psiElementPointer.element

    override val isValid: Boolean
        get() = psiElementPointer.element != null

    override val name: String?
        get() {
            val psiElement = psiElement ?: return null
            return psiParseService.computeName(psiElement)
        }

    override val attributes: List<JpaEntityAttribute>
        get() {
            val psiElement = psiElement ?: return emptyList()

            return psiParseService.computeAttributes(psiElement)
        }

    override val isPersistent: Boolean
        get() {
            val psiElement = psiElement ?: return false

            return psiParseService.isPersistent(psiElement)
        }

    override fun toString(): String {
        if (isValid) {
            return "${psiElement?.qualifiedName}: $name"
        }

        return "--outdated--"
    }

    companion object {
        operator fun invoke(psiClass: PsiClass): JpaEntityPsi {
            return CachedValuesManager.getCachedValue(psiClass) {
                CachedValueProvider.Result(
                    JpaEntityPsi(psiClass),
                    psiClass
                )
            }
        }
    }
}