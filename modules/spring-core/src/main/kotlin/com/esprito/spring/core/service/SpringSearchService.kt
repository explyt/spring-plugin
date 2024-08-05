package com.esprito.spring.core.service

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.JavaEeClasses
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.esprito.spring.core.service.beans.discoverer.AdditionalBeansDiscoverer
import com.esprito.spring.core.service.conditional.*
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.esprito.spring.core.util.SpringCoreUtil
import com.esprito.spring.core.util.SpringCoreUtil.beanPsiType
import com.esprito.spring.core.util.SpringCoreUtil.beanPsiTypeKotlin
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
import com.esprito.util.EspritoPsiUtil.getMetaAnnotation
import com.esprito.util.EspritoPsiUtil.isEqualOrInheritor
import com.esprito.util.EspritoPsiUtil.isGeneric
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isNonStatic
import com.esprito.util.EspritoPsiUtil.isPublic
import com.esprito.util.EspritoPsiUtil.psiClassType
import com.esprito.util.EspritoPsiUtil.resolvedDeepPsiClass
import com.esprito.util.EspritoPsiUtil.resolvedPsiClass
import com.esprito.util.EspritoPsiUtil.returnPsiClass
import com.esprito.util.EspritoPsiUtil.returnPsiType
import com.esprito.util.ModuleUtil
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
import com.intellij.uast.UastModificationTracker
import com.jetbrains.rd.util.getOrCreate
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.*
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class SpringSearchService(private val project: Project) {

    private val cachedValuesManager: CachedValuesManager = CachedValuesManager.getManager(project)
    private val mutexSearchBeans = ConcurrentHashMap<String, String>()
    private val mutexConditionalOn = ConcurrentHashMap<String, String>()

    private fun searchAllBeanClasses(module: Module): Set<PsiBean> {
        val allPsiClassesAnnotatedByComponent = getBeanPsiClassesAnnotatedByComponent(module)
        val methodsAnnotatedByBeanReturnTypes = searchComponentPsiClassesByBeanMethods(module)
        val staticBeans = getStaticBeans(module)
        return allPsiClassesAnnotatedByComponent + methodsAnnotatedByBeanReturnTypes + staticBeans
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

    fun getAllBeansClassesConsideringContext(project: Project): FoundBeans {
        return cachedValuesManager.getCachedValue(project) {
            CachedValueProvider.Result(
                doGetAllBeansClassesConsideringContext(project),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
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
                    // может быть стоит для внешних бинов сделать отдельную иконку?
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

    fun getAllBeanByNamesLight(module: Module): Map<String, List<PsiBean>> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                searchAllBeanLight(module).groupBy { it.name },
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun getAllBeansClassesWithAncestors(module: Module): Set<PsiClass> {
        synchronized(getMutexString(MutexType.CONDITIONAL_ON, module)) {
            return cachedValuesManager.getCachedValue(module) {
                CachedValueProvider.Result(
                    getActiveBeansClasses(module)
                        .flatMapTo(mutableSetOf()) { it.psiClass.supers.asSequence() + it.psiClass },
                    ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
                )
            }
        }
    }

    fun getAllBeansClassesWithAncestorsLight(module: Module): Set<PsiClass> {
        synchronized(getMutexString(MutexType.SEARCH, module)) {
            return cachedValuesManager.getCachedValue(module) {
                CachedValueProvider.Result(
                    searchAllBeanLight(module)
                        .flatMapTo(mutableSetOf()) { it.psiClass.supers.asSequence() + it.psiClass },
                    ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
                )
            }
        }
    }

    fun getAllActiveBeans(module: Module): Set<PsiClass> {
        synchronized(getMutexString(MutexType.CONDITIONAL_ON, module)) {
            return cachedValuesManager.getCachedValue(module) {
                CachedValueProvider.Result(
                    getActiveBeansClasses(module).mapTo(mutableSetOf()) { it.psiClass },
                    ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
                )
            }
        }
    }

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

    private fun isInSpringContext(uBeanElement: UElement, module: Module): Boolean {
        if (uBeanElement is UMethod && uBeanElement.returnType is PsiArrayType) {
            val deepArrayPsiClass = uBeanElement.returnType?.resolvedDeepPsiClass ?: return false
            return searchArrayComponentPsiClassesByBeanMethods(module)
                .map { it.psiClass }.contains(deepArrayPsiClass)
        }
        return getBeanClass(uBeanElement) in getAllActiveBeans(module)
    }

    fun isInSpringContextLight(uBeanElement: UElement, module: Module): Boolean {
        if (uBeanElement is UMethod && uBeanElement.returnType is PsiArrayType) {
            val deepArrayPsiClass = uBeanElement.returnType?.resolvedDeepPsiClass ?: return false
            return searchArrayComponentPsiClassesByBeanMethods(module)
                .map { it.psiClass }.contains(deepArrayPsiClass)
        }
        return getBeanClass(uBeanElement) in getAllActiveBeansLight(module)
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

    fun getBeanPsiClassesAnnotatedByComponent(module: Module): Set<PsiBean> {
        synchronized(getMutexString(MutexType.SEARCH, module)) {
            return cachedValuesManager.getCachedValue(module) {
                val scope = GlobalSearchScope.moduleWithDependenciesScope(module)
                val annotationPsiClasses = getComponentClassAnnotations(module)
                val modulePackagesHolder = PackageScanService.getInstance(module.project).getAllPackages()
                val allModuleWithDependenciesBeans = filterBeansByPackage(
                    searchBeanPsiClassesByAnnotations(module, annotationPsiClasses, scope),
                    modulePackagesHolder, module
                )
                val extraComponents = getExtraComponents(module, modulePackagesHolder)
                val moduleWithDependenciesBeans = allModuleWithDependenciesBeans + extraComponents
                val moduleLibraryBeans = searchBeanPsiClassesByComponentAnnotationLibraryScopeCached(module)
                val importedPsiBeans = getImportedBeans(modulePackagesHolder, module)

                CachedValueProvider.Result(
                    moduleWithDependenciesBeans + moduleLibraryBeans + importedPsiBeans,
                    ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
                )
            }
        }
    }

    fun getDependentBeanPsiClassesAnnotatedByComponent(module: Module): Set<PsiBean> {
        return cachedValuesManager.getCachedValue(module) {
            val scope = GlobalSearchScope.moduleWithDependentsScope(module)
            val annotationPsiClasses = getComponentClassAnnotations(module)
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

    fun searchReferenceByMethod(
        module: Module,
        method: PsiMethod,
        scope: SearchScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
    ): Collection<PsiReference> {
        return MethodReferencesSearch.search(method, scope, true).findAll()
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

    private fun searchBeanPsiClassesByComponentAnnotationLibraryScopeCached(module: Module): Set<PsiBean> {
        return cachedValuesManager.getCachedValue(module) {
            val annotationPsiClasses = getComponentClassAnnotations(module)
            CachedValueProvider.Result(
                searchBeanPsiClassesByAnnotations(
                    module, annotationPsiClasses, ModuleUtil.getOnlyLibrarySearchScope(module)
                ),
                ModificationTrackerManager.getInstance(project).getLibraryTracker()
            )
        }
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

    fun getAutowiredFieldAnnotations(module: Module): Collection<PsiClass> {
        return cachedValuesManager.getCachedValue(module) {
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
            .filter { !it.isGeneric(sourcePsiType) || !byTypeBeanMethodsIsEmpty } //TODO: check why we are checking against byTypeBeanMethods
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
                byTypeBeanMethods = methodsPsiBeans.filterByBeanPsiType(sourcePsiType, beanPsiType).toSet()
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

    fun findActiveBeanDeclarations(
        module: Module,
        byBeanName: String,
        language: Language,
        byBeanPsiType: PsiType? = null,
        qualifier: PsiAnnotation? = null
    ): List<PsiMember> = runReadAction {
        val beanDeclarations = findBeanDeclarations(module, byBeanName, byBeanPsiType, qualifier, language)
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
            it.returnType?.isEqualOrInheritorBeanType(sourcePsiType) ?: false ||
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
        // This function added a candidate to the beans, where returned other type.
        // Example: @Bean E dBean() { return new D(); }
        // val filterByInheritedTypes = this.filterByInheritedTypes(sourcePsiType, beanPsiType)
        return inheritedPsiMethods // + filterByInheritedTypes
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

    fun getMetaAnnotations(module: Module, annotationFqn: String): MetaAnnotationsHolder {
        val key = CacheKeyStore.getInstance(module.project).getKey<MetaAnnotationsHolder>(annotationFqn)

        return cachedValuesManager.getCachedValue(module, key, {
            CachedValueProvider.Result(
                MetaAnnotationsHolder.of(module, annotationFqn),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }, false)
    }

    fun findUAnnotation(module: Module, uAnnotations: List<UAnnotation>, annotationFqn: String): UAnnotation? {
        if (uAnnotations.isEmpty()) return null

        val metaAnnotationsHolder = getMetaAnnotations(module, annotationFqn)
        for (annotation in uAnnotations) {
            val psiAnnotation = annotation.javaPsi ?: continue
            if (metaAnnotationsHolder.contains(psiAnnotation)) {
                return annotation
            }
        }
        return null
    }

    private fun isBean(uClass: UClass): Boolean {
        if (AnnotationUtil.isAnnotated(uClass.javaPsi, SpringCoreClasses.COMPONENT, AnnotationUtil.CHECK_HIERARCHY)) {
            return true
        }
        val psiElement = uClass.sourcePsi ?: return false
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return false
        return getAllBeansClassesWithAncestors(module).contains(uClass.javaPsi)
    }

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
            MutexType.CONDITIONAL_ON -> mutexConditionalOn
                .getOrCreate("conditionalOn:" + module.name) { "conditionalOn:" + module.name }
        }
    }

    companion object {
        fun getInstance(project: Project): SpringSearchService = project.service()
    }

}

data class MethodsInfoHolder(val beanPublicAnnotatedMethods: Set<PsiMethod>) {
    val beanPublicAnnotatedMethodNames: Set<String> = beanPublicAnnotatedMethods.map { it.name }.toSet()
}

private enum class MutexType {
    SEARCH, CONDITIONAL_ON
}