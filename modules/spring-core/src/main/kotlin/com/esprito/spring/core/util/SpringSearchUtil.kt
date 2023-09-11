package com.esprito.spring.core.util

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.JavaEeClasses
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.util.EspritoPsiUtil.isAnnotatedBy
import com.esprito.util.EspritoPsiUtil.resolvedPsiClass
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.uast.UastModificationTracker

object SpringSearchUtil {

    fun findAllBeanClasses(module: Module): Set<PsiClass> {
        val allAnnotatedByComponentClasses = getComponentPsiClasses(module)
        val methodsAnnotatedByBeanReturnTypes =
            getComponentPsiClassesByBeanMethods(allAnnotatedByComponentClasses)
        return allAnnotatedByComponentClasses + methodsAnnotatedByBeanReturnTypes
    }

    fun getComponentPsiClasses(module: Module): Set<PsiClass> {
        val project = module.project
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getComponentPsiClasses(module, getComponentClassAnnotations(module)),
                UastModificationTracker.getInstance(project)
            )
        }
    }

    fun getComponentPsiClassesByBeanMethods(allAnnotatedByComponentClasses: Set<PsiClass>): Set<PsiClass> {
        return allAnnotatedByComponentClasses
            .asSequence()
            .flatMap { it.allMethods.asSequence() }
            .filter { it.isAnnotatedBy(SpringCoreClasses.BEAN) }
            .mapNotNull { it.returnType?.resolvedPsiClass }
            .filter { SpringCoreUtil.isSpringBeanCandidateClass(it) }
            .toSet()
    }

    private fun getComponentPsiClasses(module: Module, annotationPsiClasses: Collection<PsiClass>): Set<PsiClass> {
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        return annotationPsiClasses.asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, scope) }
            .filter { SpringCoreUtil.isSpringBeanCandidateClass(it) }
            .toSet()
    }

    private fun getComponentClassAnnotations(module: Module): Collection<PsiClass> {
        // TODO: cache result
        val annotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(module, SpringCoreClasses.COMPONENT, false)
        annotations += LibraryClassCache.searchForLibraryClasses(module, JavaEeClasses.RESOURCE.allFqns)
        return annotations
    }

    fun findAllComponentScanFromCache(module: Module): Set<PsiClass> {
        val project = module.project
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getPsiClassesByAnnotation(module, SpringCoreClasses.COMPONENT_SCAN),
                UastModificationTracker.getInstance(project)
            )
        }
    }

    private fun getPsiClassesByAnnotation(module: Module, annotationName: String): Set<PsiClass> {
        val annotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(module, annotationName, false)
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        return annotations.asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, scope) }
            .filter { SpringCoreUtil.isSpringBeanCandidateClass(it) }
            .toSet()
    }

}