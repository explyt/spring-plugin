/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.service

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.JavaEeClasses
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.core.service.SpringSearchService.Companion.getInstance
import com.explyt.spring.core.service.beans.discoverer.AdditionalBeansDiscoverer
import com.explyt.spring.core.service.conditional.*
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.GlobalSearchScopeTestAware
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.spring.core.util.SpringCoreUtil.beanPsiType
import com.explyt.spring.core.util.SpringCoreUtil.beanPsiTypeKotlin
import com.explyt.spring.core.util.SpringCoreUtil.filterByBeanPsiType
import com.explyt.spring.core.util.SpringCoreUtil.filterByExactMatch
import com.explyt.spring.core.util.SpringCoreUtil.filterByQualifier
import com.explyt.spring.core.util.SpringCoreUtil.getQualifierAnnotation
import com.explyt.spring.core.util.SpringCoreUtil.isComponentCandidate
import com.explyt.spring.core.util.SpringCoreUtil.matchesWildcardType
import com.explyt.spring.core.util.SpringCoreUtil.possibleMultipleBean
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanName
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.explyt.util.CacheKeyStore
import com.explyt.util.ExplytAnnotationUtil.getLongValue
import com.explyt.util.ExplytKotlinUtil.mapToSet
import com.explyt.util.ExplytPsiUtil.getMetaAnnotation
import com.explyt.util.ExplytPsiUtil.isEqualOrInheritor
import com.explyt.util.ExplytPsiUtil.isGeneric
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isNonStatic
import com.explyt.util.ExplytPsiUtil.isPublic
import com.explyt.util.ExplytPsiUtil.resolvedDeepPsiClass
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.explyt.util.ExplytPsiUtil.returnPsiClass
import com.explyt.util.ModuleUtil
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.lang.Language
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.serviceContainer.AlreadyDisposedException
import com.intellij.uast.UastModificationTracker
import com.jetbrains.rd.util.getOrCreate
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.*
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class SpringSearchService(private val project: Project) {

    private val cachedValuesManager: CachedValuesManager = CachedValuesManager.getManager(project)
    private val mutexSearchBeans = ConcurrentHashMap<String, String>()
    private val mutexConditionalOn = ConcurrentHashMap<String, String>()

    private fun searchAllBeanClasses(module: Module): Set<PsiBean> {
        try {
            val allPsiClassesAnnotatedByComponent = getBeanPsiClassesAnnotatedByComponent(module)
            val methodsAnnotatedByBeanReturnTypes = searchComponentPsiClassesByBeanMethods(module)
            val staticBeans = getStaticBeans(module)
            return allPsiClassesAnnotatedByComponent + methodsAnnotatedByBeanReturnTypes + staticBeans
        } catch (e: AlreadyDisposedException) {
            return emptySet()
        }
    }

    private fun getStaticBeans(module: Module): Set<PsiBean> {
        synchronized(getMutexString(MutexType.SEARCH, module)) {
            return cachedValuesManager.getCachedValue(module) {
                CachedValueProvider.Result(
                    AdditionalBeansDiscoverer.EP_NAME.getExtensions(project).asSequence()
                        .filter { it.accepts(module) }
                        .flatMapTo(mutableSetOf()) { it.discoverBeans(module) },
                    ModificationTrackerManager.getInstance(project).getLibraryTracker()
                )
            }
        }
    }

    @Deprecated("Don use directly. Use SpringSearchServiceFacade")
    fun getAllBeansClassesConsideringContext(project: Project): FoundBeans {
        return cachedValuesManager.getCachedValue(project) {
            CachedValueProvider.Result(
                doGetAllBeansClassesConsideringContext(project),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun searchAnnotatedClasses(annotation: PsiClass, module: Module): List<PsiClass> {
        return AnnotatedElementsSearch.searchPsiClasses(annotation, module.moduleWithDependenciesScope)
            .filter { !it.isAnnotationType }
    }

    private fun doGetAllBeansClassesConsideringContext(project: Project): FoundBeans {
        val active = mutableSetOf<PsiBean>()
        val excluded = mutableSetOf<PsiBean>()
        for (module in project.modules) {
            val (foundActive, foundExcluded) = getAllBeansClasses(module)
            excluded += foundExcluded
            for (psiBean in foundActive) {
                val uBeanMember = psiBean.psiMember.toUElement()
                if (uBeanMember == null) {
                    active += psiBean
                    continue
                }
                if (isInSpringContext(uBeanMember, module)) {
                    active += psiBean
                } else {
                    excluded += psiBean
                }
            }
        }

        return FoundBeans(active, excluded)
    }

    fun getProjectBeans(): Set<PsiBean> {
        return cachedValuesManager.getCachedValue(project) {
            CachedValueProvider.Result(
                project.modules
                    .filter { SpringCoreUtil.isSpringModule(it) }
                    .flatMapTo(mutableSetOf()) { getProjectBeanPsiClassesAnnotatedByComponent(it) },
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun getAllActiveBeans(): Set<PsiBean> {
        return project.modules
            .filter { SpringCoreUtil.isSpringModule(it) }
            .flatMapTo(mutableSetOf()) { searchAllBeanLight(it) }
    }

    private fun getAllBeansClasses(module: Module): FoundBeans {
        synchronized(getMutexString(MutexType.CONDITIONAL_ON, module)) {
            return cachedValuesManager.getCachedValue(module) {
                CachedValueProvider.Result(
                    filterByConditionals(module, searchAllBeanLight(module)),
                    ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
                )
            }
        }
    }

    @Deprecated("Used only in SpringBeanLineMarkerProvider. Use SpringSearchServiceFacade")
    fun searchAllBeanLight(module: Module): Set<PsiBean> {
        synchronized(getMutexString(MutexType.SEARCH, module)) {
            return cachedValuesManager.getCachedValue(module) {
                CachedValueProvider.Result(
                    searchAllBeanClasses(module),
                    ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
                )
            }
        }
    }

    @Deprecated("Don use directly. Use SpringSearchServiceFacade")
    fun getExcludedBeansClasses(module: Module): Set<PsiBean> {
        return getAllBeansClasses(module).excluded
    }

    @Deprecated("Don use directly. Use SpringSearchServiceFacade")
    fun getActiveBeansClasses(module: Module): Set<PsiBean> {
        return getAllBeansClasses(module).active
    }

    @Deprecated("Don use directly. Use SpringSearchServiceFacade")
    fun getAllBeanByNames(module: Module): Map<String, List<PsiBean>> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                getActiveBeansClasses(module).groupBy { it.name },
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    @Deprecated("Don use directly. Use SpringSearchServiceFacade")
    fun getAllBeanByNamesLight(module: Module): Map<String, List<PsiBean>> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                searchAllBeanLight(module).groupBy { it.name },
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    @VisibleForTesting
    fun getAllActiveBeans(module: Module): Set<PsiClass> {
        synchronized(getMutexString(MutexType.CONDITIONAL_ON, module)) {
            return cachedValuesManager.getCachedValue(module) {
                CachedValueProvider.Result(
                    SpringSearchUtils.getPsiClasses(getActiveBeansClasses(module)),
                    ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
                )
            }
        }
    }

    @Deprecated("Don use directly. Use SpringSearchServiceFacade")
    fun getAllActiveBeansLight(module: Module): Set<PsiClass> {
        synchronized(getMutexString(MutexType.SEARCH, module)) {
            return cachedValuesManager.getCachedValue(module) {
                CachedValueProvider.Result(
                    searchAllBeanLight(module).mapTo(mutableSetOf()) { it.psiClass },
                    ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
                )
            }
        }
    }

    @Deprecated("Don use directly. Use SpringSearchServiceFacade")
    fun isInSpringContext(uBeanElement: UElement, module: Module): Boolean {
        try {
            if (uBeanElement is UMethod && uBeanElement.returnType is PsiArrayType) {
                val deepArrayPsiClass = uBeanElement.returnType?.resolvedDeepPsiClass ?: return false
                return searchArrayComponentPsiClassesByBeanMethods(module)
                    .map { it.psiClass }.contains(deepArrayPsiClass)
            }
            return SpringSearchUtils.getBeanClass(uBeanElement) in getAllActiveBeans(module)
        } catch (e: AlreadyDisposedException) {
            return false
        }
    }

    @Deprecated("Don use directly. Use SpringSearchServiceFacade")
    fun getBeanPsiClassesAnnotatedByComponent(module: Module): Set<PsiBean> {
        try {
            synchronized(getMutexString(MutexType.SEARCH, module)) {
                return cachedValuesManager.getCachedValue(module) {
                    val scope = GlobalSearchScope.moduleWithDependenciesScope(module)
                    val annotationPsiClasses = SpringSearchUtils.getComponentClassAnnotations(module)
                    val modulePackagesHolder = PackageScanService.getInstance(module.project).getAllPackages()
                    val allModuleWithDependenciesBeans = filterBeansByPackage(
                        searchBeanPsiClassesByAnnotations(module, annotationPsiClasses, scope),
                        modulePackagesHolder, module
                    )
                    val extraComponents = getExtraComponents(module, modulePackagesHolder)
                    val moduleWithDependenciesBeans = allModuleWithDependenciesBeans + extraComponents
                    val moduleLibraryBeans = searchBeanPsiClassesByComponentAnnotationLibraryScopeCached(module)
                    val importedPsiBeans = getImportedBeans(modulePackagesHolder, module)
                    val configurationProperties = searchConfigurationPropertiesBean(module, scope)

                    CachedValueProvider.Result(
                        moduleWithDependenciesBeans + moduleLibraryBeans + importedPsiBeans + configurationProperties,
                        ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
                    )
                }
            }
        } catch (e: AlreadyDisposedException) {
            return emptySet()
        }
    }

    private fun isAnnotated(it: PsiBean, annotationType: String): Boolean {
        return if (it.psiClass === it.psiMember) {
            it.psiClass.isMetaAnnotatedBy(annotationType)
        } else {
            it.psiClass.isMetaAnnotatedBy(annotationType) || it.psiMember.isMetaAnnotatedBy(annotationType)
        }
    }

    @Deprecated("Don use directly. Use SpringSearchServiceFacade")
    fun getDependentBeanPsiClassesAnnotatedByComponent(module: Module): Set<PsiBean> {
        try {
            return cachedValuesManager.getCachedValue(module) {
                val scope = GlobalSearchScope.moduleWithDependentsScope(module)
                val annotationPsiClasses = SpringSearchUtils.getComponentClassAnnotations(module)
                val modulePackagesHolder = PackageScanService.getInstance(module.project).getAllPackages()
                val dependentBeans = filterBeansByPackage(
                    searchBeanPsiClassesByAnnotations(module, annotationPsiClasses, scope),
                    modulePackagesHolder, module
                )

                CachedValueProvider.Result(
                    filterByConditionals(module, dependentBeans).active,
                    ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
                )
            }
        } catch (e: AlreadyDisposedException) {
            return emptySet()
        }
    }

    private fun getImportedBeans(modulePackagesHolder: RootDataHolder, module: Module) =
        modulePackagesHolder.importQualified.asSequence()
            .mapNotNull { JavaPsiFacade.getInstance(project).findClass(it, module.moduleWithDependenciesScope) }
            .map { PsiBean(it.resolveBeanName(module), it, it.getQualifierAnnotation(), it) }
            .toSet()

    private fun filterBeansByPackage(
        beanComponents: Set<PsiBean>, rootDataHolder: RootDataHolder, module: Module
    ): Set<PsiBean> {
        if (rootDataHolder.isEmpty()) return beanComponents
        val packages = rootDataHolder.getPackages(module)
        if (packages.isEmpty()) return emptySet()
        return beanComponents.filterTo(mutableSetOf()) { checkBeanByPackage(packages, rootDataHolder, it) }
    }

    private fun checkBeanByPackage(packages: Set<String>, rootDataHolder: RootDataHolder, bean: PsiBean): Boolean {
        if (bean.psiClass.qualifiedName?.let { rootDataHolder.isRootComponent(it) } == true) {
            return true
        }
        val qualifiedName = bean.psiClass.qualifiedName ?: return false
        return packages.any { qualifiedName.startsWith(it) }
    }

    @Deprecated("Don use directly. Use SpringSearchServiceFacade")
    fun getComponentBeanPsiMethods(module: Module): Set<PsiMethod> {
        return cachedValuesManager.getCachedValue(module) {
            val psiMethods = getBeanPsiClassesAnnotatedByComponent(module)
                .asSequence()
                .map { it.psiClass }
                .filter { it.isValid }
                .flatMap { it.allMethods.asSequence() }
                .filter { it.isMetaAnnotatedBy(SpringCoreClasses.BEAN) }
                .filter { isActive(it) }
                .toSet()
            CachedValueProvider.Result(
                psiMethods,
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun isActive(psiMember: PsiMember): Boolean {
        if (!psiMember.isMetaAnnotatedBy(SpringCoreClasses.PROFILE)) return true
        val module = ModuleUtilCore.findModuleForPsiElement(psiMember) ?: return false
        val profilesService = ProfilesService.getInstance(project)

        val metaAnnotationsHolder = getMetaAnnotations(module, SpringCoreClasses.PROFILE)
        val values = metaAnnotationsHolder.getAnnotationMemberValues(psiMember, setOf("value"))

        return values.all { value ->
            profilesService.compute(
                ElementManipulators.getValueText(value)
            )
        }
    }

    private fun searchComponentPsiClassesByBeanMethods(module: Module): Set<PsiBean> {
        return getComponentBeanPsiMethods(module)
            .asSequence()
            .flatMap { method ->
                // for array created separated method: searchArrayComponentPsiClassesByBeanMethods
                method.returnType?.resolvedPsiClass?.let { psiClass ->
                    method.resolveBeanName.asSequence()
                        .map { PsiBean(it, psiClass, method.getQualifierAnnotation(), method) }
                } ?: emptySequence()
            }
            .filter { SpringCoreUtil.isSpringBeanCandidateClass(it.psiClass) }
            .toSet()
    }

    private fun searchComponentPsiClassesByBeanMethods(psiBeans: Collection<PsiBean>): Set<PsiBean> {
        return psiBeans.asSequence()
            .map { it.psiClass }
            .filter { it.isValid }
            .flatMap { it.allMethods.asSequence() }
            .filter { it.isMetaAnnotatedBy(SpringCoreClasses.BEAN) }
            .filter { isActive(it) }
            .asSequence()
            .flatMap { method ->
                // for array created separated method: searchArrayComponentPsiClassesByBeanMethods
                method.returnType?.resolvedPsiClass?.let { psiClass ->
                    method.resolveBeanName.asSequence()
                        .map { PsiBean(it, psiClass, method.getQualifierAnnotation(), method) }
                } ?: emptySequence()
            }
            .filter { SpringCoreUtil.isSpringBeanCandidateClass(it.psiClass) }
            .toSet()
    }

    fun searchArrayComponentPsiClassesByBeanMethods(module: Module): Set<PsiBean> {
        return getComponentBeanPsiMethods(module)
            .asSequence()
            .filter { it.returnType is PsiArrayType }
            .flatMap { method ->
                method.returnType?.resolvedDeepPsiClass?.let { psiClass ->
                    method.resolveBeanName.asSequence()
                        .map { PsiBean(it, psiClass, method.getQualifierAnnotation(), method) }
                } ?: emptySequence()
            }
            .filter { SpringCoreUtil.isSpringBeanCandidateClass(it.psiClass) }
            .toSet()
    }

    private fun searchBeanPsiClassesByAnnotations(
        module: Module, annotationPsiClasses: Collection<PsiClass>, scope: SearchScope
    ): Set<PsiBean> {
        return annotationPsiClasses.asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, scope) }
            .filter { SpringCoreUtil.isSpringBeanCandidateClass(it) }
            .filter { isActive(it) }
            .map { PsiBean(it.resolveBeanName(module), it, it.getQualifierAnnotation(), it) }
            .toSet()
    }

    private fun searchConfigurationPropertiesBean(module: Module, scope: SearchScope): Set<PsiBean> {
        val configurationPropertiesClass = LibraryClassCache
            .searchForLibraryClass(module, SpringCoreClasses.CONFIGURATION_PROPERTIES) ?: return emptySet()
        return AnnotatedElementsSearch.searchPsiClasses(configurationPropertiesClass, scope).asSequence()
            .filter { isActive(it) }
            .mapToSet { PsiBean(it.resolveBeanName(module), it, it.getQualifierAnnotation(), it) }
    }

    private fun searchBeanPsiClassesByComponentAnnotationLibraryScopeCached(module: Module): Set<PsiBean> {
        return cachedValuesManager.getCachedValue(module) {
            val annotationPsiClasses = SpringSearchUtils.getComponentClassAnnotations(module)
            CachedValueProvider.Result(
                searchBeanPsiClassesByAnnotations(
                    module, annotationPsiClasses, ModuleUtil.getOnlyLibrarySearchScope(module)
                ),
                ModificationTrackerManager.getInstance(project).getLibraryTracker()
            )
        }
    }

    // warning: such methods are not pure!! Do not check twice!
    fun <T> Sequence<T>.isEmpty() = this.firstOrNull() == null
    fun <T> Sequence<T>.isNotEmpty() = !isEmpty()

    fun PsiType.isMultipleBean(module: Module): Boolean {
        if (!possibleMultipleBean()) return false
        val beanPsiType = beanPsiType ?: return true
        val methodsPsiBeans = getComponentBeanPsiMethods(module)

        val byExactMatch = methodsPsiBeans.filterByExactMatch(this)
        if (byExactMatch.isEmpty()) return true

        val byTypeBeanMethods = methodsPsiBeans.filterByBeanPsiType(beanPsiType)
        if (byTypeBeanMethods.isNotEmpty()) return true

        val byTypeComponents = getPsiClassesByComponents(module, this, beanPsiType, true)
        return byTypeComponents.isNotEmpty()
    }

    private fun getPsiClassesByComponents(
        module: Module,
        sourcePsiType: PsiType,
        beanPsiType: PsiType,
        byTypeBeanMethodsIsEmpty: Boolean
    ): Sequence<PsiClass> {
        val beanPsiClass = beanPsiType.resolvedPsiClass
        val componentPsiBeans = getBeanPsiClassesAnnotatedByComponent(module)
        val byTypeComponents = componentPsiBeans.asSequence()
            .map { it.psiClass }
            .filter {
                beanPsiClass != null && it.isEqualOrInheritor(beanPsiClass)
                        || beanPsiType is PsiWildcardType && it.matchesWildcardType(beanPsiType)
            }
            .filter { !it.isGeneric(sourcePsiType) || !byTypeBeanMethodsIsEmpty }
        return byTypeComponents
    }


    private fun findBeanDeclarations(
        module: Module,
        byBeanName: String,
        sourcePsiType: PsiType?,
        qualifier: PsiAnnotation?,
        language: Language,
    ): List<PsiMember> {
        val beanPsiType = if (language == KotlinLanguage.INSTANCE)
            sourcePsiType?.beanPsiTypeKotlin else sourcePsiType?.beanPsiType
        val beanPsiClass = sourcePsiType?.resolveBeanPsiClass
        var isMultipleBean = sourcePsiType?.possibleMultipleBean() ?: false
        val methodsPsiBeans = getComponentBeanPsiMethods(module)
        val beanNameFromQualifier = qualifier?.resolveBeanName()

        val resultByType: List<PsiMember> = if (sourcePsiType != null && beanPsiType != null) {
            val byExactMatch = methodsPsiBeans.filterByExactMatch(sourcePsiType).toSet()
            var byTypeBeanMethods = byExactMatch
            if (isMultipleBean || byExactMatch.isEmpty()) {
                byTypeBeanMethods = methodsPsiBeans.filterByBeanPsiType(beanPsiType).toSet()
            }

            val byTypeComponents = getStaticBeans(module).filter { it.psiClass == beanPsiClass }.map { it.psiClass } +
                    getPsiClassesByComponents(module, sourcePsiType, beanPsiType, byTypeBeanMethods.isEmpty()).toSet()

            if (isMultipleBean && byExactMatch.isNotEmpty()
                && byTypeBeanMethods.isEmpty() && byTypeComponents.isEmpty()
            ) {
                // Check if multiple bean has exact type match
                isMultipleBean = false
                byTypeBeanMethods = byExactMatch
            }

            val aResultByTypeAndQualifier: List<PsiMember> = (byTypeBeanMethods + byTypeComponents)
                .filter { it.filterByQualifier(module, qualifier, beanNameFromQualifier) }

            if (aResultByTypeAndQualifier.isEmpty()) {
                return emptyList()
            }

            if (aResultByTypeAndQualifier.size > 1 && !isMultipleBean) {
                val byPrimary = aResultByTypeAndQualifier.filter { it.isMetaAnnotatedBy(SpringCoreClasses.PRIMARY) }
                if (byPrimary.isNotEmpty()) {
                    return byPrimary
                }
                val byPriority = aResultByTypeAndQualifier.asSequence()
                    .filter { it.isMetaAnnotatedBy(JavaEeClasses.PRIORITY.allFqns) }
                    .groupBy {
                        it.getMetaAnnotation(JavaEeClasses.PRIORITY.allFqns)?.getLongValue()?.toInt() ?: Int.MAX_VALUE
                    }
                if (byPriority.isNotEmpty()) {
                    val highestPriority = byPriority.minOf { it.key }
                    return byPriority[highestPriority] ?: emptyList()
                }
            }
            aResultByTypeAndQualifier
        } else {
            val componentPsiBeans = getBeanPsiClassesAnnotatedByComponent(module)
            (methodsPsiBeans + componentPsiBeans.mapTo(mutableSetOf()) { it.psiClass })
                .filter { it.filterByQualifier(module, qualifier, beanNameFromQualifier) }
        }

        if (resultByType.size == 1) {
            return resultByType
        }

        if (qualifier == null) {
            if (beanPsiClass != null && isMultipleBean) {
                return resultByType
            }

            val filteredByBeanName = resultByType
                .filter { byBeanName in it.resolveBeanName(module) }
            return filteredByBeanName.ifEmpty { resultByType }
        }
        val byNameFromQualifierBean = resultByType
            .filter { it.filterByQualifier(module, qualifier, beanNameFromQualifier) }
        return byNameFromQualifierBean
            .filter { byBeanName in it.resolveBeanName(module) }
            .ifEmpty { byNameFromQualifierBean }
    }

    @Deprecated("Don use directly. Use SpringSearchServiceFacade")
    fun findActiveBeanDeclarations(
        module: Module,
        byBeanName: String,
        language: Language,
        byBeanPsiType: PsiType? = null,
        qualifier: PsiAnnotation? = null
    ): List<PsiMember> = runReadAction {
        try {
            val beanDeclarations = findBeanDeclarations(module, byBeanName, byBeanPsiType, qualifier, language)
            val excludedElements = getExcludedBeansClasses(module).map { it.psiMember }.toSet()

            beanDeclarations - excludedElements
        } catch (e: AlreadyDisposedException) {
            emptyList()
        }
    }

    private fun filterByConditionals(module: Module, foundBeans: Set<PsiBean>): FoundBeans {
        try {
            if (!SpringToolRunConfigurationsSettingsState.getInstance().isBeanFilterEnabled) {
                return FoundBeans(foundBeans, emptySet())
            }
            val active = foundBeans.toMutableSet()
            val excluded = mutableSetOf<PsiBean>()

            val exclusionStrategies = listOf(
                ConditionalOnClassStrategy(module),
                ConditionalOnMissingClassStrategy(module),
                ConditionalOnBeanStrategy(module),
                ConditionalOnMissingBeanStrategy(module),
                ConditionalOnPropertyStrategy(module),
                OnWebApplicationConditionStrategy(module)
            )

            val beansGroupByClass = foundBeans.asSequence()
                .flatMap { getRootPsiClasses(it).map { clazz -> Pair(clazz, it) } }
                .groupBy({ it.first }, { it.second })
            beansGroupByClass.forEach { (key, value) ->
                checkClassToExclude(key, value, active, excluded, exclusionStrategies)
            }
            active.filter { it.psiClass != it.psiMember }
                .forEach { checkClassToExclude(it.psiMember, listOf(it), active, excluded, exclusionStrategies) }

            return FoundBeans(active, excluded)
        } catch (e: AlreadyDisposedException) {
            return FoundBeans(foundBeans, emptySet())
        }
    }

    private fun getRootPsiClasses(it: PsiBean): List<PsiClass> {
        val psiClass = if (it.psiMember !is PsiClass) it.psiMember.containingClass ?: it.psiClass else it.psiClass
        return getRootListPsiClass(psiClass)
    }

    private fun getRootListPsiClass(it: PsiClass): List<PsiClass> {
        val rootClasses = mutableListOf(it)
        var rootClass: PsiClass? = it
        var depthCount = 0
        while (rootClass?.containingClass != null && depthCount < 10) {
            rootClass = rootClass.containingClass
            rootClasses.add(rootClass!!)
            depthCount++
        }
        return rootClasses
    }

    data class FoundBeans(val active: Set<PsiBean>, val excluded: Set<PsiBean>)


    private fun checkClassToExclude(
        psiMember: PsiMember,
        dependantsFromClassBeans: Collection<PsiBean>,
        activeBeans: MutableSet<PsiBean>,
        excludeBeans: MutableSet<PsiBean>,
        exclusionStrategies: List<ExclusionStrategy>
    ) {
        if (dependantsFromClassBeans.all { excludeBeans.contains(it) }) return

        if (exclusionStrategies.any { it.shouldExclude(psiMember, activeBeans) }) {
            excludeBeans.addAll(dependantsFromClassBeans)
            activeBeans.removeAll(dependantsFromClassBeans.toSet())
        }
    }

    //utils!!! - many usages
    fun getMetaAnnotations(module: Module, annotationFqn: String): MetaAnnotationsHolder {
        val key = CacheKeyStore.getInstance(module.project).getKey<MetaAnnotationsHolder>(annotationFqn)

        return cachedValuesManager.getCachedValue(module, key, {
            CachedValueProvider.Result(
                MetaAnnotationsHolder.of(module, annotationFqn),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }, false)
    }

    private fun isBean(uClass: UClass): Boolean {
        if (AnnotationUtil.isAnnotated(uClass.javaPsi, SpringCoreClasses.COMPONENT, AnnotationUtil.CHECK_HIERARCHY)) {
            return true
        }
        val psiElement = uClass.sourcePsi ?: return false
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return false

        return isComponentCandidate(uClass.javaPsi) || getAllActiveBeans(module).contains(uClass.javaPsi)
    }

    @Deprecated("1 usages")
    fun getBeanMethods(uClass: UClass?): MethodsInfoHolder {
        uClass ?: return MethodsInfoHolder(emptySet())
        return CachedValuesManager.getManager(project).getCachedValue(uClass) {
            CachedValueProvider.Result(
                getBeanMethodsInner(uClass), UastModificationTracker.getInstance(project)
            )
        }
    }

    private fun getBeanMethodsInner(uClass: UClass?): MethodsInfoHolder {
        uClass ?: return MethodsInfoHolder(emptySet())
        val isAopClass = AnnotationUtil.isAnnotated(
            uClass.javaPsi, SpringCoreClasses.AOP_ANNOTATION, AnnotationUtil.CHECK_HIERARCHY
        )
        if (isAopClass || isBean(uClass)) {
            val psiMethods = uClass.methods.asSequence()
                .filter { isPublicAopMethod(it, isAopClass) }
                .mapTo(mutableSetOf()) { it.javaPsi }
            return MethodsInfoHolder(psiMethods)
        }
        return MethodsInfoHolder(emptySet())
    }

    private fun isPublicAopMethod(it: UMethod, isAopClass: Boolean): Boolean {
        return if (isAopClass) it.isPublic && it.isNonStatic else
            it.isPublic && it.isNonStatic && AnnotationUtil.isAnnotated(
                it.javaPsi,
                SpringCoreClasses.AOP_ANNOTATION,
                AnnotationUtil.CHECK_HIERARCHY
            )
    }

    private fun getExtraComponents(module: Module, modulePackagesHolder: RootDataHolder): Set<PsiBean> {
        return AdditionalBeansDiscoverer.EP_NAME.getExtensions(project).asSequence()
            .flatMap { getFilteredExtraBeans(it, module, modulePackagesHolder) }
            .filter { isActive(it.psiClass) }
            .toSet()
    }

    private fun getFilteredExtraBeans(
        discoverer: AdditionalBeansDiscoverer, module: Module, rootDataHolder: RootDataHolder
    ): Collection<PsiBean> {
        val extraComponents = discoverer.getExtraComponents(module)
        if (rootDataHolder.isEmpty()) return extraComponents
        return extraComponents.filterTo(mutableSetOf()) { discoverer.additionalFilterBeans(module, it, rootDataHolder) }
    }

    private fun getMutexString(mutexType: MutexType, module: Module): String {
        return when (mutexType) {
            MutexType.SEARCH -> mutexSearchBeans.getOrCreate("search:" + module.name) { "search:" + module.name }
            MutexType.SEARCH_FOR_NATIVE -> mutexSearchBeans
                .getOrCreate("searchNative:" + module.name) { "searchNative:" + module.name }

            MutexType.CONDITIONAL_ON -> mutexConditionalOn
                .getOrCreate("conditionalOn:" + module.name) { "conditionalOn:" + module.name }
        }
    }

    fun getProjectBeanPsiClassesAnnotatedByComponent(module: Module): Set<PsiBean> {
        val scope = GlobalSearchScope.moduleWithDependenciesScope(module)
        val annotationPsiClasses = SpringSearchUtils.getComponentClassAnnotations(module)
        val modulePackagesHolder = PackageScanService.getInstance(module.project).getAllPackages()
        val allModuleWithDependenciesBeans = filterBeansByPackage(
            searchBeanPsiClassesByAnnotations(module, annotationPsiClasses, scope),
            modulePackagesHolder, module
        )
        val extraComponents = getExtraComponents(module, modulePackagesHolder)
        val moduleWithDependenciesBeans = allModuleWithDependenciesBeans + extraComponents
        val importedPsiBeans = getImportedBeans(modulePackagesHolder, module)
        val configurationProperties = searchConfigurationPropertiesBean(module, scope)

        val psiBeans = moduleWithDependenciesBeans + importedPsiBeans + configurationProperties
        val methodsPsiBeans = searchComponentPsiClassesByBeanMethods(psiBeans)

        return psiBeans + methodsPsiBeans
    }

    companion object {
        fun getInstance(project: Project): SpringSearchService = project.service()
    }
}

data class MethodsInfoHolder(val beanPublicAnnotatedMethods: Set<PsiMethod>) {
    val beanPublicAnnotatedMethodNames: Set<String> = beanPublicAnnotatedMethods.map { it.name }.toSet()
}

private enum class MutexType {
    SEARCH, CONDITIONAL_ON, SEARCH_FOR_NATIVE,
}

object SpringSearchUtils {
    fun findUAnnotation(module: Module, uAnnotations: List<UAnnotation>, annotationFqn: String): UAnnotation? {
        if (uAnnotations.isEmpty()) return null

        val metaAnnotationsHolder = getInstance(module.project).getMetaAnnotations(module, annotationFqn)
        for (annotation in uAnnotations) {
            val psiAnnotation = annotation.javaPsi ?: continue
            if (metaAnnotationsHolder.contains(psiAnnotation)) {
                return annotation
            }
        }
        return null
    }

    fun searchClassInheritors(psiClass: PsiClass): Set<PsiClass> {
        val project = psiClass.project
        return CachedValuesManager.getManager(project).getCachedValue(psiClass) {
            CachedValueProvider.Result(
                ClassInheritorsSearch.search(psiClass).findAll().filterNotNullTo(mutableSetOf()),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun getBeansPsiMethodsCheckMultipleBean(
        possibleMultipleBeanPsiType: PsiType,
        allBeansPsiMethods: Set<PsiMethod>,
        beanPsiType: PsiType
    ): Collection<PsiMethod> {
        val isMultipleBean = possibleMultipleBeanPsiType.possibleMultipleBean()

        val byExactMatch = allBeansPsiMethods.filterByExactMatch(possibleMultipleBeanPsiType).toSet()
        val byTypeBeanMethods = allBeansPsiMethods.filterByBeanPsiType(beanPsiType).toSet()

        if (isMultipleBean && byExactMatch.isNotEmpty() && byTypeBeanMethods.isEmpty()) {
            // Check if multiple bean has exact type match
            return byExactMatch
        }
        return byTypeBeanMethods
    }

    fun getBeanClass(uBeanElement: UElement, isArray: Boolean = false): PsiClass? {
        if (isArray) {
            return (uBeanElement as? UMethod)?.returnType?.resolvedDeepPsiClass
        }
        return when (uBeanElement) {
            is UMethod -> uBeanElement.returnPsiClass
            is UClass -> uBeanElement.javaPsi
            else -> null
        }
    }

    fun searchReferenceByMethod(
        module: Module,
        method: PsiMethod,
        scope: SearchScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
    ): Collection<PsiReference> {
        return MethodReferencesSearch.search(method, GlobalSearchScopeTestAware.getScope(module, scope), true)
            .findAll()
    }

    fun getComponentClassAnnotations(module: Module): Collection<PsiClass> {
        val project = module.project
        return CachedValuesManager.getManager(project).getCachedValue(module) {
            CachedValueProvider.Result(
                run {
                    val annotations =
                        MetaAnnotationUtil.getAnnotationTypesWithChildren(module, SpringCoreClasses.COMPONENT, false)
                            .toMutableSet()
                    annotations += LibraryClassCache.searchForLibraryClasses(module, JavaEeClasses.RESOURCE.allFqns)
                    return@run annotations
                }.toSet(),
                ModificationTrackerManager.getInstance(project).getUastAnnotationAndLibraryTracker()
            )
        }
    }

    fun getAllReferencesToElement(element: PsiElement): Set<PsiReference> {
        val project = element.project
        return CachedValuesManager.getManager(project).getCachedValue(element) {
            CachedValueProvider.Result(
                ReferencesSearch.search(element).toSet(),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun getAutowiredFieldAnnotations(module: Module): Collection<PsiClass> {
        val project = module.project
        return CachedValuesManager.getManager(project).getCachedValue(module) {
            val annotations = MetaAnnotationUtil
                .getAnnotationTypesWithChildren(module, SpringCoreClasses.AUTOWIRED, false)
            val annotationsLib = LibraryClassCache.searchForLibraryClasses(
                module, JavaEeClasses.INJECT.allFqns + JavaEeClasses.RESOURCE.allFqns
            )

            CachedValueProvider.Result(
                annotations + annotationsLib, ModificationTrackerManager.getInstance(project).getLibraryTracker()
            )
        }
    }

    fun findAnnotationClassesByQualifiedName(module: Module, qualifiedName: String): Collection<PsiClass> {
        val project = module.project
        val key = CacheKeyStore.getInstance(project).getKey<Collection<PsiClass>>(
            "SpringAnnotationClassesLoaderByQualifiedNameCache(${qualifiedName})"
        )
        return CachedValuesManager.getManager(project).getCachedValue(module, key, {
            CachedValueProvider.Result(
                run {
                    val annotations =
                        MetaAnnotationUtil.getAnnotationTypesWithChildren(module, qualifiedName, false).toMutableSet()
                    annotations += LibraryClassCache.searchForLibraryClasses(module, listOf(qualifiedName))
                    return@run annotations
                },
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }, false)
    }

    fun getPsiClasses(psiBeans: Collection<PsiBean>): MutableSet<PsiClass> {
        val result = HashSet<PsiClass>(psiBeans.size)
        for (bean in psiBeans) {
            result.add(bean.psiClass)
            if (bean.psiMember !is PsiClass) {
                bean.psiMember.containingClass?.let { result.add(it) }
            }
        }
        return result
    }
}