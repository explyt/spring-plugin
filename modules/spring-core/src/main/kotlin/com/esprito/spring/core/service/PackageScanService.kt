package com.esprito.spring.core.service

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.SpringCoreClasses.COMPONENT
import com.esprito.spring.core.SpringCoreClasses.COMPONENT_SCAN
import com.esprito.spring.core.SpringCoreClasses.COMPONENT_SCANS
import com.esprito.spring.core.SpringCoreClasses.SPRING_BOOT_APPLICATION
import com.esprito.spring.core.SpringProperties
import com.esprito.spring.core.runconfiguration.SpringBootRunConfiguration
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.esprito.util.EspritoPsiUtil.resolvedPsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.execution.RunManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
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
        val rootModulePackages = searchRootClasses(annotationPsiClasses).mapNotNull { findRootModulePackages(it) }

        val rootPackages = rootModulePackages.flatMapTo(mutableSetOf()) { it.packages }
        val scanModulePackages = annotationPsiClasses.scanAnnotationClass
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, GlobalSearchScope.projectScope(project)) }
            .filter { filterComponentScanClass(rootPackages, it) }
            .mapNotNull { toModulePackages(it) }

        val modulePackages = rootModulePackages + scanModulePackages
        val packagesByModuleName = modulePackages.groupingBy { it.moduleName }
            .fold(setOf<String>()) { acc, ell -> acc + ell.packages }
        val rootComponentQualified = modulePackages.asSequence().mapNotNull { it.rootComponentQualified }.toSet()
        return RootDataHolder(packagesByModuleName, rootComponentQualified)
    }

    private fun searchRootClasses(annotationPsiClasses: ScanAnnotationHolder): List<PsiClass> {
        return if (Registry.`is`("esprito.spring.root.runConfiguration")) {
            val springBootRunConfiguration = RunManager.getInstance(project).selectedConfiguration
                ?.configuration as? SpringBootRunConfiguration ?: return emptyList()
            springBootRunConfiguration.mainClass?.let { listOf(it) } ?: return emptyList()
        } else {
            annotationPsiClasses.rootAnnotationClass
                .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, GlobalSearchScope.projectScope(project)) }
        }
    }

    private fun filterComponentScanClass(rootPackages: Set<String>, it: PsiClass?): Boolean {
        val packageName = (it?.containingFile as? PsiJavaFile)?.packageName ?: return false
        return rootPackages.isEmpty() || rootPackages.any { packageName.startsWith(it) }
    }

    private fun findRootModulePackages(psiClass: PsiClass): ModuleRootData? {
        val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return null
        val uClass = psiClass.toUElementOfType<UClass>() ?: return null

        val holderSpringBoot = SpringSearchService.getInstance(project)
            .getMetaAnnotations(module, SPRING_BOOT_APPLICATION)

        val springBootAnnotations = uClass.uAnnotations.filter { holderSpringBoot.contains(it) }
        if (springBootAnnotations.isEmpty()) return null

        val holderScan = SpringSearchService.getInstance(project).getMetaAnnotations(module, COMPONENT_SCAN)
        val bootPackages = springBootAnnotations.flatMapTo(mutableSetOf()) { getPackages(it, holderScan) }

        val scanPackages = uClass.uAnnotations.asSequence()
            .filter { !springBootAnnotations.contains(it) }
            .filter { holderScan.contains(it) }
            .flatMapTo(mutableSetOf()) { getPackages(it, holderScan) }

        //annotation @ComponentScan override @SpringBootApplication on some class
        val packages = scanPackages.ifEmpty { bootPackages }
        return ModuleRootData(module.name, packages, psiClass.qualifiedName)
    }

    private fun toModulePackages(psiClass: PsiClass): ModuleRootData? {
        val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return null
        val uClass = psiClass.toUElementOfType<UClass>() ?: return null

        val holderComponent = SpringSearchService.getInstance(project).getMetaAnnotations(module, COMPONENT)
        uClass.uAnnotations.find { holderComponent.contains(it) } ?: return null

        val holderScan = SpringSearchService.getInstance(project).getMetaAnnotations(module, COMPONENT_SCAN)
        val packages = uClass.uAnnotations.asSequence()
            .filter { holderScan.contains(it) }
            .flatMapTo(mutableSetOf()) { getPackages(it, holderScan) }

        val holderScans = SpringSearchService.getInstance(project).getMetaAnnotations(module, COMPONENT_SCANS)
        val packagesScans = uClass.uAnnotations.asSequence()
            .filter { holderScans.contains(it) }
            .flatMapTo(mutableSetOf()) { getPackagesScans(it, holderScan) }

        return ModuleRootData(module.name, packages + packagesScans)
    }

    private fun getPackagesScans(uAnnotation: UAnnotation, metaAnnotationsHolder: MetaAnnotationsHolder): Set<String> {
        val findAttributeValue = uAnnotation.findAttributeValue(SpringProperties.VALUE) ?: return emptySet()
        val memberValue = findAttributeValue.javaPsi as? PsiAnnotationMemberValue ?: return emptySet()
        return AnnotationUtil.arrayAttributeValues(memberValue)
            .mapNotNull { it.toUElement() as? UAnnotation }
            .flatMapTo(mutableSetOf()) { getPackages(it, metaAnnotationsHolder) }
    }

    private fun getPackages(uAnnotation: UAnnotation, metaAnnotationsHolder: MetaAnnotationsHolder): Set<String> {
        val qualifiedName = uAnnotation.qualifiedName ?: return emptySet()
        val basePackages = uAnnotation.attributeValues.asSequence()
            .filter {
                metaAnnotationsHolder.isAttributeRelatedWith(
                    qualifiedName, it.name ?: SpringProperties.VALUE, COMPONENT_SCAN, setOf(SpringProperties.VALUE)
                )
            }
            .flatMap { getBasePackages(it.expression) }
            .toSet()
        val basePackageClasses = uAnnotation.attributeValues.asSequence()
            .filter {
                metaAnnotationsHolder.isAttributeRelatedWith(
                    qualifiedName, it.name ?: SpringProperties.VALUE, COMPONENT_SCAN, setOf("basePackageClasses")
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

    private fun getBasePackages(uExpression: UExpression): List<String> {
        return if (uExpression is UCallExpression) {
            uExpression.valueArguments.mapNotNull { it.evaluate() as? String }
        } else {
            (uExpression.evaluate() as? String)?.let { listOf(it) } ?: emptyList()
        }
    }

    private fun getClassPackages(uExpression: UExpression): List<String> {
        return if (uExpression is UCallExpression) {
            uExpression.valueArguments.mapNotNull { getClassPackage(it) }
        } else {
            getClassPackage(uExpression)?.let { listOf(it) } ?: emptyList()
        }
    }

    private fun getClassPackage(uExpression: UExpression): String? {
        val uClassLiteralExpression = uExpression as? UClassLiteralExpression ?: return null
        val javaFile = uClassLiteralExpression.type?.resolvedPsiClass?.containingFile as? PsiJavaFile ?: return null
        return javaFile.packageName
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
        val childrenScan = MetaAnnotationUtil.getChildren(componentScanClass, GlobalSearchScope.allScope(project))

        val rootAnnotationClass = getSpringBootAppAnnotations()
        val scanAnnotationClass = (childrenScan + componentScanClass + componentScansClass)
            .filterTo(mutableSetOf()) { !rootAnnotationClass.contains(it) }

        return ScanAnnotationHolder(rootAnnotationClass, scanAnnotationClass)
    }

    private fun getSpringBootAppAnnotations(): Set<PsiClass> {
        val springBootAppClass = LibraryClassCache.searchForLibraryClass(project, SPRING_BOOT_APPLICATION)
            ?: return emptySet()
        val childrenBoot = MetaAnnotationUtil.getChildren(springBootAppClass, GlobalSearchScope.allScope(project))
        return (childrenBoot + springBootAppClass).toSet()
    }

    companion object {
        fun getInstance(project: Project): PackageScanService = project.service()
    }

    private data class ModuleRootData(
        val moduleName: String, val packages: Set<String>, val rootComponentQualified: String? = null
    )

    private data class ScanAnnotationHolder(
        val rootAnnotationClass: Set<PsiClass> = emptySet(), val scanAnnotationClass: Set<PsiClass> = emptySet()
    )
}

data class RootDataHolder(
    private val packagesByModuleName: Map<String, Set<String>>,
    private val rootComponentQualified: Set<String>
) {
    fun isEmpty() = packagesByModuleName.isEmpty()

    fun isRootComponent(qualifiedName: String) = rootComponentQualified.contains(qualifiedName)

    fun getPackages(module: Module): Set<String> {
        val packages = packagesByModuleName.getOrDefault(module.name, emptySet())
        val dependentModules = ModuleManager.getInstance(module.project).getModuleDependentModules(module)
        val dependentPackages = dependentModules
            .flatMapTo(mutableSetOf()) { packagesByModuleName.getOrDefault(it.name, emptySet()) }
        return dependentPackages + packages
    }
}