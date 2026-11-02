/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.data.service.kotlin

import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.data.SpringDataClasses
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch

class RepositoryComponentCandidateTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1, TestLibrary.springContext_6_0_7, TestLibrary.springDataJpa_3_1_0
    )

    override fun setUp() {
        super.setUp()
        Registry.get("explyt.spring.root.runConfiguration").setValue(false)
    }

    override fun tearDown() {
        super.tearDown()
        Registry.get("explyt.spring.root.runConfiguration").resetToDefault()
    }

    fun testAnonymousRepositoryImplementationKeepsRepositoryBeans() {
        configureRepositories()
        myFixture.addFileToProject(
            "repos/RepositoryStubs.kt", """
            package repos

            import org.springframework.data.repository.Repository

            class RepositoryStubs {
                val stub: Repository<Person, Long> = object : Repository<Person, Long> {}
            }
            """.trimIndent()
        )
        assertTrue(
            "precondition: the inheritor search finds the anonymous implementation",
            repositoryInheritors().any { it.name == null }
        )
        assertSingleBean("repos.PlainRepo", "plainRepo")
    }

    fun testRepositoryImplementationClassesAreNotBeans() {
        configureRepositories()
        myFixture.addFileToProject(
            "repos/RepositoryImplementations.kt", """
            package repos

            import org.springframework.data.repository.Repository

            abstract class BaseRepo : Repository<Person, Long>

            class PlainRepoStub : BaseRepo(), PlainRepo
            """.trimIndent()
        )
        val implementations = setOf("BaseRepo", "PlainRepoStub")
        assertEquals(
            "precondition: the inheritor search finds every implementation class",
            implementations,
            repositoryInheritors().mapNotNullTo(mutableSetOf()) { it.name }.intersect(implementations)
        )
        assertSingleBean("repos.PlainRepo", "plainRepo")
        val beans = activeBeans().filter { it.psiClass.name in implementations }
        assertEquals("implementation classes must not be beans", emptyList<String>(), beans.map { it.name })
    }

    private fun configureRepositories() {
        myFixture.addFileToProject(
            "repos/App.kt", """
            package repos

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class App
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "repos/Person.kt", """
            package repos

            class Person
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "repos/PlainRepo.kt", """
            package repos

            import org.springframework.data.repository.CrudRepository

            interface PlainRepo : CrudRepository<Person, Long>
            """.trimIndent()
        )
    }

    private fun assertSingleBean(qualifiedName: String, beanName: String) {
        val beans = activeBeans().filter { it.psiClass.qualifiedName == qualifiedName }
        assertEquals("$qualifiedName must stay a bean", setOf(beanName), beans.map { it.name }.toSet())
    }

    private fun repositoryInheritors(): Collection<PsiClass> {
        val repository = JavaPsiFacade.getInstance(project)
            .findClass(SpringDataClasses.REPOSITORY, GlobalSearchScope.allScope(project))
        assertNotNull("${SpringDataClasses.REPOSITORY} must exist", repository)
        return ClassInheritorsSearch.search(repository!!, module.moduleWithDependenciesScope, true).findAll()
    }

    private fun activeBeans(): Set<PsiBean> = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
}
