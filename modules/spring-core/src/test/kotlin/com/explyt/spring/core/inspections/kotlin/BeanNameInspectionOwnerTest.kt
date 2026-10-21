/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.inspections.SpringCacheableAnnotationInspection
import com.explyt.spring.core.inspections.SpringDependsOnBeanInspection
import com.explyt.spring.core.inspections.SpringLookupBeanInspection
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.lang.annotation.HighlightSeverity

class BeanNameInspectionOwnerTest : ExplytInspectionKotlinTestCase() {

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
        configureOwner("other", "@$LOOKUP(\"missing\") open fun a(): app.A? = null")
        assertOwnerIsNotBean("other.Owner")
        assertNoHighlightOn("\"missing\"")
    }

    fun testLookupOfOtherTypedBeanOnOwnerOutsideScanIsNotReported() {
        configureOwner("other", "@$LOOKUP(\"lookBeanA\") open fun b(): app.B? = null")
        assertOwnerIsNotBean("other.Owner")
        assertNoHighlightOn("\"lookBeanA\"")
    }

    fun testDependsOnMissingBeanOnOwnerOutsideScanIsNotReported() {
        configureOwner("other", "", "@$DEPENDS_ON(\"missing\")")
        assertOwnerIsNotBean("other.Owner")
        assertNoHighlightOn("\"missing\"")
    }

    fun testCacheableWithMissingCacheManagerOnOwnerOutsideScanIsNotReported() {
        configureOwner("other", "", "@$CACHEABLE(cacheManager = \"missing\")")
        assertOwnerIsNotBean("other.Owner")
        assertNoHighlightOn("\"missing\"")
    }

    fun testLookupOfMissingBeanOnActiveOwnerIsReported() {
        configureOwner("app", "@$LOOKUP(\"missing\") open fun a(): app.A? = null")
        assertOwnerIsBean("app.Owner")
        assertHighlightOn("\"missing\"")
    }

    fun testLookupOfOtherTypedBeanOnActiveOwnerIsReported() {
        configureOwner("app", "@$LOOKUP(\"lookBeanA\") open fun b(): app.B? = null")
        assertOwnerIsBean("app.Owner")
        assertHighlightOn("\"lookBeanA\"")
    }

    fun testLookupOfExistingBeanOnActiveOwnerIsNotReported() {
        configureOwner("app", "@$LOOKUP(\"lookBeanA\") open fun a(): app.A? = null")
        assertOwnerIsBean("app.Owner")
        assertNoHighlightOn("\"lookBeanA\"")
    }

    fun testDependsOnMissingBeanOnActiveOwnerIsReported() {
        configureOwner("app", "", "@$DEPENDS_ON(\"missing\")")
        assertOwnerIsBean("app.Owner")
        assertHighlightOn("\"missing\"")
    }

    fun testCacheableWithMissingCacheManagerOnActiveOwnerIsReported() {
        configureOwner("app", "", "@$CACHEABLE(cacheManager = \"missing\")")
        assertOwnerIsBean("app.Owner")
        assertHighlightOn("\"missing\"")
    }

    fun testLookupOfMissingBeanOnAbstractBaseOfActiveBeanIsReported() {
        addBeanClass("@org.springframework.stereotype.Component class Impl : Base()")
        configureBase("abstract class Base { @$LOOKUP(\"missing\") open fun a(): app.A? = null }")
        assertSubclassIsBean("app.Impl", "app.Base")
        assertHighlightOn("\"missing\"")
    }

    fun testCacheableWithMissingCacheManagerOnAbstractBaseOfActiveBeanIsReported() {
        addBeanClass("@org.springframework.stereotype.Component class Impl : Base()")
        configureBase("abstract class Base { @$CACHEABLE(cacheManager = \"missing\") open fun find(): String? = null }")
        assertSubclassIsBean("app.Impl", "app.Base")
        assertHighlightOn("\"missing\"")
    }

    fun testCacheableWithMissingCacheManagerOnInterfaceOfActiveBeanIsReported() {
        addBeanClass("@org.springframework.stereotype.Component class Impl : Base { override fun find(): String? = null }")
        configureBase("interface Base { @$CACHEABLE(cacheManager = \"missing\") fun find(): String? }")
        assertSubclassIsBean("app.Impl", "app.Base")
        assertHighlightOn("\"missing\"")
    }

    fun testLookupOfMissingBeanOnAbstractBaseThatNoBeanExtendsIsNotReported() {
        configureBase("abstract class Base { @$LOOKUP(\"missing\") open fun a(): app.A? = null }")
        assertOwnerIsNotBean("app.Base")
        assertNoHighlightOn("\"missing\"")
    }

    fun testCacheableWithMissingCacheManagerOnImplementationReturnedByBeanFactoryIsReported() {
        addAppFile("Foo.kt", "interface Foo { fun find(): String? }")
        addAppFile(
            "FooConfig.kt",
            "@org.springframework.context.annotation.Configuration open class FooConfig { " +
                "@org.springframework.context.annotation.Bean open fun foo(): Foo = FooImpl() }"
        )
        configureFooImpl()
        assertFactoryBeanDeclaresOnlyInterface()
        assertHighlightOn("\"missing\"")
    }

    fun testCacheableWithMissingCacheManagerOnImplementationThatNoBeanFactoryReturnsIsNotReported() {
        addAppFile("Foo.kt", "interface Foo { fun find(): String? }")
        configureFooImpl()
        assertOwnerIsNotBean("app.FooImpl")
        assertNoHighlightOn("\"missing\"")
    }

    private fun addAppFile(fileName: String, declaration: String) {
        myFixture.addFileToProject("app/$fileName", "package app\n$declaration")
    }

    private fun configureFooImpl() {
        myFixture.configureByText(
            "FooImpl.kt",
            "package app\nopen class FooImpl : Foo { " +
                "@$CACHEABLE(cacheManager = \"missing\") override fun find(): String? = null }"
        )
    }

    private fun assertFactoryBeanDeclaresOnlyInterface() {
        val active = activeBeanClassNames()
        assertTrue("Precondition: an active bean has psiClass app.Foo, got $active", "app.Foo" in active)
        assertFalse("Precondition: app.FooImpl is not an active bean class, got $active", "app.FooImpl" in active)
    }

    private fun addBeanClass(declaration: String) {
        myFixture.addFileToProject("app/Impl.kt", "package app\n$declaration")
    }

    private fun configureBase(declaration: String) {
        myFixture.configureByText("Base.kt", "package app\n$declaration")
    }

    private fun assertSubclassIsBean(beanFqn: String, baseFqn: String) {
        val active = activeBeanClassNames()
        assertTrue("Precondition: $beanFqn is an active bean, got $active", beanFqn in active)
        assertFalse("Precondition: $baseFqn is not an active bean itself, got $active", baseFqn in active)
    }

    private fun configureOwner(packageName: String, member: String, classAnnotation: String = "") {
        myFixture.configureByText(
            "Owner.kt",
            """
            package $packageName
            @org.springframework.context.annotation.Configuration
            $classAnnotation
            open class Owner {
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
