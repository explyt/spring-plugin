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

import com.explyt.jpa.model.JpaEntityAttribute
import com.explyt.jpa.model.JpaEntityAttributeType
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.uast.UField
import org.jetbrains.uast.toUElementOfType

class JpaEntityAttributePsi private constructor(
    psiElement: PsiElement
) : JpaEntityAttribute {
    private val psiElementPointer = SmartPointerManager.createPointer(psiElement)

    private val psiParseService = JpaEntityAttributePsiParseService.getInstance(psiElementPointer.project)

    override val psiElement: PsiElement?
        get() = psiElementPointer.element

    override val isValid: Boolean
        get() = psiElement != null

    override val name: String?
        get() {
            @Suppress("UElementAsPsi")
            return psiElement?.toUElementOfType<UField>()?.name
        }

    override val type: JpaEntityAttributeType
        get() {
            val uField = (psiElement?.toUElementOfType<UField>()
                ?: return JpaEntityAttributeType.Unknown)

            return psiParseService.computeAttributeType(uField)
        }

    companion object {
        operator fun invoke(uField: UField): JpaEntityAttributePsi? {
            val sourcePsi = uField.sourcePsi
                ?: return null

            return CachedValuesManager.getCachedValue(sourcePsi) {
                CachedValueProvider.Result(
                    JpaEntityAttributePsi(sourcePsi),
                    sourcePsi
                )
            }
        }
    }
}