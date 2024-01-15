package com.esprito.spring.core.service.conditional

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.service.PsiBean
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.util.EspritoPsiUtil.resolvedPsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.childrenOfType

class ConditionalOnClassStrategy(private val module: Module) : ExclusionStrategy {
    private val annotationHolder = SpringSearchService.getInstance(module.project)
        .getMetaAnnotations(module, SpringCoreClasses.CONDITIONAL_ON_CLASS)

    override fun shouldExclude(dependant: PsiMember, foundBeans: Collection<PsiBean>): Boolean {
        if (dependant.annotations.none { annotationHolder.contains(it) }) {
            return false
        }

        val classAttributesQn = annotationHolder.getAnnotationMemberValues(dependant, setOf("value"))
            .asSequence()
            .flatMap { it.childrenOfType<PsiTypeElement>() }
            .map { it.type }
            .mapNotNull { it.resolvedPsiClass?.qualifiedName }
            .toSet()

        val typesQn = annotationHolder.getAnnotationMemberValues(dependant, setOf("name"))
            .asSequence()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .toSet()


        if (classAttributesQn.isEmpty() && typesQn.isEmpty()) {
            return false
        }

        for (typeQn in (classAttributesQn + typesQn)) {
            val className = typeQn.split('.').lastOrNull() ?: continue

            val classFound = PsiShortNamesCache.getInstance(module.project)
                .getClassesByName(
                    className,
                    GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
                )
                .any { it.qualifiedName == typeQn }

            if (!classFound) return true
        }

        return false
    }

}