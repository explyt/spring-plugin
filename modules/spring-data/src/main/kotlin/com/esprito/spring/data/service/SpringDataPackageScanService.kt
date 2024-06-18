package com.esprito.spring.data.service

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.SpringCoreClasses.COMPONENT
import com.esprito.spring.core.SpringProperties
import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.esprito.spring.core.service.PackageScanService
import com.esprito.spring.core.service.PackageScanService.Companion.normalizePackage
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.esprito.spring.data.SpringDataClasses.ENABLE_JPA_REPOSITORY
import com.esprito.util.EspritoPsiUtil.resolvedPsiClass
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.uast.*

@Service(Service.Level.PROJECT)
class SpringDataPackageScanService(private val project: Project) {

    fun getPackages(module: Module): Set<String> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getAllPackagesInner(module),
                ModificationTrackerManager.getInstance(project).getUastAnnotationAndLibraryTracker()
            )
        }
    }

    private fun getAllPackagesInner(module: Module): Set<String> {
        val packages = PackageScanService.getInstance(project).getAllPackages().getPackages(module)

        val annotationPsiClass = getEnableJpaRepositoryAnnotation() ?: return packages
        val annotatedClasses = AnnotatedElementsSearch
            .searchPsiClasses(annotationPsiClass, module.moduleWithDependentsScope)
            .findAll()
            .takeIf { it.isNotEmpty() } ?: return packages
        val annotatedClassesInPackages = annotatedClasses.filter { filterAnnotatedClass(packages, it) }

        val holderComponent = SpringSearchService.getInstance(project).getMetaAnnotations(module, COMPONENT)
        val holderEnableJpa = SpringSearchService.getInstance(project).getMetaAnnotations(module, ENABLE_JPA_REPOSITORY)

        val resultPackages = mutableSetOf<String>().also { it.addAll(packages) }
        for (annotatedClass in annotatedClassesInPackages) {
            val uClass = annotatedClass.toUElementOfType<UClass>() ?: continue
            uClass.uAnnotations.find { holderComponent.contains(it) } ?: continue

            resultPackages += uClass.uAnnotations.asSequence()
                .filter { holderEnableJpa.contains(it) }
                .flatMap { getPackages(it, holderEnableJpa) }
                .map { normalizePackage(it) }
        }

        return resultPackages
    }

    private fun filterAnnotatedClass(rootPackages: Set<String>, it: PsiClass?): Boolean {
        val qualifiedName = it?.qualifiedName ?: return false
        return rootPackages.isEmpty() || rootPackages.any { qualifiedName.startsWith(it) }
    }

    private fun getPackages(uAnnotation: UAnnotation, metaAnnotationsHolder: MetaAnnotationsHolder): Set<String> {
        val qualifiedName = uAnnotation.qualifiedName ?: return emptySet()
        val basePackagesValue = uAnnotation.attributeValues.asSequence()
            .filter {
                metaAnnotationsHolder.isAttributeRelatedWith(
                    qualifiedName,
                    it.name ?: SpringProperties.VALUE,
                    ENABLE_JPA_REPOSITORY,
                    setOf(SpringProperties.VALUE)
                )
            }
            .flatMap { getBasePackages(it.expression) }
            .toSet()
        val basePackages = uAnnotation.attributeValues.asSequence()
            .filter {
                metaAnnotationsHolder.isAttributeRelatedWith(
                    qualifiedName, it.name ?: SpringProperties.VALUE, ENABLE_JPA_REPOSITORY, setOf("basePackages")
                )
            }
            .flatMap { getBasePackages(it.expression) }
            .toSet()
        val basePackageClasses = uAnnotation.attributeValues.asSequence()
            .filter {
                metaAnnotationsHolder.isAttributeRelatedWith(
                    qualifiedName, it.name ?: SpringProperties.VALUE, ENABLE_JPA_REPOSITORY, setOf("basePackageClasses")
                )
            }
            .flatMap { getClassPackages(it.expression) }
            .toSet()
        val packages = basePackagesValue + basePackages + basePackageClasses
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
        return getPsiClasses(uExpression).mapNotNull { (it.containingFile as? PsiJavaFile)?.packageName }
    }

    private fun getPsiClasses(uExpression: UExpression): List<PsiClass> {
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

    private fun getEnableJpaRepositoryAnnotation(): PsiClass? {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                LibraryClassCache.searchForLibraryClass(project, ENABLE_JPA_REPOSITORY),
                ModificationTrackerManager.getInstance(project).getLibraryTracker()
            )
        }
    }

    companion object {
        fun getInstance(project: Project): SpringDataPackageScanService = project.service()
    }
}
