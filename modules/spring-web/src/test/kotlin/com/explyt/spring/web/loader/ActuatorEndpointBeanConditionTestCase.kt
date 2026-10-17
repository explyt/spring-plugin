/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.conditional.ConditionEvidence
import com.explyt.spring.core.service.conditional.ConditionReason
import com.explyt.spring.core.service.conditional.ConditionVerdict
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytBaseLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType

abstract class ActuatorEndpointBeanConditionTestCase : ExplytBaseLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootActuatorAutoConfigure_4_1_0,
        TestLibrary.springBootHealth_4_1_0,
        TestLibrary.springWeb_6_0_7
    )

    protected abstract fun addApplication()
    protected abstract fun addPropertyGatedComponentEndpoint()
    protected abstract fun addUnconditionalComponentEndpoint()
    protected abstract fun addEndpointRegisteredByGatedConfiguration()
    protected abstract fun addRestController()
    protected abstract fun addSupertypeFactoryEndpoint()
    protected abstract fun addDuplicateEndpointRegistrations()
    protected abstract fun addPropertyGatedWebExtension()
    protected abstract fun addXEndpoint()
    protected abstract fun addXWebExtension(packageName: String, annotations: String)

    override fun setUp() {
        super.setUp()
        addApplication()
        val annotation = JavaPsiFacade.getInstance(project).findClass(CONDITIONAL_ON_PROPERTY, GlobalSearchScope.allScope(project))
        assertNotNull("Precondition: $CONDITIONAL_ON_PROPERTY comes from the fixture libraries", annotation)
    }

    fun testPropertyGatedEndpointWithoutPropertyIsListedExposedAndInactive() {
        addProperties(EXPOSE_ALL)
        addPropertyGatedComponentEndpoint()
        assertBeanModelSays<ConditionVerdict.Inactive>(projectClass(PS_ETL))

        val psEtl = endpointsOf(PS_ETL)

        assertEquals(listOf("/actuator/psEtl"), psEtl.map { it.path }.distinct())
        assertEquals(EndpointExposure.EXPOSED, psEtl.map { it.exposure }.distinct().single())
        val evidence = inactiveEvidence(beanConditionOf(PS_ETL))
        assertEquals(CONDITIONAL_ON_PROPERTY, evidence.annotationFqn)
        assertEquals(PS_ETL, evidence.carrierFqn)
        assertEquals(ConditionReason.NOT_MATCHED, evidence.reason)
        assertTrue("detail names the gating key: ${evidence.detail}", evidence.detail.orEmpty().contains(GATING_KEY))
    }

    fun testPropertyGatedEndpointWithMatchingPropertyIsActive() {
        addProperties(EXPOSE_ALL, "$GATING_KEY=true")
        addPropertyGatedComponentEndpoint()
        assertBeanModelSays<ConditionVerdict.Active>(projectClass(PS_ETL))

        assertEquals(ConditionVerdict.Active, beanConditionOf(PS_ETL))
    }

    fun testUnresolvablePlaceholderLeavesTheEndpointUndecided() {
        addProperties(EXPOSE_ALL, "$GATING_KEY=\${CH_ENABLED}")
        addPropertyGatedComponentEndpoint()
        assertBeanModelSays<ConditionVerdict.Undecided>(projectClass(PS_ETL))

        val verdict = beanConditionOf(PS_ETL)

        assertInstanceOf(verdict, ConditionVerdict.Undecided::class.java)
        val evidence = (verdict as ConditionVerdict.Undecided).conditions.single()
        assertEquals(CONDITIONAL_ON_PROPERTY, evidence.annotationFqn)
        assertEquals(ConditionReason.PROPERTY_UNRESOLVABLE, evidence.reason)
    }

    fun testEndpointRegisteredByGatedConfigurationIsInactiveOnTheConfiguration() {
        addProperties(EXPOSE_ALL)
        addEndpointRegisteredByGatedConfiguration()
        val factory = projectClass(CLICKHOUSE_CONFIG).findMethodsByName("chEtlEndpoint", false).single()
        assertFalse(
            "Precondition: the endpoint class carries no condition of its own",
            projectClass(CH_ETL).modifierList!!.hasAnnotation(CONDITIONAL_ON_PROPERTY)
        )
        assertBeanModelSays<ConditionVerdict.Inactive>(factory)

        val evidence = inactiveEvidence(beanConditionOf(CH_ETL))

        assertEquals(CONDITIONAL_ON_PROPERTY, evidence.annotationFqn)
        assertEquals(CLICKHOUSE_CONFIG, evidence.carrierFqn)
        assertTrue("detail names the gating key: ${evidence.detail}", evidence.detail.orEmpty().contains(GATING_KEY))
    }

    fun testUnconditionalEndpointIsActive() {
        addProperties(EXPOSE_ALL)
        addUnconditionalComponentEndpoint()
        assertBeanModelSays<ConditionVerdict.Active>(projectClass(CACHE_STATS))

        assertEquals(ConditionVerdict.Active, beanConditionOf(CACHE_STATS))
    }

    fun testBuiltInEndpointCarriesNoBeanCondition() {
        addProperties(EXPOSE_ALL)
        addPropertyGatedComponentEndpoint()

        val health = endpointsOf(HEALTH_ENDPOINT)

        assertEquals(EndpointExposure.EXPOSED, health.map { it.exposure }.distinct().single())
        assertEquals(listOf(null), health.map { it.beanCondition }.distinct())
    }

    fun testNonActuatorEndpointCarriesNoBeanCondition() {
        addProperties(EXPOSE_ALL)
        addPropertyGatedComponentEndpoint()
        addRestController()

        val controllerEndpoints = endpointsOfType(EndpointType.SPRING_MVC).filter { it.containingClass?.qualifiedName == ETL_CONTROLLER }

        assertEquals(listOf("/etl/runs"), controllerEndpoints.map { it.path })
        assertEquals(listOf(null), controllerEndpoints.map { it.beanCondition }.distinct())
    }

    fun testSupertypeFactoryEndpointPinsCurrentNullCondition() {
        addProperties(EXPOSE_ALL)
        addSupertypeFactoryEndpoint()
        val factory = projectClass(SUPERTYPE_CONFIG).findMethodsByName("etlEndpoint", false).single()
        assertBeanModelSays<ConditionVerdict.Inactive>(factory)

        val endpoints = endpointsOf(SUPERTYPE_ENDPOINT)

        assertEquals(listOf("/actuator/etl"), endpoints.map { it.path })
        assertEquals(listOf(null), endpoints.map { it.beanCondition }.distinct())
    }

    fun testDuplicateRegistrationsExposeTheActiveCondition() {
        addProperties(EXPOSE_ALL)
        addDuplicateEndpointRegistrations()
        val verdicts = SpringSearchService.getInstance(project).conditionVerdicts(module)
            .filterKeys { it.psiClass.qualifiedName == DUPLICATE_ENDPOINT }
            .values
        assertEquals(2, verdicts.size)
        assertEquals(1, verdicts.count { it is ConditionVerdict.Active })
        assertEquals(1, verdicts.count { it is ConditionVerdict.Inactive })

        val endpoints = endpointsOf(DUPLICATE_ENDPOINT)

        assertEquals(listOf("/actuator/twice"), endpoints.map { it.path }.distinct())
        assertEquals(listOf(ConditionVerdict.Active), endpoints.map { it.beanCondition }.distinct())
    }

    fun testInactiveEndpointStaysInTheListing() {
        addProperties(EXPOSE_ALL)
        addPropertyGatedComponentEndpoint()
        addUnconditionalComponentEndpoint()

        val projectEndpoints = endpointsOfType(EndpointType.ACTUATOR)
            .filter { it.containingClass?.qualifiedName?.startsWith("com.app.") == true }

        assertEquals(setOf("/actuator/psEtl", "/actuator/cachestats"), projectEndpoints.map { it.path }.toSet())
        assertEquals(2, projectEndpoints.size)
    }

    fun testPropertyGatedWebExtensionWithoutPropertyLeavesTheEndpointOperation() {
        addProperties(EXPOSE_ALL)
        addPropertyGatedWebExtension()
        assertBeanModelSays<ConditionVerdict.Active>(projectClass(EXT_ENDPOINT))
        assertBeanModelSays<ConditionVerdict.Inactive>(projectClass(EXT_WEB_EXTENSION))

        assertEquals(listOf(EXT_ENDPOINT), operationOwnersAt(EXT_PATH))
        assertEquals(ConditionVerdict.Active, beanConditionOf(EXT_ENDPOINT))
    }

    fun testPropertyGatedWebExtensionWithFalsePropertyLeavesTheEndpointOperation() {
        addProperties(EXPOSE_ALL, "$EXT_GATING_KEY=false")
        addPropertyGatedWebExtension()
        assertBeanModelSays<ConditionVerdict.Active>(projectClass(EXT_ENDPOINT))
        assertBeanModelSays<ConditionVerdict.Inactive>(projectClass(EXT_WEB_EXTENSION))

        assertEquals(listOf(EXT_ENDPOINT), operationOwnersAt(EXT_PATH))
        assertEquals(ConditionVerdict.Active, beanConditionOf(EXT_ENDPOINT))
    }

    fun testPropertyGatedWebExtensionWithMatchingPropertyReplacesTheEndpointOperation() {
        addProperties(EXPOSE_ALL, "$EXT_GATING_KEY=true")
        addPropertyGatedWebExtension()
        assertBeanModelSays<ConditionVerdict.Active>(projectClass(EXT_ENDPOINT))
        assertBeanModelSays<ConditionVerdict.Active>(projectClass(EXT_WEB_EXTENSION))

        assertEquals(listOf(EXT_WEB_EXTENSION), operationOwnersAt(EXT_PATH))
        assertEquals(ConditionVerdict.Active, beanConditionOf(EXT_ENDPOINT))
    }

    fun testUnresolvablePlaceholderAppliesTheWebExtensionAndLeavesTheEndpointUndecided() {
        addProperties(EXPOSE_ALL, "$EXT_GATING_KEY=\${EXT_ENABLED}")
        addPropertyGatedWebExtension()
        assertBeanModelSays<ConditionVerdict.Active>(projectClass(EXT_ENDPOINT))
        assertBeanModelSays<ConditionVerdict.Undecided>(projectClass(EXT_WEB_EXTENSION))

        assertEquals(listOf(EXT_WEB_EXTENSION), operationOwnersAt(EXT_PATH))
        val verdict = beanConditionOf(EXT_ENDPOINT)
        assertInstanceOf(verdict, ConditionVerdict.Undecided::class.java)
        val evidence = (verdict as ConditionVerdict.Undecided).conditions.single()
        assertEquals(CONDITIONAL_ON_PROPERTY, evidence.annotationFqn)
        assertEquals(EXT_WEB_EXTENSION, evidence.carrierFqn)
        assertEquals(ConditionReason.PROPERTY_UNRESOLVABLE, evidence.reason)
    }

    fun testWebExtensionOutsideTheComponentScanLeavesTheEndpointOperation() {
        addProperties(EXPOSE_ALL)
        addXEndpoint()
        addXWebExtension(OUTSIDE_SCAN_PACKAGE, "@Component")
        assertDiscoveredWebExtensionIsNotABean(OUTSIDE_SCAN_EXTENSION)

        assertEquals(listOf(X_ENDPOINT), operationOwnersAt(X_PATH))
    }

    fun testWebExtensionWithoutStereotypeLeavesTheEndpointOperation() {
        addProperties(EXPOSE_ALL)
        addXEndpoint()
        addXWebExtension(APPLICATION_PACKAGE, "")
        assertDiscoveredWebExtensionIsNotABean(IN_SCAN_EXTENSION)

        assertEquals(listOf(X_ENDPOINT), operationOwnersAt(X_PATH))
    }

    fun testProfileInactiveWebExtensionLeavesTheEndpointOperation() {
        addProperties(EXPOSE_ALL)
        addXEndpoint()
        addXWebExtension(APPLICATION_PACKAGE, "@Component\n@Profile(\"dev\")")
        assertBeanModelSays<ConditionVerdict.Inactive>(projectClass(IN_SCAN_EXTENSION))

        assertEquals(listOf(X_ENDPOINT), operationOwnersAt(X_PATH))
    }

    fun testWebExtensionBeanInsideTheComponentScanReplacesTheEndpointOperation() {
        addProperties(EXPOSE_ALL)
        addXEndpoint()
        addXWebExtension(APPLICATION_PACKAGE, "@Component")
        assertBeanModelSays<ConditionVerdict.Active>(projectClass(IN_SCAN_EXTENSION))

        assertEquals(listOf(IN_SCAN_EXTENSION), operationOwnersAt(X_PATH))
    }

    private fun assertDiscoveredWebExtensionIsNotABean(qualifiedName: String) {
        val extension = projectClass(qualifiedName).toUElementOfType<UClass>()
        assertNotNull("Precondition: $qualifiedName is a UAST class", extension)
        assertTrue(
            "Precondition: $qualifiedName declares a web extension",
            ActuatorWebExtensions.isDeclaredOn(extension!!, module)
        )
        ModificationTrackerManager.getInstance(project).invalidateAll()
        val beans = SpringSearchService.getInstance(project).foundBeans(module)
        val beanClasses = (beans.active + beans.excluded).mapNotNull { it.psiClass.qualifiedName }.toSet()
        assertTrue("Precondition: $X_ENDPOINT is a bean among $beanClasses", X_ENDPOINT in beanClasses)
        assertFalse("Precondition: $qualifiedName is not a bean among $beanClasses", qualifiedName in beanClasses)
    }

    private fun addProperties(vararg lines: String) {
        myFixture.addFileToProject("application.properties", lines.joinToString("\n"))
    }

    private fun projectClass(qualifiedName: String) = myFixture.findClass(qualifiedName)

    private inline fun <reified T : ConditionVerdict> assertBeanModelSays(member: PsiMember) {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        val verdict = SpringSearchService.getInstance(project).conditionVerdictOf(member, module)
        assertInstanceOf(verdict, T::class.java)
    }

    private fun endpointsOfType(type: EndpointType): List<EndpointElement> {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        return SpringWebEndpointsLoader.EP_NAME.getExtensions(project).asSequence()
            .filter { it.getType() == type }
            .filter { it.isApplicable(module) }
            .flatMap { it.searchEndpoints(module) }
            .toList()
    }

    private fun endpointsOf(className: String): List<EndpointElement> {
        val endpoints = endpointsOfType(EndpointType.ACTUATOR)
        val matching = endpoints.filter { it.containingClass?.qualifiedName == className }
        assertFalse(
            "expected $className among ${endpoints.mapNotNull { it.containingClass?.qualifiedName }.distinct()}",
            matching.isEmpty()
        )
        return matching
    }

    private fun beanConditionOf(className: String): ConditionVerdict? =
        endpointsOf(className).map { it.beanCondition }.distinct().single()

    private fun operationOwnersAt(path: String): List<String?> =
        endpointsOfType(EndpointType.ACTUATOR).filter { it.path == path }
            .map { (it.psiElement as? PsiMethod)?.containingClass?.qualifiedName }

    private fun inactiveEvidence(verdict: ConditionVerdict?): ConditionEvidence {
        assertInstanceOf(verdict, ConditionVerdict.Inactive::class.java)
        return (verdict as ConditionVerdict.Inactive).condition
    }

    protected companion object {
        const val CONDITIONAL_ON_PROPERTY = "org.springframework.boot.autoconfigure.condition.ConditionalOnProperty"
        const val GATING_KEY = "clickhouse.enabled"
        const val EXPOSE_ALL = "management.endpoints.web.exposure.include=*"
        const val PS_ETL = "com.app.PsEtlEndpoint"
        const val CH_ETL = "com.app.ChEtlEndpoint"
        const val CLICKHOUSE_CONFIG = "com.app.ClickhouseEndpointConfig"
        const val CACHE_STATS = "com.app.CacheStatsEndpoint"
        const val ETL_CONTROLLER = "com.app.EtlController"
        const val HEALTH_ENDPOINT = "org.springframework.boot.health.actuate.endpoint.HealthEndpoint"
        const val SUPERTYPE_ENDPOINT = "com.app.EtlEndpoint"
        const val SUPERTYPE_CONFIG = "com.app.EtlEndpointConfig"
        const val DUPLICATE_ENDPOINT = "com.app.TwiceEndpoint"
        const val EXT_GATING_KEY = "x.ext.enabled"
        const val EXT_ENDPOINT = "com.app.ExtEtlEndpoint"
        const val EXT_WEB_EXTENSION = "com.app.ExtEtlWebExtension"
        const val EXT_PATH = "/actuator/extEtl"
        const val APPLICATION_PACKAGE = "com.app"
        const val OUTSIDE_SCAN_PACKAGE = "com.other"
        const val X_ENDPOINT = "com.app.XEndpoint"
        const val IN_SCAN_EXTENSION = "com.app.XWebExtension"
        const val OUTSIDE_SCAN_EXTENSION = "com.other.XWebExtension"
        const val X_PATH = "/actuator/x"
    }
}
