/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service

import com.explyt.spring.core.inspections.SpringBeanIncorrectAutowiringInspection
import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.util.ModuleUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.DependencyScope
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope

/**
 * Issue #780: a starter the application module reaches only at run time contributes beans to the running application,
 * so the static bean model of that application must contain them.
 *
 * Gradle puts a non-exported `implementation` jar of a dependency module, and a `runtimeOnly` jar, on the application's
 * runtime classpath only. `spring-tx` plays the starter: its library `@Configuration`
 * `ProxyTransactionManagementConfiguration` declares a `BeanFactoryTransactionAttributeSourceAdvisor` bean, which the
 * application injects by name through `org.springframework.aop.Advisor` from `spring-aop`, a jar on every compile classpath.
 */
abstract class RuntimeClasspathLibraryBeansTestCase : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.kotlin_1_9_22,
    )

    protected abstract val applicationFileName: String
    protected abstract val applicationText: String
    protected abstract val consumerFileName: String
    protected abstract val consumerText: String
    protected abstract val testConsumerFileName: String
    protected abstract val testConsumerText: String

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringBeanIncorrectAutowiringInspection::class.java)
    }

    fun testStarterOfADependencyModuleIsABeanSourceOfTheApplication() {
        val library = addDependencyModule("lib")
        addLibraries(library, listOf(TestLibrary.springTx_6_0_7), DependencyScope.COMPILE, exported = false)
        assertOnlyOnRuntimeClasspath(module)

        assertApplicationHasTheStarterBeans()
    }

    fun testRuntimeOnlyStarterIsABeanSourceOfTheApplication() {
        addLibraries(module, listOf(TestLibrary.springTx_6_0_7), DependencyScope.RUNTIME)
        assertOnlyOnRuntimeClasspath(module)

        assertApplicationHasTheStarterBeans()
    }

    fun testCompileClasspathStarterIsABeanSourceOfTheApplication() {
        addLibraries(module, listOf(TestLibrary.springTx_6_0_7), DependencyScope.COMPILE)
        assertNotNull(
            "Precondition: a compile dependency is on the compile classpath",
            findClass(STARTER_CONFIGURATION, ModuleUtil.getOnlyLibrarySearchScope(module))
        )

        assertApplicationHasTheStarterBeans()
    }

    fun testStarterOutsideTheApplicationClasspathIsNoBeanSource() {
        val other = addStandaloneModule("other")
        addLibraries(other, listOf(TestLibrary.springTx_6_0_7), DependencyScope.COMPILE)
        assertNull(
            "Precondition: a jar of an unrelated module is not on the application's runtime classpath",
            findClass(STARTER_CONFIGURATION, module.getModuleRuntimeScope(false))
        )

        writeApplication()

        assertFalse(
            "A bean of a jar outside the application classpath must not be active: ${activeBeanClasses()}",
            STARTER_ADVISOR in activeBeanClasses()
        )
        val problems = autowiringProblems(writeConsumer())
        assertEquals("The qualified advisor must be reported missing: $problems", 1, problems.size)
    }

    fun testTestScopeStarterIsABeanSourceOfTheTests() {
        addLibraries(module, listOf(TestLibrary.springTx_6_0_7), DependencyScope.TEST)
        assertOnlyOnTestClasspath(STARTER_CONFIGURATION)
        writeApplication()

        val active = activeBeanClasses()
        assertTrue("The test-scope starter's @Bean must be a bean the tests see: $active", STARTER_ADVISOR in active)
        val testConsumer = addTestSourceFileToModule(module, testConsumerFileName, testConsumerText)
        assertEquals(emptyList<String>(), autowiringProblems(testConsumer))
    }

    private fun assertOnlyOnTestClasspath(fqn: String) {
        assertNotNull(
            "Precondition: a test-scope jar is on the module's test classpath",
            findClass(fqn, GlobalSearchScope.moduleWithLibrariesScope(module))
        )
        assertNull(
            "Precondition: a test-scope jar is not on the production runtime classpath",
            findClass(fqn, module.getModuleRuntimeScope(false))
        )
    }

    private fun assertApplicationHasTheStarterBeans() {
        writeApplication()

        val active = activeBeanClasses()
        assertTrue("The starter configuration must be an active bean of the application: $active", STARTER_CONFIGURATION in active)
        assertTrue("The starter's @Bean must be an active bean of the application: $active", STARTER_ADVISOR in active)
        assertEquals(emptyList<String>(), autowiringProblems(writeConsumer()))
    }

    private fun assertOnlyOnRuntimeClasspath(application: Module) {
        assertNull(
            "Precondition: the starter must not be on the application's compile classpath",
            findClass(STARTER_CONFIGURATION, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(application, false))
        )
        assertNull(
            "Precondition: the starter must not be in the scope the library bean search reads today",
            findClass(STARTER_CONFIGURATION, ModuleUtil.getOnlyLibrarySearchScope(application))
        )
        assertNotNull(
            "Precondition: the starter must be on the application's runtime classpath",
            findClass(STARTER_CONFIGURATION, application.getModuleRuntimeScope(false))
        )
    }

    private fun writeApplication() {
        addFileToModule(module, applicationFileName, applicationText)
    }

    private fun writeConsumer(): PsiFile = addFileToModule(module, consumerFileName, consumerText)

    private fun activeBeanClasses(): Set<String?> =
        SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .mapTo(mutableSetOf()) { it.psiClass.qualifiedName }

    private fun autowiringProblems(consumer: PsiFile): List<String> {
        myFixture.configureFromExistingVirtualFile(consumer.virtualFile)
        return myFixture.doHighlighting()
            .mapNotNull { it.description }
            .filter { it.startsWith("Autowire failed") }
    }

    private fun findClass(fqn: String, scope: GlobalSearchScope) = JavaPsiFacade.getInstance(project).findClass(fqn, scope)

    protected companion object {
        const val STARTER_CONFIGURATION = "org.springframework.transaction.annotation.ProxyTransactionManagementConfiguration"
        const val STARTER_ADVISOR = "org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor"
    }
}
