package com.esprito.spring.core.language.profiles

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.language.profiles.psi.ProfilesFile
import com.esprito.spring.core.language.profiles.psi.ProfilesProfile
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.contextOfType
import com.intellij.uast.UastModificationTracker
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.isInjectionHost
import org.jetbrains.uast.toUElement
import java.util.regex.Pattern


object ProfilesUtil {

    @JvmStatic
    fun findProfiles(project: Project, key: String): List<ProfilesProfile> {
        return findProfiles(project)
            .filter { profile -> profile.name == key }
    }

    @JvmStatic
    fun findProfiles(project: Project): List<ProfilesProfile> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                doFindProfiles(project),
                UastModificationTracker.getInstance(project)
            )
        }
    }

    private fun doFindProfiles(project: Project): List<ProfilesProfile> {
        val profileClass = LibraryClassCache
            .searchForLibraryClass(project, SpringCoreClasses.PROFILE) ?: return listOf()
        val query = AnnotatedElementsSearch.searchPsiMembers(profileClass, ProjectScope.getProjectScope(project))

        val profiles = mutableListOf<ProfilesProfile>()

        val annotatedMembers = query.findAll().toList()
        val psiAnnotations = annotatedMembers
            .flatMap { psiMember ->
                MetaAnnotationUtil.findMetaAnnotations(
                    psiMember,
                    setOf(SpringCoreClasses.PROFILE)
                ).toList()
            }

        for (psiAnnotation in psiAnnotations) {
            val uAnnotation = psiAnnotation.toUElement() as? UAnnotation ?: continue
            val injectionHosts = uAnnotation.attributeValues.asSequence()
                .map { it.expression }
                .filter { it.isInjectionHost() }
                .mapNotNull { it.sourcePsi }

            for (injectionHost in injectionHosts) {
                val injectedElement = InjectedLanguageManager.getInstance(injectionHost.project)
                    .findInjectedElementAt(
                        injectionHost.containingFile,
                        injectionHost.textOffset + 1
                    ) ?: continue

                val profilesFile = injectedElement.contextOfType<ProfilesFile>(true) ?: continue
                profiles.addAll(PsiTreeUtil.findChildrenOfType(profilesFile, ProfilesProfile::class.java))
            }
        }

        return profiles
    }

    private const val PROFILE_REGEX = "[\\p{L}_0-9]+"
    val profilePattern: Pattern = Pattern.compile(PROFILE_REGEX)
}