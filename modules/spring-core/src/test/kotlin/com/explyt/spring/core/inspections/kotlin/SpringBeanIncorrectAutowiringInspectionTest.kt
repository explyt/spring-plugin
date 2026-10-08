/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.util.ExplytAnnotationUtil.getStringMemberValues
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.explyt.spring.core.inspections.SpringBeanIncorrectAutowiringInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.test.TestMetadata

class SpringBeanIncorrectAutowiringInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springBootTestAutoConfigure_3_1_1
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringBeanIncorrectAutowiringInspection::class.java)
    }

    @TestMetadata("autowired")
    fun testAutowired() = doTest(SpringBeanIncorrectAutowiringInspection())

    fun testResourceLoader() {
        @Language("kotlin") val code = """
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.core.io.ResourceLoader
import org.springframework.context.support.AbstractApplicationContext

@${SpringCoreClasses.COMPONENT}
class DemoApplication {
    @Autowired var resourceLoader: ResourceLoader? = null
    @Autowired var context: ApplicationContext? = null
    @Autowired var abstractContext: AbstractApplicationContext? = null
}
            """
        myFixture.configureByText("DemoApplication.kt", code.trimIndent())
        myFixture.testHighlighting("DemoApplication.kt")
    }

    fun testAutowiredInContextConfigurationTestClass() {
        @Language("kotlin") val code = """
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

@${SpringCoreClasses.COMPONENT}
class SomeService

@ContextConfiguration(classes = [SomeService::class])
class SomeServiceTest {
    @Autowired
    lateinit var service: SomeService
}
            """
        myFixture.configureByText("SomeServiceTest.kt", code.trimIndent())
        // A test class is not a bean, but Spring injects into it, so @Autowired must not be reported
        // as "Must be defined in valid Spring bean". @SpringBootTest was already accepted through its
        // @BootstrapWith meta-annotation; plain @ContextConfiguration was not.
        myFixture.testHighlighting("SomeServiceTest.kt")
    }

    fun testBeanNameAttributeQualifier() = assertBeanNameQualifier("@Bean(name = [\"x\"])", "x")

    fun testBeanNameUnnamedQualifierControl() = assertBeanNameQualifier("@Bean", "foo")

    fun testExplicitBeanNameDoesNotMatchMethodNameQualifier() = assertBeanNameQualifier("@Bean(name = [\"x\"])", "foo", true)

    private fun assertBeanNameQualifier(annotation: String, qualifier: String, missing: Boolean = false) {
        myFixture.configureByText(
            "App.kt",
            """
            package beanname
            import org.springframework.boot.autoconfigure.SpringBootApplication
            import org.springframework.context.annotation.Bean
            import org.springframework.beans.factory.annotation.Autowired
            import org.springframework.beans.factory.annotation.Qualifier
            import org.springframework.stereotype.Component
            @SpringBootApplication
            open class App {
                $annotation
                open fun foo(): Foo = Foo()
                @Bean
                open fun other(): Foo = Foo()
            }
            class Foo
            @Component
            class Consumer {
                @Autowired
                @Qualifier("$qualifier")
                lateinit var foo: Foo
            }
            """.trimIndent()
        )
        val scope = GlobalSearchScope.projectScope(project)
        val facade = JavaPsiFacade.getInstance(project)
        val application = facade.findClass("beanname.App", scope)!!
        val beanAnnotation = application.findMethodsByName("foo", false).single()
            .getAnnotation(SpringCoreClasses.BEAN)!!
        assertEquals(SpringCoreClasses.BEAN, beanAnnotation.resolveAnnotationType()?.qualifiedName)
        val expectedNames = if (qualifier == "x" || missing) listOf("x") else emptyList()
        if (missing) assertTrue("foo" !in expectedNames)
        assertEquals("Declared @Bean name values", expectedNames, beanAnnotation.getStringMemberValues("name"))
        assertEquals("Explicit name attribute presence", qualifier == "x" || missing, beanAnnotation.findDeclaredAttributeValue("name") != null)
        assertNotNull(application.findMethodsByName("other", false).single().getAnnotation(SpringCoreClasses.BEAN))
        val injection = facade.findClass("beanname.Consumer", scope)!!.findFieldByName("foo", false)!!
        assertEquals(listOf(qualifier), injection.getAnnotation(SpringCoreClasses.QUALIFIER)!!.getStringMemberValues())
        if (missing) {
            val errors = myFixture.doHighlighting().filter { it.description == "Autowire failed. No beans of 'Foo' found" }
            assertEquals(1, errors.size)
        } else {
            myFixture.testHighlighting(true, false, false)
        }
    }
}
