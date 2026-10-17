/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.util.SpringBootUtil
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.conditional.kotlin.KotlinConditionFixtures
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.addFromMaven
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope

abstract class WebApplicationTypeVerdictTestCase : BeanConditionTestCase() {

    private val kotlin: Boolean get() = fixtures is KotlinConditionFixtures

    override fun setUp() {
        super.setUp()
        addWebProbes()
    }

    protected fun assertActive(probe: String) {
        assertEquals("Verdict of $probe", ConditionVerdict.Active, verdictOf(probe))
        assertTrue("$probe must be active", key(probe) in activeBeans())
    }

    protected fun assertInactive(probe: String, annotationFqn: String) {
        val verdict = verdictOf(probe)
        assertInstanceOf(verdict, ConditionVerdict.Inactive::class.java)
        val evidence = (verdict as ConditionVerdict.Inactive).condition
        assertEquals(annotationFqn, evidence.annotationFqn)
        assertEquals(ConditionReason.NOT_MATCHED, evidence.reason)
        assertTrue("$probe must be excluded", key(probe) in excludedBeans())
    }

    protected fun assertUndecided(probe: String, annotationFqn: String) {
        val verdict = verdictOf(probe)
        assertInstanceOf(verdict, ConditionVerdict.Undecided::class.java)
        val evidence = (verdict as ConditionVerdict.Undecided).conditions.single()
        assertEquals(annotationFqn, evidence.annotationFqn)
        assertEquals(ConditionReason.PROPERTY_UNRESOLVABLE, evidence.reason)
        assertTrue("$probe must stay active", key(probe) in activeBeans())
    }

    protected fun assertClasspath(present: Collection<String>, absent: Collection<String>) {
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        val facade = JavaPsiFacade.getInstance(project)
        for (fqn in present) assertNotNull("Precondition: $fqn must be on the classpath", facade.findClass(fqn, scope))
        for (fqn in absent) assertNull("Precondition: $fqn must be absent from the classpath", facade.findClass(fqn, scope))
    }

    protected fun assertWebApplicationTypeOverride(value: String) {
        addProperties("$WEB_APPLICATION_TYPE=$value")
        assertPropertyDefined(WEB_APPLICATION_TYPE)
    }

    private fun verdictOf(probe: String): ConditionVerdict? {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        return SpringSearchService.getInstance(project).conditionVerdictOf(beanMethod(WEB_PROBES, probe), module)
    }

    private fun key(probe: String) = "$WEB_PROBES#$probe"

    private fun addWebProbes() {
        if (kotlin) addKotlinProbes() else addJavaProbes()
        ModificationTrackerManager.getInstance(project).invalidateAll()
        assertAnnotatedBy(beanMethod(WEB_PROBES, SERVLET_BEAN), CONDITIONAL_ON_WEB_APPLICATION)
        assertAnnotatedBy(beanMethod(WEB_PROBES, REACTIVE_BEAN), CONDITIONAL_ON_WEB_APPLICATION)
        assertAnnotatedBy(beanMethod(WEB_PROBES, ANY_WEB_BEAN), CONDITIONAL_ON_WEB_APPLICATION)
        assertAnnotatedBy(beanMethod(WEB_PROBES, NOT_WEB_BEAN), CONDITIONAL_ON_NOT_WEB_APPLICATION)
    }

    private fun addJavaProbes() {
        myFixture.addFileToProject("com/app/Probe.java", "package com.app;\npublic class Probe {}")
        myFixture.addFileToProject(
            "com/app/WebProbes.java",
            """
            package com.app;

            import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
            import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            public class WebProbes {
                @Bean
                @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
                public Probe $SERVLET_BEAN() { return new Probe(); }

                @Bean
                @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
                public Probe $REACTIVE_BEAN() { return new Probe(); }

                @Bean
                @ConditionalOnWebApplication
                public Probe $ANY_WEB_BEAN() { return new Probe(); }

                @Bean
                @ConditionalOnNotWebApplication
                public Probe $NOT_WEB_BEAN() { return new Probe(); }
            }
            """.trimIndent()
        )
    }

    private fun addKotlinProbes() {
        myFixture.addFileToProject("com/app/Probe.kt", "package com.app\nclass Probe")
        myFixture.addFileToProject(
            "com/app/WebProbes.kt",
            """
            package com.app

            import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication
            import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration

            @Configuration
            class WebProbes {
                @Bean
                @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
                fun $SERVLET_BEAN() = Probe()

                @Bean
                @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
                fun $REACTIVE_BEAN() = Probe()

                @Bean
                @ConditionalOnWebApplication
                fun $ANY_WEB_BEAN() = Probe()

                @Bean
                @ConditionalOnNotWebApplication
                fun $NOT_WEB_BEAN() = Probe()
            }
            """.trimIndent()
        )
    }

    companion object {
        const val WEB_PROBES = "com.app.WebProbes"
        const val SERVLET_BEAN = "servletBean"
        const val REACTIVE_BEAN = "reactiveBean"
        const val ANY_WEB_BEAN = "anyWebBean"
        const val NOT_WEB_BEAN = "notWebBean"

        const val WEB_APPLICATION_TYPE = "spring.main.web-application-type"
        const val CONDITIONAL_ON_WEB_APPLICATION =
            "org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication"
        const val CONDITIONAL_ON_NOT_WEB_APPLICATION =
            "org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication"

        const val DISPATCHER_SERVLET = "org.springframework.web.servlet.DispatcherServlet"
        const val SERVLET_API = "jakarta.servlet.Servlet"
        const val CONFIGURABLE_WEB_CONTEXT = "org.springframework.web.context.ConfigurableWebApplicationContext"
        const val GENERIC_WEB_CONTEXT = "org.springframework.web.context.support.GenericWebApplicationContext"
        const val DISPATCHER_HANDLER = "org.springframework.web.reactive.DispatcherHandler"
        const val HANDLER_RESULT = "org.springframework.web.reactive.HandlerResult"
        const val BOOT_4_DEDUCER = "org.springframework.boot.WebApplicationType.Deducer"
        const val BOOT_4_WEBFLUX_DEDUCER = "org.springframework.boot.webflux.WebFluxWebApplicationTypeDeducer"

        val servletApi = TestLibrary("jakarta.servlet:jakarta.servlet-api:6.0.0")
        val springWebFlux_7_0_9 = TestLibrary("org.springframework:spring-webflux:7.0.9")
    }
}

abstract class ServletAndWebFluxWebApplicationTypeTestCase : WebApplicationTypeVerdictTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springReactiveWeb_3_1_1, TestLibrary.springWebMvc_6_0_7, servletApi
    )

    override fun setUp() {
        super.setUp()
        assertClasspath(
            present = listOf(DISPATCHER_SERVLET, SERVLET_API, CONFIGURABLE_WEB_CONTEXT, DISPATCHER_HANDLER, HANDLER_RESULT),
            absent = emptyList()
        )
    }

    fun testServletBeanIsActive() = assertActive(SERVLET_BEAN)

    fun testAnyWebBeanIsActive() = assertActive(ANY_WEB_BEAN)

    fun testReactiveBeanIsInactiveBecauseTheApplicationRunsAsServlet() =
        assertInactive(REACTIVE_BEAN, CONDITIONAL_ON_WEB_APPLICATION)

    fun testNotWebBeanIsInactive() = assertInactive(NOT_WEB_BEAN, CONDITIONAL_ON_NOT_WEB_APPLICATION)
}

abstract class SpringWebOnlyWebApplicationTypeTestCase : WebApplicationTypeVerdictTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1, TestLibrary.springWeb_6_0_7
    )

    override fun setUp() {
        super.setUp()
        assertClasspath(
            present = listOf(GENERIC_WEB_CONTEXT, CONFIGURABLE_WEB_CONTEXT),
            absent = listOf(DISPATCHER_SERVLET, DISPATCHER_HANDLER, HANDLER_RESULT)
        )
    }

    fun testServletBeanIsInactiveWithoutDispatcherServlet() =
        assertInactive(SERVLET_BEAN, CONDITIONAL_ON_WEB_APPLICATION)

    fun testReactiveBeanIsInactive() = assertInactive(REACTIVE_BEAN, CONDITIONAL_ON_WEB_APPLICATION)

    fun testAnyWebBeanIsInactive() = assertInactive(ANY_WEB_BEAN, CONDITIONAL_ON_WEB_APPLICATION)

    fun testNotWebBeanIsActive() = assertActive(NOT_WEB_BEAN)
}

abstract class ReactiveOnlyWebApplicationTypeTestCase : WebApplicationTypeVerdictTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springReactiveWeb_3_1_1)

    override fun setUp() {
        super.setUp()
        assertClasspath(
            present = listOf(DISPATCHER_HANDLER, HANDLER_RESULT, GENERIC_WEB_CONTEXT),
            absent = listOf(DISPATCHER_SERVLET, SERVLET_API)
        )
    }

    fun testReactiveBeanIsActive() = assertActive(REACTIVE_BEAN)

    fun testAnyWebBeanIsActive() = assertActive(ANY_WEB_BEAN)

    fun testServletBeanIsInactive() = assertInactive(SERVLET_BEAN, CONDITIONAL_ON_WEB_APPLICATION)

    fun testNotWebBeanIsInactive() = assertInactive(NOT_WEB_BEAN, CONDITIONAL_ON_NOT_WEB_APPLICATION)
}

abstract class ServletOnlyWebApplicationTypeTestCase : WebApplicationTypeVerdictTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1, TestLibrary.springWebMvc_6_0_7, servletApi
    )

    override fun setUp() {
        super.setUp()
        assertClasspath(
            present = listOf(DISPATCHER_SERVLET, SERVLET_API, CONFIGURABLE_WEB_CONTEXT),
            absent = listOf(DISPATCHER_HANDLER, HANDLER_RESULT)
        )
    }

    fun testServletBeanIsActive() = assertActive(SERVLET_BEAN)

    fun testAnyWebBeanIsActive() = assertActive(ANY_WEB_BEAN)

    fun testReactiveBeanIsInactive() = assertInactive(REACTIVE_BEAN, CONDITIONAL_ON_WEB_APPLICATION)

    fun testNotWebBeanIsInactive() = assertInactive(NOT_WEB_BEAN, CONDITIONAL_ON_NOT_WEB_APPLICATION)

    fun testNoneOverrideExcludesServletBean() {
        assertWebApplicationTypeOverride("none")

        assertInactive(SERVLET_BEAN, CONDITIONAL_ON_WEB_APPLICATION)
    }

    fun testNoneOverrideExcludesAnyWebBean() {
        assertWebApplicationTypeOverride("none")

        assertInactive(ANY_WEB_BEAN, CONDITIONAL_ON_WEB_APPLICATION)
    }

    fun testNoneOverrideActivatesNotWebBean() {
        assertWebApplicationTypeOverride("none")

        assertActive(NOT_WEB_BEAN)
    }

    fun testUnresolvableOverrideLeavesServletBeanUndecided() {
        assertWebApplicationTypeOverride("\${WEB_TYPE}")
        assertPropertyUndefined("WEB_TYPE")

        assertUndecided(SERVLET_BEAN, CONDITIONAL_ON_WEB_APPLICATION)
    }

    fun testUnresolvableOverrideLeavesNotWebBeanUndecided() {
        assertWebApplicationTypeOverride("\${WEB_TYPE}")
        assertPropertyUndefined("WEB_TYPE")

        assertUndecided(NOT_WEB_BEAN, CONDITIONAL_ON_NOT_WEB_APPLICATION)
    }
}

abstract class Boot4ReactiveWithoutBootWebFluxWebApplicationTypeTestCase : WebApplicationTypeVerdictTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_4_1_0, springWebFlux_7_0_9
    )

    override fun setUp() {
        super.setUp()
        assertClasspath(
            present = listOf(BOOT_4_DEDUCER, DISPATCHER_HANDLER, HANDLER_RESULT),
            absent = listOf(BOOT_4_WEBFLUX_DEDUCER, DISPATCHER_SERVLET, SERVLET_API)
        )
    }

    fun testReactiveBeanIsInactiveWithoutBootWebFlux() = assertInactive(REACTIVE_BEAN, CONDITIONAL_ON_WEB_APPLICATION)

    fun testNotWebBeanIsActive() = assertActive(NOT_WEB_BEAN)
}

abstract class Boot4ReactiveWebApplicationTypeTestCase : WebApplicationTypeVerdictTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_4_1_0, springWebFlux_7_0_9, TestLibrary.springBootWebFlux_4_1_1
    )

    override fun setUp() {
        super.setUp()
        assertClasspath(
            present = listOf(BOOT_4_DEDUCER, BOOT_4_WEBFLUX_DEDUCER, DISPATCHER_HANDLER, HANDLER_RESULT),
            absent = listOf(DISPATCHER_SERVLET, SERVLET_API)
        )
    }

    fun testReactiveBeanIsActive() = assertActive(REACTIVE_BEAN)

    fun testNotWebBeanIsInactive() = assertInactive(NOT_WEB_BEAN, CONDITIONAL_ON_NOT_WEB_APPLICATION)
}

abstract class RuntimeServletWebApplicationTypeTestCase : WebApplicationTypeVerdictTestCase() {
    override fun setUp() {
        super.setUp()
        ModuleRootModificationUtil.updateModel(module) { model ->
            for (library in listOf(TestLibrary.springWebMvc_6_0_7, TestLibrary.jakartaServlet_6_0_0)) {
                addFromMaven(
                    model, library.mavenCoordinates, library.includeTransitiveDependencies,
                    dependencyScope = DependencyScope.RUNTIME
                )
            }
        }
        IndexingTestUtil.waitUntilIndexesAreReady(project)
        ModificationTrackerManager.getInstance(project).invalidateAll()
        val facade = JavaPsiFacade.getInstance(project)
        val compileScope = GlobalSearchScope.moduleWithLibrariesScope(module)
        val runtimeScope = module.getModuleRuntimeScope(false)
        for (fqn in listOf(DISPATCHER_SERVLET, SERVLET_API, CONFIGURABLE_WEB_CONTEXT)) {
            assertNull("Precondition: $fqn is absent from the compile classpath", facade.findClass(fqn, compileScope))
            assertNotNull("Precondition: $fqn is on the runtime classpath", facade.findClass(fqn, runtimeScope))
        }
        assertNull("Precondition: no runtime WebFlux", facade.findClass(DISPATCHER_HANDLER, runtimeScope))
    }

    fun testRuntimeServletBeanIsActive() = assertActive(SERVLET_BEAN)

    fun testRuntimeNotWebBeanIsInactive() = assertInactive(NOT_WEB_BEAN, CONDITIONAL_ON_NOT_WEB_APPLICATION)
}

abstract class Boot3JavaxServletWebApplicationTypeTestCase : WebApplicationTypeVerdictTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1, TestLibrary.springWeb_6_0_7, TestLibrary.javaxServlet_4_0_1
    )

    override fun setUp() {
        super.setUp()
        assertClasspath(
            present = listOf("javax.servlet.Servlet", CONFIGURABLE_WEB_CONTEXT, GENERIC_WEB_CONTEXT),
            absent = listOf(SERVLET_API, DISPATCHER_SERVLET, DISPATCHER_HANDLER, BOOT_4_DEDUCER)
        )
        val bootVersion = beanClass("org.springframework.boot.SpringBootVersion")
            .findMethodsByName("getVersion", false).single()
        val bootJar = bootVersion.containingFile.virtualFile.path
        assertTrue("Precondition: Boot 3.1.1 jar, got $bootJar", "spring-boot-3.1.1.jar" in bootJar)
    }

    fun testBoot3JavaxServletBeanIsInactive() = assertInactive(SERVLET_BEAN, CONDITIONAL_ON_WEB_APPLICATION)

    fun testBoot3JavaxNotWebBeanIsActive() = assertActive(NOT_WEB_BEAN)
}

abstract class Boot2JavaxServletWebApplicationTypeTestCase : WebApplicationTypeVerdictTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_2_7_18, springWeb_5_3_39, springContext_5_3_39,
        TestLibrary.javaxServlet_4_0_1
    )

    override fun setUp() {
        super.setUp()
        assertEquals("Precondition: Boot 2.7.18", "2.7.18", SpringBootUtil.getSpringBootVersion(module))
        assertClasspath(
            present = listOf("javax.servlet.Servlet", CONFIGURABLE_WEB_CONTEXT, GENERIC_WEB_CONTEXT),
            absent = listOf(SERVLET_API, DISPATCHER_SERVLET, DISPATCHER_HANDLER, HANDLER_RESULT, BOOT_4_DEDUCER)
        )
    }

    fun testBoot2JavaxServletBeanIsActive() = assertActive(SERVLET_BEAN)

    fun testBoot2JavaxNotWebBeanIsInactive() = assertInactive(NOT_WEB_BEAN, CONDITIONAL_ON_NOT_WEB_APPLICATION)

    fun testBoot2JavaxAnyWebBeanIsActive() = assertActive(ANY_WEB_BEAN)

    companion object {
        val springWeb_5_3_39 = TestLibrary("org.springframework:spring-web:5.3.39")
        val springContext_5_3_39 = TestLibrary("org.springframework:spring-context:5.3.39")
    }
}
