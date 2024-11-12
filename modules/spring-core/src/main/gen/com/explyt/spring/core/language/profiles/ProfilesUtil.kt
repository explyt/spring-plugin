package com.explyt.spring.core.language.profiles

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.DelimitedListProcessor
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.ElementManipulators
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.toUElementOfType
import java.util.regex.Pattern


object ProfilesUtil {

    @JvmStatic
    fun findTargetProfiles(project: Project, key: String): List<SpringProfileTarget> {
        return findTargetProfiles(project)
            .filter { profile -> profile.name == key }
    }

    @JvmStatic
    fun findTargetProfiles(project: Project): List<SpringProfileTarget> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                doFindTargetProfiles(project),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun doFindTargetProfiles(project: Project): List<SpringProfileTarget> {
        val profileClass = LibraryClassCache
            .searchForLibraryClass(project, SpringCoreClasses.PROFILE) ?: return listOf()
        val query = AnnotatedElementsSearch.searchPsiMembers(profileClass, ProjectScope.getProjectScope(project))

        val profiles = mutableListOf<SpringProfileTarget>()

        val annotatedMembers = query.findAll().toList()
        val psiAnnotations = annotatedMembers
            .flatMap { MetaAnnotationUtil.findMetaAnnotations(it, setOf(SpringCoreClasses.PROFILE)).toList() }

        for (psiAnnotation in psiAnnotations) {
            val uAnnotation = psiAnnotation.toUElementOfType<UAnnotation>() ?: continue

            val attributeValueExpression = uAnnotation.findAttributeValue("value")
            val sourcePsi = attributeValueExpression?.sourcePsi ?: continue
            val text = ElementManipulators.getValueText(sourcePsi)
            val profileRanges = getProfileRanges(text)
            profiles += profileRanges.map { SpringProfileTarget(sourcePsi, it.substring(text), it.startOffset) }
        }

        return profiles
    }

    fun getProfileRanges(value: String): List<TextRange> {
        if (StringUtil.isEmptyOrSpaces(value)) return emptyList()
        val ranges: MutableList<TextRange> = ArrayList(1)
        object : DelimitedListProcessor(PROFILE_DELIMITERS) {
            override fun processToken(start: Int, end: Int, delimitersOnly: Boolean) {
                val profileName = value.substring(start, end)
                val profileNameTrimmed = profileName.trim()
                val profileNameIdx = profileName.indexOf(profileNameTrimmed)
                val trimmedRange = TextRange.from(start + profileNameIdx, profileNameTrimmed.length)
                if (trimmedRange.length > 0) {
                    ranges.add(trimmedRange)
                }
            }
        }.processText(value)
        return ranges
    }

    private const val PROFILE_REGEX = "[\\p{L}_0-9]+"
    private const val PROFILE_DELIMITERS = "()&|!"
    val profilePattern: Pattern = Pattern.compile(PROFILE_REGEX)
}