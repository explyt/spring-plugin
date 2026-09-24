/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope

/**
 * The type a query names must be read in the selected application's classpath, like the beans it is compared to.
 *
 * Reading the beans in one scope and the query type in another is not a cosmetic asymmetry: two applications can
 * declare the same fully-qualified class against different dependencies, so a project-wide lookup compares the
 * selected application's beans against the *other* application's class. The reply then names a real bean and
 * decides compatibility from code the caller never asked about, and no field of the answer reveals it.
 */
class ScopedBeanMatcherClasspathScopeTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    /**
     * `ClockImpl` implements app-a's `ClockA` and app-b's `ClockB` respectively. A bean of app-a's `ClockImpl`
     * therefore answers a query for `ClockA` and must not answer one for `ClockB` - which it would if the query
     * type were resolved anywhere in the project.
     */
    fun testQueryTypeIsResolvedInsideTheSelectedApplicationClasspath() {
        val (moduleA, moduleB) = twoIsolatedApplications()

        val markerInA = classInScope(moduleA, "com.explyt.demo.a.ClockA")
        val markerInB = classInScope(moduleB, "com.explyt.demo.b.ClockB")
        assertNotNull("Precondition: app-a must declare its own marker interface", markerInA)
        assertNotNull("Precondition: app-b must declare its own marker interface", markerInB)
        assertNull(
            "Precondition: app-a must not see app-b's marker, otherwise the scope proves nothing",
            classInScope(moduleA, "com.explyt.demo.b.ClockB")
        )

        val snapshot = snapshotOf(moduleA, implOf(moduleA))

        val own = ScopedBeanMatcher(project).lookup(snapshot, BeanLookupSelector("com.explyt.demo.a.ClockA", null))
        assertEquals("The bean implements app-a's own marker", BeanOutcome.SINGLE, own.outcome)

        val foreign = queryProblem {
            ScopedBeanMatcher(project).lookup(snapshot, BeanLookupSelector("com.explyt.demo.b.ClockB", null))
        }
        assertEquals(
            "A type absent from the selected application's classpath is not found, not merely unmatched",
            ScopedBeanMatcher.TYPE_NOT_FOUND, foreign.problem.code
        )
    }

    /**
     * The same fully-qualified name declared in both applications must resolve to the selected one.
     *
     * A project-wide lookup finds two declarations of `ClockImpl` and resolves to neither or to an arbitrary one;
     * either way the comparison stops describing app-a.
     */
    fun testAmbiguousProjectWideNameStillResolvesInsideTheApplication() {
        val (moduleA, _) = twoIsolatedApplications()
        assertEquals(
            "Precondition: the FQN must be declared twice project-wide, otherwise the scope proves nothing",
            2,
            JavaPsiFacade.getInstance(project)
                .findClasses(CLOCK_IMPL, GlobalSearchScope.projectScope(project)).size
        )

        val snapshot = snapshotOf(moduleA, implOf(moduleA))

        val result = ScopedBeanMatcher(project).lookup(snapshot, BeanLookupSelector(CLOCK_IMPL, null))

        assertEquals(BeanOutcome.SINGLE, result.outcome)
        assertEquals(listOf("bean-clock"), result.match.records.map { it.id })
    }

    private fun twoIsolatedApplications(): Pair<Module, Module> {
        val moduleA = addDependencyModule("app-a")
        val moduleB = addDependencyModule("app-b")

        addFileToModule(moduleA, "com/explyt/demo/a/ClockA.java", marker("com.explyt.demo.a", "ClockA"))
        addFileToModule(moduleA, "com/explyt/demo/ClockImpl.java", clockImpl("com.explyt.demo.a.ClockA"))

        addFileToModule(moduleB, "com/explyt/demo/b/ClockB.java", marker("com.explyt.demo.b", "ClockB"))
        addFileToModule(moduleB, "com/explyt/demo/ClockImpl.java", clockImpl("com.explyt.demo.b.ClockB"))

        return moduleA to moduleB
    }

    private fun implOf(module: Module): PsiType {
        val psiClass = classInScope(module, CLOCK_IMPL) ?: error("No $CLOCK_IMPL in ${module.name}")
        return JavaPsiFacade.getElementFactory(project).createType(psiClass)
    }

    private fun classInScope(module: Module, fqn: String): PsiClass? = JavaPsiFacade.getInstance(project)
        .findClass(fqn, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false))

    private fun snapshotOf(module: Module, declaredType: PsiType) = ScopedBeanSnapshot(
        application = BeanApplicationIdentity("com.explyt.demo.a.AppA", module.name, "app-source"),
        selection = BeanContextSelection(BeanModelSource.NATIVE_SNAPSHOT, null, emptySet()),
        modelStamp = "stamp",
        records = listOf(
            ScopedBeanRecord(
                id = "bean-clock",
                name = "clock",
                knownNames = setOf("clock"),
                typeName = CLOCK_IMPL,
                kind = BeanKind.COMPONENT,
                declaration = null,
                declaredType = declaredType,
                declarationModule = module.name,
                primary = null,
                priority = null,
                details = BeanDetailsEvidence(),
                limitations = emptySet()
            )
        ),
        limitations = emptySet()
    )

    private fun queryProblem(action: () -> Unit): BeanQueryException =
        org.junit.Assert.assertThrows(BeanQueryException::class.java) { action() }

    private fun marker(packageName: String, name: String): String = """
        package $packageName;

        public interface $name {}
    """.trimIndent()

    private fun clockImpl(markerFqn: String): String = """
        package com.explyt.demo;

        public class ClockImpl implements $markerFqn {}
    """.trimIndent()

    private companion object {
        const val CLOCK_IMPL = "com.explyt.demo.ClockImpl"
    }
}
