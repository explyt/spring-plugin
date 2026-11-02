/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.data.service.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.data.SpringDataClasses
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch

class RepositoryComponentCandidateTest : ExplytJavaLightTestCase() {
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

    fun testAbstractLibraryRepositoryIsNotBean() {
        configureRepositories()
        val querydslSupport = "org.springframework.data.jpa.repository.support.QuerydslRepositorySupport"
        val psiClass = findClass(querydslSupport, GlobalSearchScope.allScope(project))
        assertTrue(psiClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT))
        assertTrue(psiClass.hasModifierProperty("abstract"))
        assertSingleBean("repos.PlainRepo", "plainRepo")
        val beans = activeBeans().filter { it.psiClass.qualifiedName == querydslSupport }
        assertEquals("$querydslSupport must not be a bean", emptyList<String>(), beans.map { it.name })
    }

    fun testRepositoryInterfaceStaysBean() {
        configureRepositories()
        assertSingleBean("repos.PlainRepo", "plainRepo")
    }

    fun testAnnotatedRepositoryInterfaceStaysBean() {
        configureRepositories()
        val annotated = findClass("repos.AnnotatedRepo", GlobalSearchScope.projectScope(project))
        assertTrue(annotated.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT))
        assertSingleBean("repos.AnnotatedRepo", "annotatedRepo")
    }

    fun testAnonymousRepositoryImplementationKeepsRepositoryBeans() {
        configureRepositories()
        myFixture.addFileToProject(
            "repos/RepositoryStubs.java", """
            package repos;
            import org.springframework.data.repository.Repository;
            public class RepositoryStubs {
                Repository<Person, Long> stub = new Repository<Person, Long>() {};
            }
            """.trimIndent()
        )
        assertTrue(
            "precondition: the inheritor search finds the anonymous implementation",
            repositoryInheritors().any { it is PsiAnonymousClass }
        )
        assertSingleBean("repos.PlainRepo", "plainRepo")
        assertSingleBean("repos.AnnotatedRepo", "annotatedRepo")
    }

    fun testRepositoryImplementationClassesAreNotBeans() {
        configureRepositories()
        myFixture.addFileToProject(
            "repos/RepositoryImplementations.java", """
            package repos;
            import org.springframework.data.repository.Repository;
            abstract class BaseRepo implements Repository<Person, Long> {}
            class PlainRepoStub extends BaseRepo implements PlainRepo {}
            class RepositoryFactory {
                void create() {
                    class LocalRepo implements Repository<Person, Long> {}
                }
            }
            """.trimIndent()
        )
        val implementations = setOf("BaseRepo", "PlainRepoStub", "LocalRepo")
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
            "repos/App.java", """
            package repos;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            @SpringBootApplication
            public class App {}
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "repos/Person.java", """
            package repos;
            public class Person {}
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "repos/PlainRepo.java", """
            package repos;
            import org.springframework.data.repository.CrudRepository;
            public interface PlainRepo extends CrudRepository<Person, Long> {}
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "repos/AnnotatedRepo.java", """
            package repos;
            import org.springframework.data.repository.CrudRepository;
            import org.springframework.stereotype.Repository;
            @Repository
            public interface AnnotatedRepo extends CrudRepository<Person, Long> {}
            """.trimIndent()
        )
    }

    private fun assertSingleBean(qualifiedName: String, beanName: String) {
        val beans = activeBeans().filter { it.psiClass.qualifiedName == qualifiedName }
        assertEquals("$qualifiedName must stay a bean", setOf(beanName), beans.map { it.name }.toSet())
    }

    private fun findClass(qualifiedName: String, scope: GlobalSearchScope): PsiClass {
        val psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName, scope)
        assertNotNull("$qualifiedName must exist", psiClass)
        return psiClass!!
    }

    private fun repositoryInheritors(): Collection<PsiClass> {
        val repository = findClass(SpringDataClasses.REPOSITORY, GlobalSearchScope.allScope(project))
        return ClassInheritorsSearch.search(repository, module.moduleWithDependenciesScope, true).findAll()
    }

    private fun activeBeans(): Set<PsiBean> = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
}
