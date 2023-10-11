package com.esprito.spring.core.service.conditional

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.esprito.spring.core.service.PsiBean
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMember
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AllClassesSearch

class ConditionalOnMissingClassStrategy(private val module: Module) : ExclusionStrategy {
    private val annotationHolder = MetaAnnotationsHolder.of(module, SpringCoreClasses.CONDITIONAL_ON_MISSING_CLASS)

    override fun shouldExclude(dependant: PsiMember, foundBeans: Collection<PsiBean>): Boolean {
        if (dependant.annotations.none { annotationHolder.contains(it) }) {
            return false
        }

        val types = annotationHolder.getAnnotationMemberValues(dependant, setOf("value"))
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .toSet()

        if (types.isNotEmpty()) {
            val allClassesQn = AllClassesSearch.search(
                GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module),
                module.project
            )
                .mapNotNull { it.qualifiedName }
                .toSet()

            return types.any { allClassesQn.contains(it) }

        }

        return false
    }

}