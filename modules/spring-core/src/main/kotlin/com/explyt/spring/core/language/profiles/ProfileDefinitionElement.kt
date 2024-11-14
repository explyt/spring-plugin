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

package com.explyt.spring.core.language.profiles

import com.explyt.util.ExplytPsiUtil.toSmartPointer
import com.intellij.ide.presentation.Presentation
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.pom.PomRenameableTarget
import com.intellij.psi.PsiElement


@Presentation(typeName = "Spring Profile")
class ProfileDefinitionElement(element: PsiElement, private var name: String, private val nameOffset: Int) :
    PomRenameableTarget<Any> {
    private val smartPointer = element.toSmartPointer()

    override fun isValid() = smartPointer.element != null

    override fun getName() = name

    override fun isWritable() = true

    override fun setName(newName: String): Any? {
        name = newName
        return null
    }

    override fun navigate(requestFocus: Boolean) {
        val elementRange = smartPointer.range ?: return
        var offset: Int = elementRange.startOffset
        val project = smartPointer.project
        if (nameOffset < elementRange.endOffset - offset) {
            offset += nameOffset
        }
        smartPointer.virtualFile?.takeIf { it.isValid }?.also {
            PsiNavigationSupport.getInstance().createNavigatable(project, it, offset).navigate(requestFocus)
        }
    }

    override fun canNavigate() = canNavigateToSource()

    override fun canNavigateToSource(): Boolean {
        if (nameOffset < 0) return false
        val element: PsiElement? = smartPointer.element
        return element != null && PsiNavigationSupport.getInstance().canNavigate(element)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProfileDefinitionElement

        return name == other.name
    }

    override fun hashCode() = name.hashCode()
}