package com.esprito.spring.core.language.profiles

import com.esprito.spring.core.language.profiles.psi.ProfilesProfile
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*


open class ProfilesReference(element: PsiElement, private val profileName: String, rangeInElement: TextRange) :
    PsiReferenceBase<PsiElement>(element, rangeInElement),
    PsiPolyVariantReference {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val project: Project = myElement.project
        val profiles: List<ProfilesProfile> = ProfilesUtil.findProfiles(project, profileName)
        val results: MutableList<ResolveResult> = ArrayList()
        for (profile in profiles) {
            results.add(PsiElementResolveResult(profile))
        }
        return results.toTypedArray()
    }

    override fun resolve(): PsiElement? {
        val resolveResults = multiResolve(false)
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

    override fun getVariants(): Array<Any> {
        val project: Project = myElement.project
        val profiles: List<ProfilesProfile> = ProfilesUtil.findProfiles(project)
        val variants: MutableList<LookupElement> = ArrayList()
        for (profile in profiles) {
            if (!profile.value.text.isNullOrBlank()) {
                variants.add(
                    LookupElementBuilder
                        .create(profile)
                        .withTypeText(profile.containingFile.name)
                )
            }
        }
        return variants.toTypedArray()
    }

}