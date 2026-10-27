/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.SpringAbstractComponentInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope

class SpringAbstractComponentInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springBootTest_3_2_3
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringAbstractComponentInspection::class.java)
        assertSpringClassResolves(SpringCoreClasses.COMPONENT)
        assertSpringClassResolves(SpringCoreClasses.LOOKUP)
    }

    fun testComponentOnAbstractClassWithoutLookup() {
        myFixture.configureByText(
            "Base.kt",
            """
            import org.springframework.stereotype.Component

            <warning descr="${noBean("Component")}">@Component</warning>
            abstract class Base
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testComponentOnAbstractClassWithConcreteSubclassBean() {
        myFixture.configureByText(
            "Base.kt",
            """
            import org.springframework.stereotype.Component

            <warning descr="${subclassBean("Component")}">@Component</warning>
            abstract class Base

            @Component
            class Impl : Base()
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testServiceOnAbstractClass() {
        assertSpringClassResolves("org.springframework.stereotype.Service")
        myFixture.configureByText(
            "BaseService.kt",
            """
            import org.springframework.stereotype.Service

            <warning descr="${noBean("Service")}">@Service</warning>
            abstract class BaseService
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testCustomStereotypeOnAbstractClass() {
        myFixture.configureByText(
            "BaseHandler.kt",
            """
            import org.springframework.stereotype.Component

            @Component
            @Retention(AnnotationRetention.RUNTIME)
            annotation class Handler

            <warning descr="${noBean("Handler")}">@Handler</warning>
            abstract class BaseHandler
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testAbstractClassWithLookupMethodIsNotReported() {
        myFixture.configureByText(
            "Base.kt",
            """
            import org.springframework.beans.factory.annotation.Lookup
            import org.springframework.stereotype.Component

            @Component
            abstract class Base {
                @Lookup
                protected abstract fun prototype(): Any
            }
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testConcreteComponentIsNotReported() {
        myFixture.configureByText(
            "Concrete.kt",
            """
            import org.springframework.stereotype.Component

            @Component
            class Concrete
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testComponentOnInterfaceIsNotReported() {
        myFixture.configureByText(
            "Gateway.kt",
            """
            import org.springframework.stereotype.Component

            @Component
            interface Gateway
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testAbstractClassWithoutStereotypeIsNotReported() {
        myFixture.configureByText(
            "Base.kt",
            """
            abstract class Base
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testRemoveAnnotationQuickFix() {
        myFixture.configureByText(
            "Base.kt",
            """
            @org.springframework.stereotype.<caret>Component
            abstract class Base
            """.trimIndent()
        )
        val fix = myFixture.findSingleIntention(SpringCoreBundle.message("explyt.spring.inspection.abstract.component.fix"))
        myFixture.checkPreviewAndLaunchAction(fix)
        myFixture.checkResult(
            """
            abstract class Base
            """.trimIndent()
        )
    }

    fun testRestControllerOnAbstractBaseOfControllerBeanIsNotReported() {
        assertSpringClassResolves(REST_CONTROLLER)
        myFixture.configureByText(
            "BaseController.kt",
            """
            @$REST_CONTROLLER
            abstract class BaseController

            @$REST_CONTROLLER
            class ItemController : BaseController()
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testControllerOnAbstractClassWithoutSubclassIsNotReported() {
        assertSpringClassResolves(CONTROLLER)
        myFixture.configureByText(
            "BaseController.kt",
            """
            @$CONTROLLER
            abstract class BaseController
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testControllerAdviceOnAbstractClassIsNotReported() {
        assertSpringClassResolves(CONTROLLER_ADVICE)
        myFixture.configureByText(
            "BaseAdvice.kt",
            """
            @$CONTROLLER_ADVICE
            abstract class BaseAdvice
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testRepositoryOnAbstractClassIsNotReported() {
        assertSpringClassResolves(REPOSITORY)
        myFixture.configureByText(
            "BaseRepository.kt",
            """
            @$REPOSITORY
            abstract class BaseRepository
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testCustomControllerStereotypeOnAbstractClassIsNotReported() {
        assertSpringClassResolves(CONTROLLER)
        myFixture.configureByText(
            "BaseApi.kt",
            """
            @$CONTROLLER
            @Retention(AnnotationRetention.RUNTIME)
            annotation class ApiController

            @ApiController
            abstract class BaseApi
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testServiceOnAbstractClassIsReportedBesideWebStereotypes() {
        assertSpringClassResolves(REST_CONTROLLER)
        myFixture.configureByText(
            "BaseService.kt",
            """
            import org.springframework.stereotype.Service

            <warning descr="${noBean("Service")}">@Service</warning>
            abstract class BaseService

            @$REST_CONTROLLER
            class ItemController
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun testAbstractSpringBootTestBaseIsNotReported() {
        assertSpringClassResolves(SPRING_BOOT_TEST)
        myFixture.configureByText(
            "BaseIntegrationTest.kt",
            """
            @$SPRING_BOOT_TEST
            abstract class BaseIntegrationTest
            """.trimIndent()
        )
        myFixture.checkHighlighting()
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

    private companion object {
        const val CONTROLLER = "org.springframework.stereotype.Controller"
        const val REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController"
        const val CONTROLLER_ADVICE = "org.springframework.web.bind.annotation.ControllerAdvice"
        const val REPOSITORY = "org.springframework.stereotype.Repository"
        const val SPRING_BOOT_TEST = "org.springframework.boot.test.context.SpringBootTest"
    }
}
