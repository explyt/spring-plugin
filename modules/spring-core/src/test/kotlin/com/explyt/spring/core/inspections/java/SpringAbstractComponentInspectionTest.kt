/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.SpringAbstractComponentInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope

class SpringAbstractComponentInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringAbstractComponentInspection::class.java)
        assertSpringClassResolves(SpringCoreClasses.COMPONENT)
        assertSpringClassResolves(SpringCoreClasses.LOOKUP)
    }

    fun testComponentOnAbstractClassWithoutLookup() {
        myFixture.configureByText(
            "Base.java",
            """
            import org.springframework.stereotype.Component;

            <warning descr="${noBean("Component")}">@Component</warning>
            public abstract class Base {}
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testComponentOnAbstractClassWithConcreteSubclassBean() {
        myFixture.addClass(
            """
            @org.springframework.stereotype.Component
            public class Impl extends Base {}
            """.trimIndent()
        )
        myFixture.configureByText(
            "Base.java",
            """
            import org.springframework.stereotype.Component;

            <warning descr="${subclassBean("Component")}">@Component</warning>
            public abstract class Base {}
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testServiceOnAbstractClass() {
        assertSpringClassResolves("org.springframework.stereotype.Service")
        myFixture.configureByText(
            "BaseService.java",
            """
            import org.springframework.stereotype.Service;

            <warning descr="${noBean("Service")}">@Service</warning>
            public abstract class BaseService {}
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testCustomStereotypeOnAbstractClass() {
        myFixture.addClass(
            """
            package demo;

            @org.springframework.stereotype.Component
            @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
            public @interface Handler {}
            """.trimIndent()
        )
        myFixture.configureByText(
            "BaseHandler.java",
            """
            import demo.Handler;

            <warning descr="${noBean("Handler")}">@Handler</warning>
            public abstract class BaseHandler {}
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testAbstractClassWithLookupMethodIsNotReported() {
        myFixture.configureByText(
            "Base.java",
            """
            import org.springframework.beans.factory.annotation.Lookup;
            import org.springframework.stereotype.Component;

            @Component
            public abstract class Base {
                @Lookup
                protected abstract Object prototype();
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testConcreteComponentIsNotReported() {
        myFixture.configureByText(
            "Concrete.java",
            """
            import org.springframework.stereotype.Component;

            @Component
            public class Concrete {}
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testComponentOnInterfaceIsNotReported() {
        myFixture.configureByText(
            "Gateway.java",
            """
            import org.springframework.stereotype.Component;

            @Component
            public interface Gateway {}
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testAbstractClassWithoutStereotypeIsNotReported() {
        myFixture.configureByText(
            "Base.java",
            """
            public abstract class Base {}
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testRemoveAnnotationQuickFix() {
        myFixture.configureByText(
            "Base.java",
            """
            @org.springframework.stereotype.<caret>Component
            public abstract class Base {}
            """.trimIndent()
        )
        val fix = myFixture.findSingleIntention(SpringCoreBundle.message("explyt.spring.inspection.abstract.component.fix"))
        myFixture.checkPreviewAndLaunchAction(fix)
        myFixture.checkResult(
            """
            public abstract class Base {}
            """.trimIndent()
        )
    }

    private fun assertSpringClassResolves(fqn: String) {
        assertNotNull(
            "$fqn must resolve from the test library",
            JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))
        )
    }

    private fun noBean(annotation: String) =
        SpringCoreBundle.message("explyt.spring.inspection.abstract.component.no.bean", annotation)

    private fun subclassBean(annotation: String) =
        SpringCoreBundle.message("explyt.spring.inspection.abstract.component.subclass.bean", annotation)
}
