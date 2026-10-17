/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.integration.service.beans.discoverer

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.RootDataHolder
import com.explyt.spring.core.service.beans.discoverer.AdditionalBeansDiscoverer
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanName
import com.explyt.spring.integration.SpringIntegrationClasses
import com.explyt.spring.integration.util.SpringIntegrationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.*
import com.intellij.psi.search.searches.AnnotatedElementsSearch

class SpringIntegrationAdditionalBeansDiscoverer : AdditionalBeansDiscoverer() {

    override fun accepts(module: Module): Boolean {
        return SpringIntegrationUtil.isSpringIntegrationModule(module)
    }

    override fun discoverBeans(module: Module): Collection<PsiBean> {
        return listOfNotNull(
            getStaticBean(module, SpringIntegrationClasses.NULL_CHANNEL, "nullChannel"),
            getStaticBean(module, SpringIntegrationClasses.DIRECT_CHANNEL, "errorChannel"),
            getStaticBean(module, SpringIntegrationClasses.INTEGRATION_FLOW_CONTEXT, "integrationFlowContext")
        )
    }

    override fun ownsComponent(module: Module, psiClass: PsiClass): Boolean =
        psiClass.isGatewayOf(gatewayAnnotationTypes(module))

    override fun getExtraComponents(module: Module): Collection<PsiBean> {
        val gatewayPackages = gatewayScanPackages(module)
        if (gatewayPackages.isEmpty()) return emptyList()
        val gatewayAnnotationTypes = gatewayAnnotationTypes(module)
        return gatewayAnnotationTypes.asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, module.moduleWithDependenciesScope) }
            .distinct()
            .filter { it.isGatewayOf(gatewayAnnotationTypes) && it.isInAnyPackage(gatewayPackages) }
            .map { PsiBean(name = it.gatewayBeanName(module), psiClass = it, psiMember = it) }
            .toList()
    }

    private fun gatewayAnnotationTypes(module: Module): Collection<PsiClass> =
        MetaAnnotationUtil.getAnnotationTypesWithChildren(module, SpringIntegrationClasses.MESSAGING_GATEWAY, false)

    private fun PsiClass.isGatewayOf(gatewayAnnotationTypes: Collection<PsiClass>): Boolean =
        isInterface && gatewayAnnotationTypes.any { type -> type.qualifiedName?.let { hasAnnotation(it) } == true }

    override fun additionalFilterBeans(module: Module, bean: PsiBean, rootDataHolder: RootDataHolder): Boolean = true

    private fun gatewayScanPackages(module: Module): Set<String> =
        packagesDeclaredBy(module, SpringIntegrationClasses.INTEGRATION_COMPONENT_SCAN) { it.integrationScanPackages() }
            .ifEmpty { autoConfigurationPackages(module) }

    private fun autoConfigurationPackages(module: Module): Set<String> {
        val integrationAutoConfigured = SpringIntegrationClasses.INTEGRATION_AUTO_CONFIGURATIONS
            .any { LibraryClassCache.searchForLibraryClass(module, it) != null }
        if (!integrationAutoConfigured) return emptySet()
        return listOf(SpringIntegrationClasses.SPRING_BOOT_APPLICATION, SpringIntegrationClasses.ENABLE_AUTO_CONFIGURATION)
            .flatMapTo(mutableSetOf()) { annotation ->
                packagesDeclaredBy(module, annotation) { listOf(it.declaringClass.packageName()) }
            }
    }

    private fun packagesDeclaredBy(
        module: Module, annotationFqn: String, packagesOf: (DeclaringAnnotation) -> Collection<String>
    ): Set<String> {
        val annotationClass = LibraryClassCache.searchForLibraryClass(module, annotationFqn) ?: return emptySet()
        return AnnotatedElementsSearch.searchPsiClasses(annotationClass, module.moduleWithDependenciesScope)
            .flatMapTo(mutableSetOf()) { declaringClass ->
                val annotation = declaringClass.getAnnotation(annotationFqn) ?: return@flatMapTo emptyList()
                packagesOf(DeclaringAnnotation(declaringClass, annotation))
            }
    }

    private inner class DeclaringAnnotation(val declaringClass: PsiClass, val annotation: PsiAnnotation) {

        fun integrationScanPackages(): List<String> {
            val declared = annotation.strings("value") + annotation.strings("basePackages") +
                    annotation.classLiterals("basePackageClasses").map { it.packageName() }
            return declared.ifEmpty { listOf(declaringClass.packageName()) }
        }
    }


    private fun PsiClass.gatewayBeanName(module: Module): String =
        getAnnotation(SpringIntegrationClasses.MESSAGING_GATEWAY)?.strings("name")?.firstOrNull { it.isNotEmpty() }
            ?: resolveBeanName(module)

    private fun PsiClass.isInAnyPackage(packages: Set<String>): Boolean {
        val classPackage = packageName()
        return packages.any { it.isEmpty() || classPackage == it || classPackage.startsWith("$it.") }
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
}
