package com.esprito.spring.core.service

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.JavaEeClasses
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.esprito.spring.core.service.conditional.*
import com.esprito.spring.core.util.SpringCoreUtil
import com.esprito.spring.core.util.SpringCoreUtil.canBeMoreThanOneBean
import com.esprito.spring.core.util.SpringCoreUtil.getQualifierAnnotation
import com.esprito.spring.core.util.SpringCoreUtil.isEligibleBeanMethodType
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanName
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.esprito.spring.core.util.SpringCoreUtil.targetClass
import com.esprito.util.CacheKeyStore
import com.esprito.util.EspritoAnnotationUtil
import com.esprito.util.EspritoAnnotationUtil.getLongValue
import com.esprito.util.EspritoPsiUtil.findChildrenOfType
import com.esprito.util.EspritoPsiUtil.getMetaAnnotation
import com.esprito.util.EspritoPsiUtil.isEqualOrInheritor
import com.esprito.util.EspritoPsiUtil.isGeneric
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.resolvedPsiClass
import com.esprito.util.EspritoPsiUtil.returnPsiClass
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.childrenOfType
import com.intellij.uast.UastModificationTracker

@Service(Service.Level.PROJECT)
class SpringSearchService(private val project: Project) {

    private val cachedValuesManager: CachedValuesManager = CachedValuesManager.getManager(project)

    private fun searchAllBeanClasses(module: Module): Set<PsiBean> {
        val allPsiClassesAnnotatedByComponent = getBeanPsiClassesAnnotatedByComponent(module)
        val methodsAnnotatedByBeanReturnTypes = searchComponentPsiClassesByBeanMethods(module)
        return allPsiClassesAnnotatedByComponent +
                methodsAnnotatedByBeanReturnTypes
    }

    private fun getAllBeansClasses(module: Module): FoundBeans {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                filterByConditionals(
                    module,
                    searchAllBeanClasses(module)
                ),
                UastModificationTracker.getInstance(project)
            )
        }
    }

    fun getExcludedBeansClasses(module: Module): Set<PsiBean> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                getAllBeansClasses(module)
                    .excluded,
                UastModificationTracker.getInstance(project)
            )
        }
    }

    fun getActiveBeansClasses(module: Module): Set<PsiBean> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                getAllBeansClasses(module)
                    .active,
                UastModificationTracker.getInstance(project)
            )
        }
    }

    fun getAllBeanByNames(module: Module): Map<String, List<PsiBean>> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                getActiveBeansClasses(module)
                    .groupBy { it.name },
                UastModificationTracker.getInstance(project)
            )
        }
    }

    fun getAllBeansClassesWithAncestors(module: Module): Set<PsiClass> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                getActiveBeansClasses(module).asSequence().flatMap { it.psiClass.supers.asSequence() + it.psiClass }.toSet(),
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

    fun searchReferenceByMethod(module: Module, method: PsiMethod): Collection<PsiReference> {
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        return MethodReferencesSearch.search(method, scope, true).findAll()
    }

    private fun searchComponentPsiClassesByBeanMethods(module: Module): Set<PsiBean> {
        return getComponentBeanPsiMethods(module)
            .asSequence()
            .flatMap { method -> method.returnType?.resolvedPsiClass
                ?.let { psiClass ->
                    method.resolveBeanName.asSequence()
                        .map { PsiBean(it, psiClass, method.getQualifierAnnotation(), method) }
                } ?: emptySequence()
            }
            .filter { SpringCoreUtil.isSpringBeanCandidateClass(it.psiClass) }
            .toSet()
    }

    private fun searchBeanPsiClassesByAnnotations(module: Module, annotationPsiClasses: Collection<PsiClass>): Set<PsiBean> {
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        return annotationPsiClasses.asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, scope) }
            .filter { SpringCoreUtil.isSpringBeanCandidateClass(it) }
            .map { PsiBean(it.resolveBeanName(module), it, it.getQualifierAnnotation(), it) }
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

    fun findClassesByQualifiedName(module: Module, qualifiedName: String): Collection<PsiClass> {
        val key = CacheKeyStore.getInstance(project).getKey<Collection<PsiClass>>(
            "SpringClassesLoaderByQualifiedNameCache(${qualifiedName})"
        )
        return cachedValuesManager.getCachedValue(module, key, {
            CachedValueProvider.Result(
                LibraryClassCache.searchForLibraryClasses(module, listOf(qualifiedName)),
                UastModificationTracker.getInstance(project)
            )
        }, false)
    }

    private fun PsiMember.targetBeanTypeIsSuitable(byBeanPsiClass: PsiClass?): Boolean {
        if (byBeanPsiClass == null) {
            return true
        }

        return targetClass?.isEqualOrInheritor(byBeanPsiClass) ?: true
    }

    private fun findBeanDeclarations(
        module: Module,
        byBeanName: String,
        byBeanPsiType: PsiType?,
        qualifier: PsiAnnotation?
    ): List<PsiMember> {
        val byBeanPsiClass = byBeanPsiType?.resolveBeanPsiClass
        var isMultipleBean = byBeanPsiType?.canBeMoreThanOneBean(listOf(byBeanPsiClass))
        val componentPsiBeans = getBeanPsiClassesAnnotatedByComponent(module)
        val methodsPsiBeans = getComponentBeanPsiMethods(module)
        val beanNameFromQualifier = qualifier?.resolveBeanName()

        val resultByType: List<PsiMember> = if (byBeanPsiClass != null) {
            val byTypeBeanMethods = getBeansPsiMethods(byBeanPsiType, methodsPsiBeans, byBeanPsiClass)

            // Check if multiple bean has exact match
            if (isMultipleBean == true && byTypeBeanMethods.isNotEmpty()) {
                isMultipleBean = false
            }

            val byTypeComponents = componentPsiBeans.asSequence()
                .filter { it.psiClass.isEqualOrInheritor(byBeanPsiClass) }
                .map { it.psiClass }
                .filter { !it.isGeneric(byBeanPsiType) || byTypeBeanMethods.isEmpty() }
                .toSet()
            val aResultByTypeAndQualifier: List<PsiMember> = (byTypeBeanMethods + byTypeComponents)
                .filter { qualifier == null || beanNameFromQualifier != null && beanNameFromQualifier in it.resolveBeanName(module) || EspritoAnnotationUtil.equal(qualifier, it.getAnnotation(qualifier.qualifiedName!!)) }

            if (aResultByTypeAndQualifier.isEmpty()) {
                return emptyList()
            }

            if (aResultByTypeAndQualifier.size > 1) {
                val byPrimary = aResultByTypeAndQualifier.filter { it.isMetaAnnotatedBy(SpringCoreClasses.PRIMARY) }
                if (byPrimary.isNotEmpty()) {
                    return byPrimary
                }
                val byPriority = aResultByTypeAndQualifier
                    .filter { it.isMetaAnnotatedBy(JavaEeClasses.PRIORITY.allFqns) }
                    .groupBy { it.getMetaAnnotation(JavaEeClasses.PRIORITY.allFqns)?.getLongValue()?.toInt() ?: Int.MAX_VALUE }
                if (byPriority.isNotEmpty()) {
                    val highestPriority = byPriority.minOf { it.key }
                    return byPriority[highestPriority] ?: emptyList()
                }
            }
            aResultByTypeAndQualifier
        } else {
            (methodsPsiBeans + (componentPsiBeans.map { it.psiClass }.toSet()))
                .filter { qualifier == null || beanNameFromQualifier != null && beanNameFromQualifier in it.resolveBeanName(module) || EspritoAnnotationUtil.equal(qualifier, it.getAnnotation(qualifier.qualifiedName!!)) }
                .toList()
        }

        if (resultByType.size == 1) {
            return resultByType
        }

        if (qualifier == null) {
            if (byBeanPsiClass != null && isMultipleBean == true) {
                return resultByType
            }

            val filteredByBeanName = resultByType
                .filter { byBeanName in it.resolveBeanName(module) }
            return filteredByBeanName.ifEmpty { resultByType }
        }
        val byNameFromQualifierBean = resultByType
            .filter { beanNameFromQualifier != null && beanNameFromQualifier in it.resolveBeanName(module) || EspritoAnnotationUtil.equal(qualifier, it.getAnnotation(qualifier.qualifiedName!!)) }
        return byNameFromQualifierBean
            .filter { byBeanName in it.resolveBeanName(module) }
            .ifEmpty { byNameFromQualifierBean }
    }

    fun findActiveBeanDeclarations(
        module: Module,
        byBeanName: String,
        byBeanPsiType: PsiType? = null,
        qualifier: PsiAnnotation? = null
    ): List<PsiMember> = runReadAction {
        val beanDeclarations = findBeanDeclarations(module, byBeanName, byBeanPsiType, qualifier)
        val excludedElements = getExcludedBeansClasses(module).map { it.psiMember }.toSet()

        beanDeclarations - excludedElements
    }

    fun searchClassInheritors(psiClass: PsiClass): Set<PsiClass> {
        return cachedValuesManager.getCachedValue(psiClass) {
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

    fun getBeansPsiMethods(psiType: PsiType, allBeansPsiMethods: Set<PsiMethod>, resolvedPsiBeanClass: PsiClass): Collection<PsiMethod> {
        return if (psiType.isEligibleBeanMethodType()) {
            allBeansPsiMethods
                .filter { it.returnType == psiType }
        } else {
            var methods = allBeansPsiMethods
                .filter { it.returnPsiClass?.isEqualOrInheritor(resolvedPsiBeanClass) == true }
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

    private fun filterByConditionals(module: Module, foundBeans: Set<PsiBean>): FoundBeans {
        val active = foundBeans.toMutableSet()
        val excluded = mutableSetOf<PsiBean>()

        val autoConfigurationsFqn =
            SpringConfigurationPropertiesSearch.getInstance(module.project)
                .getAllFactoriesNames(module)
        if (autoConfigurationsFqn.isEmpty()) return FoundBeans(active, excluded)

        val exclusionStrategies = listOf(
            ConditionalOnBeanStrategy(module),
            ConditionalOnMissingBeanStrategy(module),
            ConditionalOnClassStrategy(module),
            ConditionalOnMissingClassStrategy(module)
        )

        val dependants = foundBeans.asSequence()
            .filter { it.psiMember is PsiClass }
            .filter { autoConfigurationsFqn.contains(it.psiClass.qualifiedName) }
            .map { it.psiClass }
            .toList()


        var changed: Boolean
        do {
            changed = false
            findToExclude(dependants, active, exclusionStrategies)
                .forEach { psiMember ->
                    val beansToExclude = active.filter { psiMember == it.psiMember }.toSet()
                    if (beansToExclude.isNotEmpty()) {
                        changed = true
                        excluded.addAll(beansToExclude)
                        active.removeAll(beansToExclude)
                    }
                }
        } while (changed)

        return FoundBeans(active, excluded)
    }

    data class FoundBeans(val active: Set<PsiBean>, val excluded: Set<PsiBean>)

    private fun findToExclude(
        dependants: List<PsiMember>,
        foundBeans: Set<PsiBean>,
        exclusionStrategies: List<ExclusionStrategy>
    ): List<PsiMember> {
        val toExclude = mutableListOf<PsiMember>()

        for (dependant in dependants) {
            val dependantWithAllNestedMembers = (dependant.findChildrenOfType<PsiMember>() + dependant).toSet()
            val otherBeans = foundBeans.filter { !dependantWithAllNestedMembers.contains(it.psiMember) }

            if (exclusionStrategies.any { it.shouldExclude(dependant, otherBeans) }) {
                toExclude.addAll(dependantWithAllNestedMembers)
            } else {
                val nestedDependant = dependant.childrenOfType<PsiMember>()
                toExclude.addAll(findToExclude(nestedDependant, foundBeans, exclusionStrategies))
            }
        }

        return toExclude
    }

    fun getMetaAnnotations(module: Module, annotationFqn: String): MetaAnnotationsHolder {
        val key = CacheKeyStore.getInstance(module.project).getKey<MetaAnnotationsHolder>(annotationFqn)

        return cachedValuesManager.getCachedValue(module, key, {
            CachedValueProvider.Result(
                MetaAnnotationsHolder.of(module, annotationFqn),
                UastModificationTracker.getInstance(module.project)
            )
        }, false)
    }

    companion object {
        fun getInstance(project: Project): SpringSearchService = project.service()
    }

}