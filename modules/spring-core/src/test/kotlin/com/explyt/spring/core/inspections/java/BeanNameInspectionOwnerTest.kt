/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringCacheableAnnotationInspection
import com.explyt.spring.core.inspections.SpringDependsOnBeanInspection
import com.explyt.spring.core.inspections.SpringLookupBeanInspection
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.lang.annotation.HighlightSeverity

class BeanNameInspectionOwnerTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(
            TestLibrary.springContext_6_0_7,
            TestLibrary.springBoot_3_1_1,
            TestLibrary.springBootAutoConfigure_3_1_1,
        )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(
            SpringLookupBeanInspection::class.java,
            SpringDependsOnBeanInspection::class.java,
            SpringCacheableAnnotationInspection::class.java,
        )
        myFixture.addClass(
            """
            package app;
            @org.springframework.boot.autoconfigure.SpringBootApplication
            public class App {}
            """.trimIndent()
        )
        myFixture.addClass(
            """
            package app;
            @org.springframework.stereotype.Component("lookBeanA")
            public class A {}
            """.trimIndent()
        )
        myFixture.addClass(
            """
            package app;
            public class B {}
            """.trimIndent()
        )
    }

    fun testLookupOfMissingBeanOnOwnerOutsideScanIsNotReported() {
        configureOwner("other", "@${LOOKUP}(\"missing\") public app.A a() { return null; }")
        assertOwnerIsNotBean("other.Owner")
        assertNoHighlightOn("\"missing\"")
    }

    fun testLookupOfOtherTypedBeanOnOwnerOutsideScanIsNotReported() {
        configureOwner("other", "@${LOOKUP}(\"lookBeanA\") public app.B b() { return null; }")
        assertOwnerIsNotBean("other.Owner")
        assertNoHighlightOn("\"lookBeanA\"")
    }

    fun testDependsOnMissingBeanOnOwnerOutsideScanIsNotReported() {
        configureOwner("other", "", "@${DEPENDS_ON}(\"missing\")")
        assertOwnerIsNotBean("other.Owner")
        assertNoHighlightOn("\"missing\"")
    }

    fun testCacheableWithMissingCacheManagerOnOwnerOutsideScanIsNotReported() {
        configureOwner("other", "", "@${CACHEABLE}(cacheManager = \"missing\")")
        assertOwnerIsNotBean("other.Owner")
        assertNoHighlightOn("\"missing\"")
    }

    fun testLookupOfMissingBeanOnActiveOwnerIsReported() {
        configureOwner("app", "@${LOOKUP}(\"missing\") public app.A a() { return null; }")
        assertOwnerIsBean("app.Owner")
        assertHighlightOn("\"missing\"")
    }

    fun testLookupOfOtherTypedBeanOnActiveOwnerIsReported() {
        configureOwner("app", "@${LOOKUP}(\"lookBeanA\") public app.B b() { return null; }")
        assertOwnerIsBean("app.Owner")
        assertHighlightOn("\"lookBeanA\"")
    }

    fun testLookupOfExistingBeanOnActiveOwnerIsNotReported() {
        configureOwner("app", "@${LOOKUP}(\"lookBeanA\") public app.A a() { return null; }")
        assertOwnerIsBean("app.Owner")
        assertNoHighlightOn("\"lookBeanA\"")
    }

    fun testDependsOnMissingBeanOnActiveOwnerIsReported() {
        configureOwner("app", "", "@${DEPENDS_ON}(\"missing\")")
        assertOwnerIsBean("app.Owner")
        assertHighlightOn("\"missing\"")
    }

    fun testCacheableWithMissingCacheManagerOnActiveOwnerIsReported() {
        configureOwner("app", "", "@${CACHEABLE}(cacheManager = \"missing\")")
        assertOwnerIsBean("app.Owner")
        assertHighlightOn("\"missing\"")
    }

    fun testLookupOfMissingBeanOnAbstractBaseOfActiveBeanIsReported() {
        addBeanClass("@org.springframework.stereotype.Component public class Impl extends Base {}")
        configureBase("public abstract class Base { @${LOOKUP}(\"missing\") public app.A a() { return null; } }")
        assertSubclassIsBean("app.Impl", "app.Base")
        assertHighlightOn("\"missing\"")
    }

    fun testCacheableWithMissingCacheManagerOnAbstractBaseOfActiveBeanIsReported() {
        addBeanClass("@org.springframework.stereotype.Component public class Impl extends Base {}")
        configureBase(
            "public abstract class Base { @${CACHEABLE}(cacheManager = \"missing\") public String find() { return null; } }"
        )
        assertSubclassIsBean("app.Impl", "app.Base")
        assertHighlightOn("\"missing\"")
    }

    fun testCacheableWithMissingCacheManagerOnInterfaceOfActiveBeanIsReported() {
        addBeanClass(
            "@org.springframework.stereotype.Component public class Impl implements Base { public String find() { return null; } }"
        )
        configureBase("public interface Base { @${CACHEABLE}(cacheManager = \"missing\") String find(); }")
        assertSubclassIsBean("app.Impl", "app.Base")
        assertHighlightOn("\"missing\"")
    }

    fun testLookupOfMissingBeanOnAbstractBaseThatNoBeanExtendsIsNotReported() {
        configureBase("public abstract class Base { @${LOOKUP}(\"missing\") public app.A a() { return null; } }")
        assertOwnerIsNotBean("app.Base")
        assertNoHighlightOn("\"missing\"")
    }

    fun testCacheableWithMissingCacheManagerOnImplementationReturnedByBeanFactoryIsReported() {
        addBeanClass("public interface Foo { String find(); }")
        addBeanClass(
            "@org.springframework.context.annotation.Configuration public class FooConfig { " +
                "@org.springframework.context.annotation.Bean public Foo foo() { return new FooImpl(); } }"
        )
        configureFooImpl()
        assertFactoryBeanDeclaresOnlyInterface()
        assertHighlightOn("\"missing\"")
    }

    fun testCacheableWithMissingCacheManagerOnImplementationThatNoBeanFactoryReturnsIsNotReported() {
        addBeanClass("public interface Foo { String find(); }")
        configureFooImpl()
        assertOwnerIsNotBean("app.FooImpl")
        assertNoHighlightOn("\"missing\"")
    }

    private fun configureFooImpl() {
        myFixture.configureByText(
            "FooImpl.java",
            "package app;\npublic class FooImpl implements Foo { " +
                "@${CACHEABLE}(cacheManager = \"missing\") public String find() { return null; } }"
        )
    }

    private fun assertFactoryBeanDeclaresOnlyInterface() {
        val active = activeBeanClassNames()
        assertTrue("Precondition: an active bean has psiClass app.Foo, got $active", "app.Foo" in active)
        assertFalse("Precondition: app.FooImpl is not an active bean class, got $active", "app.FooImpl" in active)
    }

    private fun addBeanClass(declaration: String) {
        myFixture.addClass("package app;\n$declaration")
    }

    private fun configureBase(declaration: String) {
        myFixture.configureByText("Base.java", "package app;\n$declaration")
    }

    private fun assertSubclassIsBean(beanFqn: String, baseFqn: String) {
        val active = activeBeanClassNames()
        assertTrue("Precondition: $beanFqn is an active bean, got $active", beanFqn in active)
        assertFalse("Precondition: $baseFqn is not an active bean itself, got $active", baseFqn in active)
    }

    private fun configureOwner(packageName: String, member: String, classAnnotation: String = "") {
        myFixture.configureByText(
            "Owner.java",
            """
            package $packageName;
            @org.springframework.context.annotation.Configuration
            $classAnnotation
            public class Owner {
                $member
            }
            """.trimIndent()
        )
    }

    private fun activeBeanClassNames(): List<String?> =
        SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module).map { it.psiClass.qualifiedName }

    private fun assertOwnerIsNotBean(fqn: String) {
        val active = activeBeanClassNames()
        assertTrue("Precondition: app.A is an active bean, got $active", "app.A" in active)
        assertFalse("Precondition: $fqn is not an active bean, got $active", fqn in active)
    }

    private fun assertOwnerIsBean(fqn: String) {
        val active = activeBeanClassNames()
        assertTrue("Precondition: $fqn is an active bean, got $active", fqn in active)
    }

    private fun highlightsOn(text: String) =
        myFixture.doHighlighting().filter { it.severity >= HighlightSeverity.WEAK_WARNING && it.text == text }

    private fun assertNoHighlightOn(text: String) {
        val found = highlightsOn(text)
        assertTrue("Expected no bean-name problem on $text, got ${found.map { it.description }}", found.isEmpty())
    }

    private fun assertHighlightOn(text: String) {
        val found = highlightsOn(text)
        assertTrue("Expected a bean-name problem on $text", found.isNotEmpty())
    }

    private companion object {
        const val LOOKUP = "org.springframework.beans.factory.annotation.Lookup"
        const val DEPENDS_ON = "org.springframework.context.annotation.DependsOn"
        const val CACHEABLE = "org.springframework.cache.annotation.Cacheable"
    }
}
