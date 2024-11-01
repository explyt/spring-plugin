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

import com.explyt.spring.core.SpringProperties.SPRING_PROFILES_ACTIVE
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext

class XmlProfilesReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(XmlTag::class.java),
            SpringXmlProfilesReferenceProvider(),
            Double.MIN_VALUE
        )
    }
}

class SpringXmlProfilesReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val xmlTag = element as? XmlTag ?: return PsiReference.EMPTY_ARRAY
        val xmlValueText = xmlTag.value.text

        val result = mutableListOf<PsiReference>()

        if (isFromMavenProfile(xmlTag)) {
            val matcher = ProfilesUtil.profilePattern.matcher(xmlValueText)

            while (matcher.find()) {
                val text = matcher.group()
                val range = TextRange.allOf(text).shiftRight(matcher.start()).shiftRight(xmlTag.name.length + 2)
                result.add(SpringProfilePsiReference(xmlTag, text, false, range))
            }
        } else if (isFromMavenPlugin(xmlTag)) {
            result.add(
                SpringProfilePsiReference(
                    xmlTag, xmlValueText, false, TextRange.allOf(xmlValueText).shiftRight(xmlTag.name.length + 2)
                )
            )
        }

        return result.toTypedArray()
    }

    private fun isFromMavenPlugin(xmlTag: XmlTag): Boolean {
        if (xmlTag.name != "profile") return false

        val pluginTag = getParentTagByPath(xmlTag, "plugin->configuration->profiles") ?: return false
        if (getParentTagByPath(pluginTag, "project->build->plugins") == null) {
            return false
        }

        if (pluginTag.subTags.none { it.name == "groupId" && it.value.text == "org.springframework.boot" }) {
            return false
        }
        if (pluginTag.subTags.none { it.name == "artifactId" && it.value.text == "spring-boot-maven-plugin" }) {
            return false
        }

        return true
    }

    private fun isFromMavenProfile(xmlTag: XmlTag): Boolean {
        if (xmlTag.name != SPRING_PROFILES_ACTIVE) return false

        return getParentTagByPath(xmlTag, "project->profiles->profile->properties") != null
    }

    private fun getParentTagByPath(xmlTag: XmlTag, pathFromParent: String): XmlTag? {
        var parentTag: XmlTag = xmlTag
        val path = pathFromParent.split("->")

        for (i in path.size - 1 downTo 0) {
            val parentTagName = path[i]

            parentTag = parentTag.parentTag ?: return null
            if (parentTag.name != parentTagName) return null
        }
        return parentTag
    }

}
