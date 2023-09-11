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

    private fun searchAllBeanClasses(module: Module): Set<PsiClass> {
        val allPsiClassesAnnotatedByComponent = getBeanPsiClassesAnnotatedByComponent(module)
        val methodsAnnotatedByBeanReturnTypes =
            searchComponentPsiClassesByBeanMethods(allPsiClassesAnnotatedByComponent)
        return allPsiClassesAnnotatedByComponent + methodsAnnotatedByBeanReturnTypes
    }

    fun getAllBeansClasses(module: Module): Set<PsiClass> {
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result(
                searchAllBeanClasses(module),
                UastModificationTracker.getInstance(module.project)
            )
        }
    }

    fun getAllBeansClassesWithInheritors(module: Module): Set<PsiClass> {
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result(
                getAllBeansClasses(module).asSequence().flatMap { it.supers.asSequence() + it }.toSet(),
                UastModificationTracker.getInstance(module.project)
            )
        }
    }


    fun getBeanPsiClassesAnnotatedByComponent(module: Module): Set<PsiClass> {
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result(
                searchBeanPsiClassesByAnnotations(module, getComponentClassAnnotations(module)),
                UastModificationTracker.getInstance(module.project)
            )
        }
    }

    private fun searchComponentPsiClassesByBeanMethods(allAnnotatedByComponentClasses: Set<PsiClass>): Set<PsiClass> {
        return allAnnotatedByComponentClasses
            .asSequence()
            .flatMap { it.allMethods.asSequence() }
            .filter { it.isAnnotatedBy(SpringCoreClasses.BEAN) }
            .mapNotNull { it.returnType?.resolvedPsiClass }
            .filter { SpringCoreUtil.isSpringBeanCandidateClass(it) }
            .toSet()
    }

    private fun searchBeanPsiClassesByAnnotations(module: Module, annotationPsiClasses: Collection<PsiClass>): Set<PsiClass> {
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        return annotationPsiClasses.asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, scope) }
            .filter { SpringCoreUtil.isSpringBeanCandidateClass(it) }
            .toSet()
    }

    private fun searchBeanPsiClassesByAnnotation(module: Module, annotationName: String): Set<PsiClass> {
        val annotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(module, annotationName, false)
        return searchBeanPsiClassesByAnnotations(module, annotations)
    }

    private fun getComponentClassAnnotations(module: Module): Collection<PsiClass> {
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result(
                run {
                    val annotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(module, SpringCoreClasses.COMPONENT, false)
                    annotations += LibraryClassCache.searchForLibraryClasses(module, JavaEeClasses.RESOURCE.allFqns)
                    return@run annotations
                },
                UastModificationTracker.getInstance(module.project)
            )
        }
    }

    fun getAllComponentScanBeans(module: Module): Set<PsiClass> {
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result(
                searchBeanPsiClassesByAnnotation(module, SpringCoreClasses.COMPONENT_SCAN),
                UastModificationTracker.getInstance(module.project)
            )
        }
    }


}