package com.esprito.spring.core.service

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.JavaEeClasses
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.esprito.spring.core.service.conditional.*
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.esprito.spring.core.util.SpringCoreUtil
import com.esprito.spring.core.util.SpringCoreUtil.beanPsiType
import com.esprito.spring.core.util.SpringCoreUtil.filterByInheritedTypes
import com.esprito.spring.core.util.SpringCoreUtil.getQualifierAnnotation
import com.esprito.spring.core.util.SpringCoreUtil.isBounded
import com.esprito.spring.core.util.SpringCoreUtil.isEqualOrInheritorBeanType
import com.esprito.spring.core.util.SpringCoreUtil.matchesWildcardType
import com.esprito.spring.core.util.SpringCoreUtil.possibleMultipleBean
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanName
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.esprito.util.CacheKeyStore
import com.esprito.util.EspritoAnnotationUtil
import com.esprito.util.EspritoAnnotationUtil.getLongValue
import com.esprito.util.EspritoPsiUtil.findChildrenOfType
import com.esprito.util.EspritoPsiUtil.getMetaAnnotation
import com.esprito.util.EspritoPsiUtil.isEqualOrInheritor
import com.esprito.util.EspritoPsiUtil.isGeneric
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.psiClassType
import com.esprito.util.EspritoPsiUtil.resolvedPsiClass
import com.esprito.util.EspritoPsiUtil.returnPsiClass
import com.esprito.util.EspritoPsiUtil.returnPsiType
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
                filterByConditionals(module, searchAllBeanClasses(module)),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun getExcludedBeansClasses(module: Module): Set<PsiBean> {
        return getAllBeansClasses(module).excluded
    }

    fun getActiveBeansClasses(module: Module): Set<PsiBean> {
        return getAllBeansClasses(module).active
    }

    fun getAllBeanByNames(module: Module): Map<String, List<PsiBean>> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                getActiveBeansClasses(module).groupBy { it.name },
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun getAllBeansClassesWithAncestors(module: Module): Set<PsiClass> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                getActiveBeansClasses(module)
                    .flatMapTo(mutableSetOf()) { it.psiClass.supers.asSequence() + it.psiClass },
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }


    fun getBeanPsiClassesAnnotatedByComponent(module: Module): Set<PsiBean> {
        return cachedValuesManager.getCachedValue(module) {
            val annotationPsiClasses = getComponentClassAnnotations(module)
            CachedValueProvider.Result(
                searchBeanPsiClassesByAnnotations(module, annotationPsiClasses),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
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
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
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
            .flatMap { method ->
                // fixme: because of resolvedPsiClass - the method takes only classes, not arrays
                method.returnType?.resolvedPsiClass?.let { psiClass ->
                    method.resolveBeanName.asSequence()
                        .map { PsiBean(it, psiClass, method.getQualifierAnnotation(), method) }
                } ?: emptySequence()
            }
            .filter { SpringCoreUtil.isSpringBeanCandidateClass(it.psiClass) }
            .toSet()
    }

    private fun searchBeanPsiClassesByAnnotations(
        module: Module, annotationPsiClasses: Collection<PsiClass>
    ): Set<PsiBean> {
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
                    val annotations =
                        MetaAnnotationUtil.getAnnotationTypesWithChildren(module, SpringCoreClasses.COMPONENT, false)
                            .toMutableSet()
                    annotations += LibraryClassCache.searchForLibraryClasses(module, JavaEeClasses.RESOURCE.allFqns)
                    return@run annotations
                }.toSet(),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun getAllReferencesToElement(element: PsiElement): Set<PsiReference> {
        return cachedValuesManager.getCachedValue(element) {
            CachedValueProvider.Result(
                ReferencesSearch.search(element).toSet(),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun getAllComponentScanBeans(module: Module, annotation: String): Set<PsiClass> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                searchBeanPsiClassesByAnnotation(module, annotation),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun getAutowiredFieldAnnotations(module: Module): Collection<PsiClass> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                run {
                    val annotations =
                        MetaAnnotationUtil.getAnnotationTypesWithChildren(module, SpringCoreClasses.AUTOWIRED, false)
                            .toMutableSet()
                    annotations += LibraryClassCache.searchForLibraryClasses(
                        module,
                        JavaEeClasses.INJECT.allFqns + JavaEeClasses.RESOURCE.allFqns
                    )
                    return@run annotations
                },
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun findAnnotationClassesByQualifiedName(module: Module, qualifiedName: String): Collection<PsiClass> {
        val key = CacheKeyStore.getInstance(project).getKey<Collection<PsiClass>>(
            "SpringAnnotationClassesLoaderByQualifiedNameCache(${qualifiedName})"
        )
        return cachedValuesManager.getCachedValue(module, key, {
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

    private fun PsiMember.filterByQualifier(
        module: Module, qualifier: PsiAnnotation?, beanNameFromQualifier: String?
    ): Boolean {
        return qualifier == null
                || beanNameFromQualifier != null && beanNameFromQualifier in this.resolveBeanName(module)
                || EspritoAnnotationUtil.equal(qualifier, this.getAnnotation(qualifier.qualifiedName!!))
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

        val byTypeBeanMethods = methodsPsiBeans.filterByBeanPsiType(this, beanPsiType)
        if (byTypeBeanMethods.isNotEmpty()) return true

        val byTypeComponents = getPsiClassesByComponents(module, this, beanPsiType, true)
        if (byTypeComponents.isNotEmpty()) return true
        return false
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
            .filter { !it.isGeneric(sourcePsiType) || !byTypeBeanMethodsIsEmpty } //TODO: check why we are checking against byTypeBeanMethods
        return byTypeComponents
    }


    private fun findBeanDeclarations(
        module: Module,
        byBeanName: String,
        sourcePsiType: PsiType?,
        qualifier: PsiAnnotation?
    ): List<PsiMember> {
        val beanPsiType = sourcePsiType?.beanPsiType
        val beanPsiClass = sourcePsiType?.resolveBeanPsiClass
        var isMultipleBean = sourcePsiType?.possibleMultipleBean() ?: false
        val methodsPsiBeans = getComponentBeanPsiMethods(module)
        val beanNameFromQualifier = qualifier?.resolveBeanName()

        val resultByType: List<PsiMember> = if (beanPsiType != null) {
            val byExactMatch = methodsPsiBeans.filterByExactMatch(sourcePsiType).toSet()
            var byTypeBeanMethods = byExactMatch
            if (isMultipleBean || byExactMatch.isEmpty()) {
                byTypeBeanMethods = methodsPsiBeans.filterByBeanPsiType(sourcePsiType, beanPsiType).toSet()
            }

            val byTypeComponents =
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
                ClassInheritorsSearch.search(psiClass).findAll().filterNotNullTo(mutableSetOf()),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun Collection<PsiMethod>.filterByExactMatch(sourcePsiType: PsiType): Sequence<PsiMethod> {
        val isSourcePsiTypeHasParameters = sourcePsiType.psiClassType?.let { it.parameterCount > 0 } == true
        val isSourcePsiTypeHasSingleUnboundedWildcardType = sourcePsiType.psiClassType?.let {
            it.parameterCount == 1 && !it.parameters[0].isBounded()
        } == true

        val resolvedSourcePsiClass = sourcePsiType.resolvedPsiClass
        return this.asSequence().filter {
            it.returnType == sourcePsiType
                    || it.returnPsiType?.isEqualOrInheritorBeanType(sourcePsiType) == true
                    || (!isSourcePsiTypeHasParameters || isSourcePsiTypeHasSingleUnboundedWildcardType)
                    && resolvedSourcePsiClass != null && it.returnPsiClass == resolvedSourcePsiClass
        }
    }

    fun Collection<PsiMethod>.filterByBeanPsiType(sourcePsiType: PsiType, beanPsiType: PsiType): Sequence<PsiMethod> {
        val inheritedPsiMethods = this.asSequence().filter {
            it.returnPsiType?.isEqualOrInheritorBeanType(beanPsiType) == true
        }
        val filterByInheritedTypes = this.filterByInheritedTypes(sourcePsiType, beanPsiType)
        return inheritedPsiMethods + filterByInheritedTypes
    }

    fun getBeansPsiMethodsCheckMultipleBean(
        possibleMultipleBeanPsiType: PsiType,
        allBeansPsiMethods: Set<PsiMethod>,
        beanPsiType: PsiType
    ): Collection<PsiMethod> {
        val isMultipleBean = possibleMultipleBeanPsiType.possibleMultipleBean()

        val byExactMatch = allBeansPsiMethods.filterByExactMatch(possibleMultipleBeanPsiType).toSet()
        val byTypeBeanMethods = allBeansPsiMethods.filterByBeanPsiType(possibleMultipleBeanPsiType, beanPsiType).toSet()

        if (isMultipleBean && byExactMatch.isNotEmpty() && byTypeBeanMethods.isEmpty()) {
            // Check if multiple bean has exact type match
            return byExactMatch
        }
        return byTypeBeanMethods
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
            ConditionalOnMissingClassStrategy(module),
            ConditionalOnPropertyStrategy(module) // TODO: не только для автоконфигураций
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
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }, false)
    }

    companion object {
        fun getInstance(project: Project): SpringSearchService = project.service()
    }

}