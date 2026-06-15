/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.language.profiles


import com.explyt.spring.core.color.ExplytConfigurationHighlighting
import com.explyt.util.ExplytKotlinUtil.mapToArray
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.pom.references.PomService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

open class ProfilePsiReference(
    private val element: PsiElement,
    private val profileName: String,
    range: TextRange,
) : PsiReferenceBase<PsiElement>(element, range) {

    fun getTextAttributesKey(): TextAttributesKey {
        return ExplytConfigurationHighlighting.PROFILE_HIGHLIGHTER_KEY
    }

    override fun resolve(): PsiElement? {
        if (StringUtil.isEmptyOrSpaces(profileName)) return myElement
        return ProfilesUtil.findProfiles(element.project, profileName).firstOrNull()
            ?.let { PomService.convertToPsi(getElement().project, it) }
    }

    override fun isSoft() = true

    override fun getVariants(): Array<Any> {
        return ProfilesUtil.findProfiles(element.project).asSequence()
            .filter { it.name.isNotBlank() }
            .mapToArray { LookupElementBuilder.create(it.name) }
    }
}