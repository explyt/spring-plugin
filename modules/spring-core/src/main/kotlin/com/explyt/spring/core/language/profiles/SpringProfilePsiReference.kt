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