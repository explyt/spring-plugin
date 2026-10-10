/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.beans.BeanSourcePreference
import com.explyt.util.ExplytAnnotationUtil.getStringMemberValues
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue

object ComposedBeanNameFixture {
    private const val ALIAS_FOR = "org.springframework.core.annotation.AliasFor"
    private const val COMPOSED = "beanname.MyBean"

    fun addJava(fixture: CodeInsightTestFixture, attributeDeclaration: String, usage: String) {
        fixture.addFileToProject(
            "beanname/App.java",
            """
            package beanname;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            import org.springframework.context.annotation.Bean;
            import org.springframework.core.annotation.AliasFor;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            @Bean
            @Retention(RetentionPolicy.RUNTIME)
            @interface MyBean {
                $attributeDeclaration
            }
            @SpringBootApplication
            public class App {
                $usage
                public Foo foo() { return new Foo(); }
            }
            class Foo {}
            """.trimIndent()
        )
    }

    fun addKotlin(fixture: CodeInsightTestFixture, attributeDeclaration: String, usage: String) {
        fixture.addFileToProject(
            "beanname/App.kt",
            """
            package beanname
            import org.springframework.boot.autoconfigure.SpringBootApplication
            import org.springframework.context.annotation.Bean
            import org.springframework.core.annotation.AliasFor
            @Bean
            annotation class MyBean($attributeDeclaration)
            @SpringBootApplication
            open class App {
                $usage
                open fun foo(): Foo = Foo()
            }
            class Foo
            """.trimIndent()
        )
    }

    fun addJavaApplication(fixture: CodeInsightTestFixture, declarations: String, usage: String) {
        fixture.addFileToProject(
            "beanname/App.java",
            """
            |package beanname;
            |import org.springframework.boot.autoconfigure.SpringBootApplication;
            |import org.springframework.context.annotation.Bean;
            |import org.springframework.core.annotation.AliasFor;
            |import java.lang.annotation.Retention;
            |import java.lang.annotation.RetentionPolicy;
            |${declarations.trimIndent()}
            |@SpringBootApplication
            |public class App {
            |    $usage
            |    public Foo foo() { return new Foo(); }
            |}
            |class Foo {}
            """.trimMargin()
        )
    }

    fun addKotlinApplication(fixture: CodeInsightTestFixture, declarations: String, usage: String) {
        fixture.addFileToProject(
            "beanname/App.kt",
            """
            |package beanname
            |import org.springframework.boot.autoconfigure.SpringBootApplication
            |import org.springframework.context.annotation.Bean
            |import org.springframework.core.annotation.AliasFor
            |${declarations.trimIndent()}
            |@SpringBootApplication
            |open class App {
            |    $usage
            |    open fun foo(): Foo = Foo()
            |}
            |class Foo
            """.trimMargin()
        )
    }

    fun assertFactoryBeanNames(
        fixture: CodeInsightTestFixture,
        module: Module,
        annotationValues: Map<Pair<String, String>, List<String>>,
        expectedNames: List<String>,
    ) {
        val project = fixture.project
        val application = JavaPsiFacade.getInstance(project)
            .findClass("beanname.App", GlobalSearchScope.projectScope(project))!!
        val method = application.findMethodsByName("foo", false).single()
        annotationValues.forEach { (annotationAndAttribute, values) ->
            val (annotationName, attribute) = annotationAndAttribute
            val annotation = method.getAnnotation(annotationName)
            assertNotNull("$annotationName on the factory method", annotation)
            val declaration = annotation!!.resolveAnnotationType()
                ?: JavaPsiFacade.getInstance(project).findClass(annotationName, GlobalSearchScope.allScope(project))
            assertEquals("$annotationName resolves", annotationName, declaration?.qualifiedName)
            assertTrue("$annotationName is Spring @Bean or meta-annotated with it", isBeanAnnotation(fixture, declaration!!, emptySet()))
            assertEquals("Declared $annotationName.$attribute values", values, annotation.getStringMemberValues(attribute))
        }
        assertTrue("Expected names differ from the method name unless they fall back to it",
            expectedNames == listOf(method.name) || method.name !in expectedNames)

        val records = SpringSearchServiceFacade.getInstance(project)
            .getBeanSnapshot(application, BeanSourcePreference.STATIC).records
            .filter { it.typeName == "beanname.Foo" }
        assertEquals("One factory must produce one snapshot record", 1, records.size)
        assertEquals("Snapshot bean name", expectedNames.first(), records.single().name)
        assertEquals("Snapshot known names", expectedNames, records.single().knownNames.toList())
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .filter { it.psiClass.qualifiedName == "beanname.Foo" }
        assertEquals("Active model bean names", expectedNames.toSet(), beans.map { it.name }.toSet())
    }

    private fun isBeanAnnotation(fixture: CodeInsightTestFixture, type: PsiClass, visited: Set<String>): Boolean {
        val name = type.qualifiedName ?: return false
        if (name == SpringCoreClasses.BEAN) return true
        if (name in visited || name.startsWith("java.") || name.startsWith("kotlin.")) return false
        return type.annotations.any { meta ->
            val metaType = meta.resolveAnnotationType() ?: meta.qualifiedName?.let {
                JavaPsiFacade.getInstance(fixture.project).findClass(it, GlobalSearchScope.allScope(fixture.project))
            }
            metaType?.let { isBeanAnnotation(fixture, it, visited + name) } == true
        }
    }

    fun assertSpringCoreMajorVersion(fixture: CodeInsightTestFixture, module: Module, major: Int) {
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        val springVersion = JavaPsiFacade.getInstance(fixture.project).findClass("org.springframework.core.SpringVersion", scope)
        assertNotNull("spring-core is on the module classpath", springVersion)
        val jarName = springVersion!!.containingFile.virtualFile.path.substringBefore("!/").substringAfterLast('/')
        assertTrue("spring-core jar $jarName is version $major.x", jarName.startsWith("spring-core-$major."))
    }

    fun assertComposedBeanNames(
        fixture: CodeInsightTestFixture,
        module: Module,
        attribute: String,
        aliasTarget: String?,
        declaredValues: List<String>,
        expectedNames: List<String>,
    ) {
        val project = fixture.project
        val application = JavaPsiFacade.getInstance(project)
            .findClass("beanname.App", GlobalSearchScope.projectScope(project))!!
        val method = application.findMethodsByName("foo", false).single()
        val annotation = method.getAnnotation(COMPOSED)
        assertNotNull("Composed annotation on the factory method", annotation)
        val declaration = JavaPsiFacade.getInstance(project)
            .findClass(annotation!!.qualifiedName!!, GlobalSearchScope.allScope(project))
        assertEquals("Composed annotation resolves", COMPOSED, declaration?.qualifiedName)
        assertTrue("Composed declaration is an annotation type", declaration!!.isAnnotationType)
        assertEquals(
            "Composed annotation is meta-annotated with Spring @Bean",
            SpringCoreClasses.BEAN,
            declaration.getAnnotation(SpringCoreClasses.BEAN)?.resolveAnnotationType()?.qualifiedName
        )
        val aliasAttribute = declaration.findMethodsByName(attribute, false).singleOrNull()
        assertNotNull("Composed attribute '$attribute' is visible", aliasAttribute)
        val aliasFor = aliasAttribute!!.getAnnotation(ALIAS_FOR)
        if (aliasTarget == null) {
            assertNull("Attribute '$attribute' declares no @AliasFor", aliasFor)
        } else {
            assertEquals("@AliasFor resolves", ALIAS_FOR, aliasFor?.resolveAnnotationType()?.qualifiedName)
            assertEquals("@AliasFor target attribute", listOf(aliasTarget), aliasFor!!.getStringMemberValues("attribute"))
            assertTrue(
                "@AliasFor targets @Bean",
                aliasFor.findDeclaredAttributeValue("annotation")?.text.orEmpty().contains("Bean")
            )
        }
        assertEquals("Declared composed attribute values", declaredValues, annotation.getStringMemberValues(attribute))
        assertTrue("Fixture names differ from the method name", declaredValues.none { it == method.name })

        val records = SpringSearchServiceFacade.getInstance(project)
            .getBeanSnapshot(application, BeanSourcePreference.STATIC).records
            .filter { it.typeName == "beanname.Foo" }
        assertEquals("One factory must produce one snapshot record", 1, records.size)
        assertEquals("Snapshot bean name", expectedNames.first(), records.single().name)
        assertEquals("Snapshot known names", expectedNames, records.single().knownNames.toList())
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .filter { it.psiClass.qualifiedName == "beanname.Foo" }
        assertEquals("Active model bean names", expectedNames.toSet(), beans.map { it.name }.toSet())
    }
}
