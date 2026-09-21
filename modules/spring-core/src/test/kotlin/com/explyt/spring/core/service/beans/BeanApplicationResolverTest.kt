/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import org.junit.Assert

class BeanApplicationResolverTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    fun testTwoApplicationsRequireAnExplicitChoice() {
        twoApplications()

        val failure = queryProblem { BeanApplicationResolver(project).resolve(null, null) }

        assertEquals(BeanApplicationResolver.APPLICATION_REQUIRED, failure.problem.code)
        assertEquals(2, failure.problem.choices.size)
        assertEquals(
            setOf("com.explyt.demo.a.AppA", "com.explyt.demo.b.AppB"),
            failure.problem.choices.mapNotNull { it["applicationClassName"] }.toSet()
        )
    }

    fun testExplicitNameSelectsThatApplication() {
        twoApplications()

        val resolved = BeanApplicationResolver(project).resolve("com.explyt.demo.b.AppB", null)

        assertEquals("com.explyt.demo.b.AppB", resolved.qualifiedName)
    }

    /** A class that exists but is not an application must be told apart from one that does not exist at all. */
    fun testNonApplicationClassIsNotAccepted() {
        val (moduleA, _) = twoApplications()
        sharedFile(moduleA)
        assertNotNull(
            "Precondition: the shared class must resolve, otherwise the rejection proves nothing",
            findClass("com.explyt.demo.shared.SharedComponent")
        )

        val failure = queryProblem {
            BeanApplicationResolver(project).resolve("com.explyt.demo.shared.SharedComponent", null)
        }

        assertEquals(BeanApplicationResolver.APPLICATION_NOT_FOUND, failure.problem.code)
    }

    fun testNoApplicationAtAllIsReportedAsAbsence() {
        val failure = queryProblem { BeanApplicationResolver(project).resolve(null, null) }

        assertEquals(BeanApplicationResolver.APPLICATION_NOT_FOUND, failure.problem.code)
    }

    /**
     * Two applications inside one module are two contexts. Answering from the first one would be indistinguishable
     * from answering from the right one, so the caller is asked.
     */
    fun testTwoApplicationsInOneModuleDoNotCollapse() {
        addFileToModule(module, "com/explyt/demo/one/AppOne.java", application("com.explyt.demo.one", "AppOne"))
        addFileToModule(module, "com/explyt/demo/two/AppTwo.java", application("com.explyt.demo.two", "AppTwo"))
        assertNotNull(findClass("com.explyt.demo.one.AppOne"))
        assertNotNull(findClass("com.explyt.demo.two.AppTwo"))

        val failure = queryProblem { BeanApplicationResolver(project).resolve(null, null) }

        assertEquals(BeanApplicationResolver.APPLICATION_REQUIRED, failure.problem.code)
        assertEquals(
            setOf("com.explyt.demo.one.AppOne", "com.explyt.demo.two.AppTwo"),
            failure.problem.choices.mapNotNull { it["applicationClassName"] }.toSet()
        )
    }

    /**
     * An injection file narrows the candidates to the applications that actually see it. The file below lives in
     * the module `app-b` depends on, so only `AppB` is a candidate - `AppA` never sees it.
     */
    fun testInjectionFileNarrowsCandidatesToItsScope() {
        twoApplications()
        val consumer = addFileToModule(
            module,
            "com/explyt/demo/consumer/Consumer.java",
            """
            package com.explyt.demo.consumer;

            public class Consumer {}
            """.trimIndent()
        )

        val resolved = BeanApplicationResolver(project).resolve(null, consumer)

        assertEquals("com.explyt.demo.b.AppB", resolved.qualifiedName)
    }

    /**
     * [com.intellij.testFramework.UsefulTestCase] inherits its own `assertThrows` returning `Unit`, which shadows
     * the JUnit import inside a fixture subclass, so the qualified call is the one that hands back the exception.
     */
    private fun queryProblem(action: () -> Unit): BeanQueryException =
        Assert.assertThrows(BeanQueryException::class.java) { action() }

    private fun twoApplications(): Pair<Module, Module> {
        val moduleA = addDependencyModule("app-a")
        val moduleB = addDependentModule("app-b")
        addFileToModule(moduleA, "com/explyt/demo/a/AppA.java", application("com.explyt.demo.a", "AppA"))
        addFileToModule(moduleB, "com/explyt/demo/b/AppB.java", application("com.explyt.demo.b", "AppB"))

        assertNotNull(
            "Precondition: AppA must resolve, otherwise the fixture proves nothing",
            findClass("com.explyt.demo.a.AppA")
        )
        assertNotNull(
            "Precondition: AppB must resolve, otherwise the fixture proves nothing",
            findClass("com.explyt.demo.b.AppB")
        )
        return moduleA to moduleB
    }

    private fun findClass(qualifiedName: String) =
        JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.allScope(project))

    private fun application(packageName: String, name: String) = """
        package $packageName;

        import org.springframework.boot.autoconfigure.SpringBootApplication;

        @SpringBootApplication
        public class $name {}
    """.trimIndent()

    private fun sharedFile(module: Module): PsiFile = addFileToModule(
        module,
        "com/explyt/demo/shared/SharedComponent.java",
        """
        package com.explyt.demo.shared;

        public class SharedComponent {}
        """.trimIndent()
    )
}
