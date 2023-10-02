package com.esprito.spring.core.service

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.JavaEeClasses
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.util.SpringCoreUtil
import com.esprito.spring.core.util.SpringCoreUtil.equalsByReturnType
import com.esprito.spring.core.util.SpringCoreUtil.getQualifierAnnotation
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanName
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.esprito.util.EspritoPsiUtil.isEqualOrInheritor
import com.esprito.util.EspritoPsiUtil.isGeneric
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.resolvedPsiClass
import com.esprito.util.EspritoPsiUtil.returnPsiClass
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.uast.UastModificationTracker

@Service(Service.Level.PROJECT)
class SpringSearchService(private val project: Project) {

    private val cachedValuesManager: CachedValuesManager = CachedValuesManager.getManager(project)

    private fun searchAllBeanClasses(module: Module): Set<PsiBean> {
        val allPsiClassesAnnotatedByComponent = getBeanPsiClassesAnnotatedByComponent(module)
        val methodsAnnotatedByBeanReturnTypes = searchComponentPsiClassesByBeanMethods(module)
        return allPsiClassesAnnotatedByComponent + methodsAnnotatedByBeanReturnTypes
    }

    fun getAllBeansClasses(module: Module): Set<PsiBean> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                searchAllBeanClasses(module),
                UastModificationTracker.getInstance(project)
            )
        }
    }

    fun getAllBeanByNames(module: Module): Map<String, List<PsiBean>> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                searchAllBeanClasses(module)
                    .groupBy { it.name },
                UastModificationTracker.getInstance(project)
            )
        }
    }

    fun getAllBeansClassesWithAncestors(module: Module): Set<PsiClass> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                getAllBeansClasses(module).asSequence().flatMap { it.psiClass.supers.asSequence() + it.psiClass }.toSet(),
                UastModificationTracker.getInstance(project)
            )
        }
    }


    fun getBeanPsiClassesAnnotatedByComponent(module: Module): Set<PsiBean> {
        return cachedValuesManager.getCachedValue(module) {
            val annotationPsiClasses = getComponentClassAnnotations(module)
            CachedValueProvider.Result(
                searchBeanPsiClassesByAnnotations(module, annotationPsiClasses),
                UastModificationTracker.getInstance(project)
            )
        }
    }

    fun getComponentBeanPsiMethods(module: Module): Set<PsiMethod> {
        return cachedValuesManager.getCachedValue(module) {
            val psiMethods = getBeanPsiClassesAnnotatedByComponent(module)
                .asSequence()
                .map { it.psiClass }
                .filter { it.isValid }
                .flatMap { it.allMethods.asSequence() }
                .filter { it.isMetaAnnotatedBy(SpringCoreClasses.BEAN) }
                .toSet()
            CachedValueProvider.Result(
                psiMethods,
                UastModificationTracker.getInstance(project)
            )
        }
    }


    private fun searchComponentPsiClassesByBeanMethods(module: Module): Set<PsiBean> {
        return getComponentBeanPsiMethods(module)
            .asSequence()
            .mapNotNull { method -> method.returnType?.resolvedPsiClass
                ?.let { PsiBean(method.resolveBeanName, it, method.getQualifierAnnotation()) }
            }
            .filter { SpringCoreUtil.isSpringBeanCandidateClass(it.psiClass) }
            .toSet()
    }

    private fun searchBeanPsiClassesByAnnotations(module: Module, annotationPsiClasses: Collection<PsiClass>): Set<PsiBean> {
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        return annotationPsiClasses.asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, scope) }
            .filter { SpringCoreUtil.isSpringBeanCandidateClass(it) }
            .map { PsiBean(it.resolveBeanName(module), it, it.getQualifierAnnotation()) }
            .toSet()
    }


    private fun searchBeanPsiClassesByAnnotation(module: Module, annotationName: String): Set<PsiClass> {
        val annotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(module, annotationName, false)
        return searchBeanPsiClassesByAnnotations(module, annotations).map { it.psiClass }.toSet()
    }


    fun getComponentClassAnnotations(module: Module): Collection<PsiClass> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                run {
                    val annotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(module, SpringCoreClasses.COMPONENT, false).toMutableSet()
                    annotations += LibraryClassCache.searchForLibraryClasses(module, JavaEeClasses.RESOURCE.allFqns)
                    return@run annotations
                }.toSet(),
                UastModificationTracker.getInstance(project)
            )
        }
    }

    fun getAllReferencesToElement(element: PsiElement): Set<PsiReference> {
        return cachedValuesManager.getCachedValue(element) {
            CachedValueProvider.Result(
                ReferencesSearch.search(element).toSet(),
                UastModificationTracker.getInstance(project)
            )
        }
    }

    fun getAllComponentScanBeans(module: Module, annotation: String): Set<PsiClass> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                searchBeanPsiClassesByAnnotation(module, annotation),
                UastModificationTracker.getInstance(project)
            )
        }
    }

    fun getAutowiredFieldAnnotations(module: Module): Collection<PsiClass> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                run {
                    val annotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(module, SpringCoreClasses.AUTOWIRED, false).toMutableSet()
                    annotations += LibraryClassCache.searchForLibraryClasses(module, JavaEeClasses.INJECT.allFqns + JavaEeClasses.RESOURCE.allFqns)
                    return@run annotations
                },
                UastModificationTracker.getInstance(module.project)
            )
        }
    }

    fun findBeanDeclarations(module: Module, byBeanName: String, forcedBeanName: String? = byBeanName, byBeanPsiType: PsiType? = null, qualifier: PsiAnnotation? = null): List<PsiElement> {
        val byBeanPsiClass = byBeanPsiType?.resolveBeanPsiClass
        val componentPsiBeans = getBeanPsiClassesAnnotatedByComponent(module)
        val methodsPsiBeans = getComponentBeanPsiMethods(module)

        val byNameBeanMethods = methodsPsiBeans.filter { it.resolveBeanName == byBeanName }
        val byNameComponents = componentPsiBeans.filter { it.name == byBeanName }.map { it.psiClass }
        val resultByName = (byNameBeanMethods + byNameComponents).filter {
            if (byBeanPsiClass == null) {
                return@filter true
            }
            val targetClass = when (it) {
                is PsiMethod -> it.returnType?.resolvedPsiClass
                is PsiClass -> it
                else -> null
            } ?: return@filter true

            return@filter targetClass.isEqualOrInheritor(byBeanPsiClass)
        }
        if (forcedBeanName != null || resultByName.size == 1) {
            return resultByName
        }

        if (byBeanPsiClass == null) {
            return resultByName
        }

        val byTypeBeanMethods = getBeansPsiMethods(byBeanPsiType, methodsPsiBeans, byBeanPsiClass)

        val byTypeComponents = componentPsiBeans
            .filter { it.psiClass.isEqualOrInheritor(byBeanPsiClass) }.map { it.psiClass }
            .filter { !it.isGeneric(byBeanPsiType) || (it.isGeneric(byBeanPsiType) && byTypeBeanMethods.isEmpty()) }
        val resultByType = byTypeBeanMethods + byTypeComponents

        val resultList = if (resultByName.isNotEmpty()) {
            resultByName.filter { it in resultByType }
        } else {
            resultByType
        }
        // TODO:
        //resultList.any { it.isA}

        val byPrimary = resultList.filter { it.isMetaAnnotatedBy(SpringCoreClasses.PRIMARY) }
        return byPrimary.takeIf { it.isNotEmpty() } ?: resultList
    }

    fun searchClassInheritors(psiClass: PsiClass): Set<PsiClass> {
        return cachedValuesManager.getCachedValue(psiClass)
        {
            CachedValueProvider.Result(
                ClassInheritorsSearch.search(psiClass).findAll()
                    .asSequence()
                    .filterNotNull()
                    .filter { it.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT) }
                    .toSet(),
                UastModificationTracker.getInstance(project)
            )
        }
    }

    fun getBeansPsiMethods(psiType: PsiType, allBeansPsiMethods: Set<PsiMethod>, resolvedPsiBeanClass: PsiClass): List<PsiMethod> {
        return if (psiType.equalsByReturnType()) {
            allBeansPsiMethods.filter { it.returnType == psiType }
        } else {
            var methods =
                allBeansPsiMethods.filter { it.returnPsiClass?.isEqualOrInheritor(resolvedPsiBeanClass) == true }
            val resolvedPsiClass = psiType.resolvedPsiClass
            if (resolvedPsiClass != null && methods.isEmpty() && searchClassInheritors(resolvedPsiBeanClass).isEmpty()) {
                methods = getInheritorMethods(allBeansPsiMethods, psiType, resolvedPsiClass)
            }
            methods
        }
    }

    private fun getInheritorMethods(methodsPsiBeans: Set<PsiMethod>, psiType: PsiType, resolvedPsiClass: PsiClass): List<PsiMethod> {
        return if (psiType is PsiClassType) {
            methodsPsiBeans.filter {
                it.returnPsiClass?.isEqualOrInheritor(resolvedPsiClass) == true &&
                        (it.returnType as? PsiClassType)?.parameters.contentEquals(psiType.parameters)
            }
        } else emptyList()
    }

    companion object {
        fun getInstance(project: Project): SpringSearchService = project.service()
    }


}