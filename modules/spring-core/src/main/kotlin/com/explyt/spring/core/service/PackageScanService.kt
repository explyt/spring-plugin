/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreClasses.COMPONENT
import com.explyt.spring.core.SpringCoreClasses.COMPONENT_SCAN
import com.explyt.spring.core.SpringCoreClasses.COMPONENT_SCANS
import com.explyt.spring.core.SpringCoreClasses.ENABLE_AUTO_CONFIGURATION
import com.explyt.spring.core.SpringCoreClasses.IMPORT
import com.explyt.spring.core.SpringCoreClasses.SPRING_BOOT_APPLICATION
import com.explyt.spring.core.SpringCoreClasses.SPRING_BOOT_TEST
import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.runconfiguration.RunConfigurationUtil
import com.explyt.spring.core.runconfiguration.SpringBootRunConfiguration
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.execution.RunManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.uast.*

@Service(Service.Level.PROJECT)
class PackageScanService(private val project: Project) {

    fun getAllPackages(): RootDataHolder {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getAllPackagesInner(),
                ModificationTrackerManager.getInstance(project).getUastAnnotationAndLibraryTracker()
            )
        }
    }

    private fun getAllPackagesInner(): RootDataHolder {
        val annotationPsiClasses = getComponentScanAnnotations()
        val rootClasses = searchRootClasses(annotationPsiClasses)
        val rootModuleData = rootClasses.mapNotNull { findRootModulePackages(it) }
        val programConfigRootClasses = AnnotationConfigApplicationService.getRootClasses(rootClasses, rootModuleData)

        val rootPackages = rootModuleData.flatMapTo(mutableSetOf()) { it.packages }
        val scanModuleData = getComponentScanClasses(annotationPsiClasses, rootPackages, programConfigRootClasses)

        val moduleRootDataList = rootModuleData + scanModuleData

        val importAnnotationClass = annotationPsiClasses.importAnnotationClass
        val importClasses = getImportClasses(moduleRootDataList, importAnnotationClass, programConfigRootClasses)
        val importModuleRootDataList = importClasses.mapNotNull { toModulePackages(it, true) }
        val allModuleRootDataList = moduleRootDataList + importModuleRootDataList

        val packagesByModuleName = allModuleRootDataList
            .filterNot { it.rootComponentQualified != null && it.packages.isEmpty() }
            .groupingBy { it.moduleName }
            .fold(setOf<String>()) { acc, ell -> acc + ell.packages.map { normalizePackage(it) } }
        val rootComponentQualified = moduleRootDataList.mapNotNullTo(mutableSetOf()) { it.rootComponentQualified }
        val importQualified = importClasses.mapNotNullTo(mutableSetOf()) { it.qualifiedName }
        val scannedRootsByModuleName = scanModuleData
            .mapNotNull { data -> data.declaringClass?.let { data.moduleName to ScannedRoot(it, data.packages) } }
            .groupBy({ it.first }, { it.second })

        return RootDataHolder(packagesByModuleName, rootComponentQualified, importQualified, scannedRootsByModuleName)
    }

    private fun getComponentScanClasses(
        annotationPsiClasses: ScanAnnotationHolder, rootPackages: MutableSet<String>, programConfigRoots: List<PsiClass>
    ): List<ModuleRootData> {
        val classesForSearchComponentScan = programConfigRoots.ifEmpty {
            annotationPsiClasses.scanAnnotationClass
                .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, GlobalSearchScope.projectScope(project)) }
        }
        return classesForSearchComponentScan
            .filter { filterComponentScanClass(rootPackages, it) }
            .mapNotNull { toModulePackages(it) }
    }

    private fun getImportClasses(
        moduleRootData: List<ModuleRootData>, importAnnotationClass: Set<PsiClass>, programConfigRoots: List<PsiClass>
    ): Set<PsiClass> {
        val packages = moduleRootData.flatMapTo(mutableSetOf()) { it.packages }
        val classesForSearchImport = programConfigRoots.ifEmpty {
            importAnnotationClass
                .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, GlobalSearchScope.projectScope(project)) }
        }
        return classesForSearchImport
            .filter { filterComponentScanClass(packages, it) }
            .flatMapTo(mutableSetOf()) { getImportClasses(it) }
    }

    private fun searchRootClasses(annotationPsiClasses: ScanAnnotationHolder): List<PsiClass> {
        if (Registry.`is`("explyt.spring.root.runConfiguration")) {
            val runConfiguration = RunManager.getInstance(project).selectedConfiguration
                ?.configuration as? SpringBootRunConfiguration
            if (runConfiguration != null) {
                return RunConfigurationUtil.getRunPsiClass(runConfiguration)
            }
        }

        return annotationPsiClasses.rootAnnotationClass
                .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, GlobalSearchScope.projectScope(project)) }
    }

    private fun filterComponentScanClass(rootPackages: Set<String>, it: PsiClass?): Boolean {
        val packageName = (it?.containingFile as? PsiJavaFile)?.packageName ?: return false
        return rootPackages.isEmpty() || rootPackages.any { packageName.startsWith(it) }
    }

    private fun findRootModulePackages(psiClass: PsiClass): ModuleRootData? {
        val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return null
        val uClass = psiClass.toUElementOfType<UClass>() ?: return null

        val holderApplication = SpringSearchService.getInstance(project)
            .getMetaAnnotations(module, ENABLE_AUTO_CONFIGURATION)

        val applicationAnnotations = uClass.uAnnotations.filter { holderApplication.contains(it) }
        if (applicationAnnotations.isEmpty()) return null

        val holderScan = SpringSearchService.getInstance(project).getMetaAnnotations(module, COMPONENT_SCAN)
        val bootPackages = applicationAnnotations.asSequence()
            .filter { holderScan.contains(it) }
            .flatMapTo(mutableSetOf()) { getPackages(it, holderScan) }

        val scanPackages = uClass.uAnnotations.asSequence()
            .filter { !applicationAnnotations.contains(it) }
            .filter { holderScan.contains(it) }
            .flatMapTo(mutableSetOf()) { getPackages(it, holderScan) }

        //annotation @ComponentScan override @SpringBootApplication on some class
        val packages = scanPackages.ifEmpty { bootPackages }
        return ModuleRootData(module.name, packages, psiClass.qualifiedName)
    }

    private fun toModulePackages(psiClass: PsiClass, isImport: Boolean = false): ModuleRootData? {
        val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return null
        val uClass = psiClass.toUElementOfType<UClass>() ?: return null

        if (!isImport) {
            val holderComponent = SpringSearchService.getInstance(project).getMetaAnnotations(module, COMPONENT)
            uClass.uAnnotations.find { holderComponent.contains(it) } ?: return null
        }

        val holderScan = SpringSearchService.getInstance(project).getMetaAnnotations(module, COMPONENT_SCAN)
        val packages = uClass.uAnnotations.asSequence()
            .filter { holderScan.contains(it) }
            .flatMapTo(mutableSetOf()) { getPackages(it, holderScan) }

        val holderScans = SpringSearchService.getInstance(project).getMetaAnnotations(module, COMPONENT_SCANS)
        val packagesScans = uClass.uAnnotations.asSequence()
            .filter { holderScans.contains(it) }
            .flatMapTo(mutableSetOf()) { getPackagesScans(it, holderScan) }

        val allPackages = packages + packagesScans
        if (allPackages.isEmpty()) return null
        return ModuleRootData(module.name, allPackages, declaringClass = psiClass.qualifiedName)
    }

    private fun getImportClasses(psiClass: PsiClass): Set<PsiClass> {
        val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return emptySet()
        val uClass = psiClass.toUElementOfType<UClass>() ?: return emptySet()

        val holderComponent = SpringSearchService.getInstance(project).getMetaAnnotations(module, COMPONENT)
        uClass.uAnnotations.find { holderComponent.contains(it) } ?: return emptySet()

        val holderImport = SpringSearchService.getInstance(project).getMetaAnnotations(module, IMPORT)
        return uClass.uAnnotations.asSequence()
            .filter { holderImport.contains(it) }
            .flatMapTo(mutableSetOf()) { getImportClasses(it, holderImport) }
    }

    fun getAllImportClasses(uClass: UClass): Set<PsiClass> {
        val module = uClass.module ?: return emptySet()
        val holderImport = SpringSearchService.getInstance(project).getMetaAnnotations(module, IMPORT)
        val holderSpringTest = SpringSearchService.getInstance(project).getMetaAnnotations(module, SPRING_BOOT_TEST)

        val springTestClasses = uClass.uAnnotations.asSequence()
            .filter { holderSpringTest.contains(it) }
            .flatMap { holderSpringTest.getAnnotationMemberValues(it, setOf("classes")) }
            .flatMap { getPsiClasses(it) }
            .toSet()
        val importClasses = uClass.uAnnotations.asSequence()
            .filter { holderImport.contains(it) }
            .flatMap { holderImport.getAnnotationMemberValues(it, setOf(SpringProperties.VALUE)) }
            .flatMap { getPsiClasses(it) }
            .toSet()
        return importClasses + springTestClasses
    }

    private fun getPackagesScans(uAnnotation: UAnnotation, metaAnnotationsHolder: MetaAnnotationsHolder): Set<String> {
        val findAttributeValue = uAnnotation.findAttributeValue(SpringProperties.VALUE) ?: return emptySet()
        val memberValue = findAttributeValue.javaPsi as? PsiAnnotationMemberValue ?: return emptySet()
        return AnnotationUtil.arrayAttributeValues(memberValue)
            .mapNotNull { it.toUElement() as? UAnnotation }
            .flatMapTo(mutableSetOf()) { getPackages(it, metaAnnotationsHolder) }
    }

    private fun getImportClasses(
        uAnnotation: UAnnotation,
        metaAnnotationsHolder: MetaAnnotationsHolder
    ): Set<PsiClass> {
        val qualifiedName = uAnnotation.qualifiedName ?: return emptySet()
        return uAnnotation.attributeValues.asSequence()
            .filter {
                metaAnnotationsHolder.isAttributeRelatedWith(
                    qualifiedName, it.name ?: SpringProperties.VALUE, IMPORT, setOf(SpringProperties.VALUE)
                )
            }
            .flatMap { getPsiClasses(it.expression) }
            .toSet()
    }

    private fun getComponentScanAnnotations(): ScanAnnotationHolder {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getComponentScanAnnotationsInner(),
                ModificationTrackerManager.getInstance(project).getLibraryTracker()
            )
        }
    }

    private fun getComponentScanAnnotationsInner(): ScanAnnotationHolder {
        val componentScanClass = LibraryClassCache.searchForLibraryClass(project, COMPONENT_SCAN)
            ?: return ScanAnnotationHolder()
        val componentScansClass = LibraryClassCache.searchForLibraryClass(project, COMPONENT_SCANS)
            ?: return ScanAnnotationHolder()
        val importClass = LibraryClassCache.searchForLibraryClass(project, IMPORT)
            ?: return ScanAnnotationHolder()
        val childrenScan = MetaAnnotationUtil.getChildren(componentScanClass, GlobalSearchScope.allScope(project))
        val childrenImport = MetaAnnotationUtil.getChildren(importClass, GlobalSearchScope.allScope(project))

        val rootAnnotationClass = getSpringBootAppAnnotations()
        val scanAnnotationClass = (childrenScan + componentScanClass + componentScansClass)
            .filterTo(mutableSetOf()) { !rootAnnotationClass.contains(it) }
        val importClasses = (childrenImport + importClass).toSet()

        return ScanAnnotationHolder(rootAnnotationClass, scanAnnotationClass, importClasses)
    }

    fun getSpringBootAppAnnotations(): Set<PsiClass> {
        val allScope = GlobalSearchScope.allScope(project)
        return listOfNotNull(
            LibraryClassCache.searchForLibraryClass(project, ENABLE_AUTO_CONFIGURATION),
            LibraryClassCache.searchForLibraryClass(project, SPRING_BOOT_APPLICATION),
        ).flatMapTo(mutableSetOf()) { MetaAnnotationUtil.getChildren(it, allScope) + it }
    }

    fun applicationClasses(scope: GlobalSearchScope): List<PsiClass> {
        val fileIndex = ProjectFileIndex.getInstance(project)
        val inTestSources = { psiClass: PsiClass ->
            psiClass.containingFile?.virtualFile?.let { fileIndex.isInTestSourceContent(it) } ?: false
        }
        return getSpringBootAppAnnotations().asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, scope) }
            .filterNot { it.isAnnotationType }
            .distinct()
            .sortedWith(compareBy(inTestSources).thenBy { it.qualifiedName.orEmpty() })
            .toList()
    }

    companion object {
        fun getInstance(project: Project): PackageScanService = project.service()

        fun normalizePackage(packageName: String): String {
            if (packageName.endsWith(".")) return packageName
            if (packageName.endsWith("*")) return normalizePackage(packageName.substring(0, packageName.length - 2))
            return "$packageName."
        }

        fun getPackages(uAnnotation: UAnnotation, metaAnnotationsHolder: MetaAnnotationsHolder): Set<String> {
            val qualifiedName = uAnnotation.qualifiedName ?: return emptySet()
            val rootMetaClass = metaAnnotationsHolder.getRootClassQualified()
            val basePackages = uAnnotation.attributeValues.asSequence()
                .filter {
                    metaAnnotationsHolder.isAttributeRelatedWith(
                        qualifiedName, it.name ?: SpringProperties.VALUE, rootMetaClass, setOf(SpringProperties.VALUE)
                    )
                }
                .flatMap { getBasePackages(it.expression) }
                .toSet()
            val basePackageClasses = uAnnotation.attributeValues.asSequence()
                .filter {
                    metaAnnotationsHolder.isAttributeRelatedWith(
                        qualifiedName, it.name ?: SpringProperties.VALUE, rootMetaClass, setOf("basePackageClasses")
                    )
                }
                .flatMap { getClassPackages(it.expression) }
                .toSet()
            val packages = basePackages + basePackageClasses
            //case with empty params. example: @ComponentScan
            if (packages.isEmpty()) {
                val packageName = uAnnotation.getContainingUFile()?.packageName ?: return emptySet()
                return setOf(packageName)
            }
            return packages
        }

        fun getBasePackages(uExpression: UExpression): List<String> {
            return if (uExpression is UCallExpression) {
                uExpression.valueArguments.mapNotNull { it.evaluate() as? String }
            } else {
                (uExpression.evaluate() as? String)?.let { listOf(it) } ?: emptyList()
            }
        }

        fun getClassPackages(uExpression: UExpression): List<String> {
            return getPsiClasses(uExpression).mapNotNull { (it.containingFile as? PsiJavaFile)?.packageName }
        }

        fun getPsiClasses(uExpression: UExpression): List<PsiClass> {
            return if (uExpression is UCallExpression) {
                uExpression.valueArguments.mapNotNull { getPsiClass(it) }
            } else {
                getPsiClass(uExpression)?.let { listOf(it) } ?: emptyList()
            }
        }

        private fun getPsiClass(uExpression: UExpression): PsiClass? {
            val uClassLiteralExpression = uExpression as? UClassLiteralExpression ?: return null
            return uClassLiteralExpression.type?.resolvedPsiClass
        }
    }

    /**
     * @property rootComponentQualified the application class, for an application's own root.
     * @property declaringClass the configuration that declares the scan, for a root found on a scanned configuration.
     */
    data class ModuleRootData(
        val moduleName: String,
        val packages: Set<String>,
        val rootComponentQualified: String? = null,
        val declaringClass: String? = null,
    )

    private data class ScanAnnotationHolder(
        val rootAnnotationClass: Set<PsiClass> = emptySet(),
        val scanAnnotationClass: Set<PsiClass> = emptySet(),
        val importAnnotationClass: Set<PsiClass> = emptySet()
    )
}

/**
 * The packages the application context of each module scans.
 *
 * @property packagesByModuleName every scan root, keyed by the module that declares it: an application's own root and
 * the `@ComponentScan`/`@Import` roots of the configurations it reaches.
 * @property scannedRootsByModuleName the `@ComponentScan` roots declared by scanned configurations, keyed by the
 * configuration's module. These are what a dependency module adds to an application that scans it. A dependency module's
 * own `@SpringBootApplication` root belongs to that other application and is never among them.
 */
data class RootDataHolder(
    private val packagesByModuleName: Map<String, Set<String>>,
    val rootComponentQualified: Set<String>,
    val importQualified: Set<String>,
    private val scannedRootsByModuleName: Map<String, List<ScannedRoot>> = emptyMap(),
) {
    fun isEmpty() = packagesByModuleName.isEmpty()

    fun isRootComponent(qualifiedName: String) = rootComponentQualified.contains(qualifiedName)

    fun getPackages(module: Module): Set<String> {
        val packages = packagesByModuleName.getOrDefault(module.name, emptySet())
        val dependentModules = ModuleManager.getInstance(module.project).getModuleDependentModules(module)
        val dependentPackages = dependentModules
            .flatMapTo(mutableSetOf()) { packagesByModuleName.getOrDefault(it.name, emptySet()) }
        val resultPackages = dependentPackages + packages + scannedPackagesOfDependencies(module, packages)
        if (resultPackages.isEmpty() && module.name.endsWith(".test")) {
            val mainModuleName = module.name.substringBeforeLast(".test") + ".main"
            val mainModule = ModuleManager.getInstance(module.project)
                .findModuleByName(mainModuleName) ?: return emptySet()
            return getPackages(mainModule)
        }
        return resultPackages
    }

    /**
     * The `@ComponentScan` roots that configurations in [module]'s dependencies declare, kept only where [module]'s own
     * scan reaches the configuration that declares them: Spring follows a scan root only from a class it registered.
     */
    private fun scannedPackagesOfDependencies(module: Module, ownPackages: Set<String>): Set<String> {
        if (ownPackages.isEmpty() || scannedRootsByModuleName.isEmpty()) return emptySet()
        val dependencies = linkedSetOf<Module>().also { ModuleUtilCore.getDependencies(module, it) } - module
        return dependencies.asSequence()
            .flatMap { scannedRootsByModuleName[it.name].orEmpty() }
            .filter { root -> ownPackages.any(root.declaringClass::startsWith) }
            .flatMapTo(mutableSetOf()) { root -> root.packages.map(PackageScanService::normalizePackage) }
    }
}

/** The packages a scanned configuration's `@ComponentScan` adds, and that configuration's qualified name. */
data class ScannedRoot(val declaringClass: String, val packages: Set<String>)
