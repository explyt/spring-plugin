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


import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.pom.references.PomService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

open class SpringProfilePsiReference(
    private val element: PsiElement,
    private val profileName: String,
    private val definition: Boolean,
    range: TextRange,
) : PsiReferenceBase<PsiElement>(element, range) {

    override fun resolve(): PsiElement? {
        if (StringUtil.isEmptyOrSpaces(profileName)) return myElement
        val target: SpringProfileTarget? =
            if (definition) {
                SpringProfileTarget(getElement(), profileName, getRangeInElement().startOffset)
            } else {
                ProfilesUtil.findTargetProfiles(element.project, profileName).firstOrNull()
            }
        return if (target == null) null else PomService.convertToPsi(getElement().project, target)
    }

    override fun isSoft(): Boolean {
        return true
    }

    override fun getVariants(): Array<Any> {
        val project: Project = myElement.project
        val profiles: List<SpringProfileTarget> = ProfilesUtil.findTargetProfiles(project)
        val variants: MutableList<LookupElement> = ArrayList()
        for (profile in profiles) {
            if (profile.name.isNotBlank()) {
                variants.add(LookupElementBuilder.create(profile.name))
            }
        }
        return variants.toTypedArray()
    }
}