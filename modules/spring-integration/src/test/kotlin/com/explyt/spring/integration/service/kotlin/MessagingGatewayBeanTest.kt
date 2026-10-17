/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.integration.service.kotlin

import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope

private const val MESSAGING_GATEWAY = "org.springframework.integration.annotation.MessagingGateway"
private const val INTEGRATION_COMPONENT_SCAN = "org.springframework.integration.annotation.IntegrationComponentScan"
private const val ORDER_GATEWAY = "com.app.OrderGateway"
private const val OTHER_GATEWAY = "com.other.OtherGateway"

class MessagingGatewayBeanIntegration6Test : MessagingGatewayBeanTest() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springIntegrationCore_6_5_0, TestLibrary.springBootAutoConfigure_3_5_0
    )
    override val integrationAutoConfiguration =
        "org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration"
}

class MessagingGatewayBeanIntegration7Test : MessagingGatewayBeanTest() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springIntegrationCore_7_1_0,
        TestLibrary.springBootAutoConfigure_4_1_0,
        TestLibrary.springBootIntegration_4_1_0
    )
    override val integrationAutoConfiguration =
        "org.springframework.boot.integration.autoconfigure.IntegrationAutoConfiguration"
}

abstract class MessagingGatewayBeanTest : ExplytKotlinLightTestCase() {

    abstract val integrationAutoConfiguration: String

    override fun setUp() {
        super.setUp()
        Registry.get("explyt.spring.root.runConfiguration").setValue(false)
    }

    override fun tearDown() {
        super.tearDown()
        Registry.get("explyt.spring.root.runConfiguration").resetToDefault()
    }

    fun testIntegrationComponentScanRegistersGatewayUnderItsNameAttribute() {
        addApplication("com.app", "@EnableIntegration @IntegrationComponentScan")
        addGateway(ORDER_GATEWAY, "@MessagingGateway(name = \"orders\")")
        assertGatewayFixture(ORDER_GATEWAY)

        assertBeanNames(ORDER_GATEWAY, setOf("orders"))
    }

    fun testIntegrationComponentScanRegistersGatewayUnderDefaultBeanName() {
        addApplication("com.app", "@EnableIntegration @IntegrationComponentScan")
        addGateway(ORDER_GATEWAY, "@MessagingGateway")
        assertGatewayFixture(ORDER_GATEWAY)

        assertBeanNames(ORDER_GATEWAY, setOf("orderGateway"))
    }

    fun testGatewayOutsideIntegrationComponentScanPackagesIsNotBean() {
        addApplication("com", "@EnableIntegration @IntegrationComponentScan(basePackages = [\"com.app\"])")
        addGateway(ORDER_GATEWAY, "@MessagingGateway")
        addGateway(OTHER_GATEWAY, "@MessagingGateway")
        assertGatewayFixture(OTHER_GATEWAY)

        assertBeanNames(ORDER_GATEWAY, setOf("orderGateway"))
        assertBeanNames(OTHER_GATEWAY, emptySet())
    }

    fun testIntegrationComponentScanBasePackageClassesBoundsGateways() {
        myFixture.addFileToProject("com/app/Marker.kt", "package com.app\n\nclass Marker")
        addApplication(
            "com",
            "@EnableIntegration @IntegrationComponentScan(basePackageClasses = [com.app.Marker::class])"
        )
        addGateway(ORDER_GATEWAY, "@MessagingGateway")
        addGateway(OTHER_GATEWAY, "@MessagingGateway")
        assertGatewayFixture(OTHER_GATEWAY)

        assertBeanNames(ORDER_GATEWAY, setOf("orderGateway"))
        assertBeanNames(OTHER_GATEWAY, emptySet())
    }

    fun testBootIntegrationAutoConfigurationRegistersGatewayInApplicationPackage() {
        myFixture.addFileToProject(
            "com/app/App.kt", """
            package com.app

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class App
            """.trimIndent()
        )
        addGateway(ORDER_GATEWAY, "@MessagingGateway")
        assertGatewayFixture(ORDER_GATEWAY)
        findClass(integrationAutoConfiguration, GlobalSearchScope.allScope(project))

        assertBeanNames(ORDER_GATEWAY, setOf("orderGateway"))
    }

    fun testEnableIntegrationAloneRegistersNoGateway() {
        addApplication("com.app", "@EnableIntegration")
        addGateway(ORDER_GATEWAY, "@MessagingGateway")
        assertGatewayFixture(ORDER_GATEWAY)

        assertBeanNames(ORDER_GATEWAY, emptySet())
    }

    private fun addApplication(packageName: String, annotations: String) {
        myFixture.addFileToProject(
            "${packageName.replace('.', '/')}/App.kt", """
            package $packageName

            import org.springframework.context.annotation.ComponentScan
            import org.springframework.context.annotation.Configuration
            import org.springframework.integration.annotation.IntegrationComponentScan
            import org.springframework.integration.config.EnableIntegration

            @Configuration
            @ComponentScan
            $annotations
            class App
            """.trimIndent()
        )
    }

    private fun addGateway(qualifiedName: String, annotation: String) {
        val packageName = qualifiedName.substringBeforeLast('.')
        val simpleName = qualifiedName.substringAfterLast('.')
        myFixture.addFileToProject(
            "${qualifiedName.replace('.', '/')}.kt", """
            package $packageName

            import org.springframework.integration.annotation.MessagingGateway

            $annotation
            interface $simpleName {
                fun send(order: String)
            }
            """.trimIndent()
        )
    }

    private fun assertGatewayFixture(gatewayName: String) {
        findClass(MESSAGING_GATEWAY, GlobalSearchScope.allScope(project))
        findClass(INTEGRATION_COMPONENT_SCAN, GlobalSearchScope.allScope(project))
        val gateway = findClass(gatewayName, GlobalSearchScope.projectScope(project))
        assertTrue("$gatewayName must be an interface", gateway.isInterface)
        assertNotNull("$gatewayName must carry a resolved @MessagingGateway", gateway.getAnnotation(MESSAGING_GATEWAY))
    }

    private fun assertBeanNames(qualifiedName: String, expected: Set<String>) {
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
        val actual = beans.filter { it.psiClass.qualifiedName == qualifiedName }.map { it.name }.toSet()
        assertEquals("bean names of $qualifiedName", expected, actual)
    }

    private fun findClass(qualifiedName: String, scope: GlobalSearchScope): PsiClass {
        val psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName, scope)
        assertNotNull("$qualifiedName must resolve", psiClass)
        return psiClass!!
    }
}
