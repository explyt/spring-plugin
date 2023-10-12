package com.esprito.spring.core.service.conditional

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.esprito.spring.core.service.PsiBean
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMember
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache

class ConditionalOnMissingClassStrategy(private val module: Module) : ExclusionStrategy {
    private val annotationHolder = MetaAnnotationsHolder.of(module, SpringCoreClasses.CONDITIONAL_ON_MISSING_CLASS)

    override fun shouldExclude(dependant: PsiMember, foundBeans: Collection<PsiBean>): Boolean {
        if (dependant.annotations.none { annotationHolder.contains(it) }) {
            return false
        }

        val types = annotationHolder.getAnnotationMemberValues(dependant, setOf("value"))
            .asSequence()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .toSet()

        for (typeQn in types) {
            val className = typeQn.split('.').lastOrNull() ?: continue

            val classFound = PsiShortNamesCache.getInstance(module.project)
                .getClassesByName(
                    className,
                    GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
                )
                .any { it.qualifiedName == typeQn }

            if (classFound) return true
        }

        return false
    }

}