/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.mybatis.service.beans.discoverer

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.RootDataHolder
import com.explyt.spring.core.service.beans.discoverer.AdditionalBeansDiscoverer
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanName
import com.explyt.spring.mybatis.SpringMybatisClasses
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch

class SpringMybatisAdditionalBeansDiscoverer : AdditionalBeansDiscoverer() {

    override fun accepts(module: Module): Boolean =
        LibraryClassCache.searchForLibraryClass(module, SpringMybatisClasses.MAPPER_SCAN) != null

    override fun discoverBeans(module: Module): Collection<PsiBean> = emptyList()

    override fun getExtraComponents(module: Module): Collection<PsiBean> {
        val scope = module.moduleWithDependenciesScope
        return mapperScans(module).ifEmpty { listOfNotNull(bootAutoConfiguredScan(module)) }
            .asSequence()
            .flatMap { scan -> scan.mappers(module, scope) }
            .distinct()
            .map { PsiBean(name = it.resolveBeanName(module), psiClass = it, psiMember = it) }
            .toList()
    }

    override fun additionalFilterBeans(module: Module, bean: PsiBean, rootDataHolder: RootDataHolder): Boolean = true

    private fun mapperScans(module: Module): List<MapperScan> {
        val repeated = declaringAnnotations(module, SpringMybatisClasses.MAPPER_SCANS).flatMap { container ->
            container.annotation.attributeValues("value").filterIsInstance<PsiAnnotation>()
                .map { DeclaringAnnotation(container.declaringClass, it) }
        }
        return (declaringAnnotations(module, SpringMybatisClasses.MAPPER_SCAN) + repeated)
            .map { it.toMapperScan(module) }
    }

    private fun bootAutoConfiguredScan(module: Module): MapperScan? {
        LibraryClassCache.searchForLibraryClass(module, SpringMybatisClasses.MYBATIS_AUTO_CONFIGURATION) ?: return null
        val packages = listOf(SpringMybatisClasses.SPRING_BOOT_APPLICATION, SpringMybatisClasses.ENABLE_AUTO_CONFIGURATION)
            .flatMapTo(mutableSetOf()) { annotation ->
                declaringAnnotations(module, annotation).map { it.declaringClass.packageName() }
            }
        if (packages.isEmpty()) return null
        return MapperScan(packages, annotationFqns = annotationTypesOf(module, SpringMybatisClasses.MAPPER), markerInterface = null)
    }

    private fun declaringAnnotations(module: Module, annotationFqn: String): List<DeclaringAnnotation> {
        val annotationClass = LibraryClassCache.searchForLibraryClass(module, annotationFqn) ?: return emptyList()
        return AnnotatedElementsSearch.searchPsiClasses(annotationClass, module.moduleWithDependenciesScope)
            .mapNotNull { declaringClass ->
                declaringClass.getAnnotation(annotationFqn)?.let { DeclaringAnnotation(declaringClass, it) }
            }
    }

    private fun annotationTypesOf(module: Module, annotationFqn: String): Set<String> =
        MetaAnnotationUtil.getAnnotationTypesWithChildren(module, annotationFqn, false)
            .mapNotNullTo(mutableSetOf()) { it.qualifiedName }

    private inner class DeclaringAnnotation(val declaringClass: PsiClass, val annotation: PsiAnnotation) {

        fun toMapperScan(module: Module): MapperScan {
            val declared = annotation.strings("value") + annotation.strings("basePackages") +
                    annotation.classLiterals("basePackageClasses").map { it.packageName() }
            val annotationFqns = annotation.classLiterals("annotationClass")
                .mapNotNull { it.qualifiedName }
                .filterNot { it == JAVA_ANNOTATION }
                .flatMapTo(mutableSetOf()) { annotationTypesOf(module, it) }
            val markerInterface = annotation.classLiterals("markerInterface")
                .firstOrNull { it.qualifiedName != JAVA_CLASS }
            return MapperScan(
                packages = declared.ifEmpty { listOf(declaringClass.packageName()) }.toSet(),
                annotationFqns = annotationFqns,
                markerInterface = markerInterface
            )
        }
    }

    private class MapperScan(val packages: Set<String>, val annotationFqns: Set<String>, val markerInterface: PsiClass?) {

        fun mappers(module: Module, scope: GlobalSearchScope): Sequence<PsiClass> {
            val facade = JavaPsiFacade.getInstance(module.project)
            return packages.asSequence()
                .mapNotNull { facade.findPackage(it) }
                .flatMap { it.interfacesWithSubPackages(scope) }
                .filter { isIncluded(it) }
        }

        private fun isIncluded(psiClass: PsiClass): Boolean {
            if (annotationFqns.isEmpty() && markerInterface == null) return true
            return annotationFqns.any { psiClass.hasAnnotation(it) } ||
                    markerInterface?.let { psiClass.isInheritor(it, true) } == true
        }

        private fun PsiPackage.interfacesWithSubPackages(scope: GlobalSearchScope): Sequence<PsiClass> = sequence {
            ProgressManager.checkCanceled()
            yieldAll(getClasses(scope).filter { it.isInterface && !it.isAnnotationType })
            getSubPackages(scope).forEach { yieldAll(it.interfacesWithSubPackages(scope)) }
        }
    }

    private companion object {
        const val JAVA_ANNOTATION = "java.lang.annotation.Annotation"
        const val JAVA_CLASS = "java.lang.Class"
    }
}

private fun PsiClass.packageName(): String = (containingFile as? PsiClassOwner)?.packageName.orEmpty()

private fun PsiAnnotation.strings(attribute: String): List<String> =
    attributeValues(attribute).mapNotNull {
        JavaPsiFacade.getInstance(project).constantEvaluationHelper.computeConstantExpression(it) as? String
    }

private fun PsiAnnotation.classLiterals(attribute: String): List<PsiClass> =
    attributeValues(attribute).mapNotNull {
        ((it as? PsiClassObjectAccessExpression)?.operand?.type as? PsiClassType)?.resolve()
    }

private fun PsiAnnotation.attributeValues(attribute: String): List<PsiAnnotationMemberValue> =
    when (val value = findDeclaredAttributeValue(attribute)) {
        null -> emptyList()
        is PsiArrayInitializerMemberValue -> value.initializers.toList()
        else -> listOf(value)
    }
