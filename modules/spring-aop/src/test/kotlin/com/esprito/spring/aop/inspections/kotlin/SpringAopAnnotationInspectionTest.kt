package com.esprito.spring.aop.inspections.kotlin

import com.esprito.spring.aop.SpringAopClasses
import com.esprito.spring.aop.inspections.SpringAopAnnotationInspection
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.test.EspritoInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import junit.framework.TestCase

class SpringAopAnnotationInspectionTest : EspritoInspectionKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7, TestLibrary.aspectjWeaver_1_9_7, TestLibrary.springAop_6_0_7
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringAopAnnotationInspection::class.java)
    }

    fun testCorrectConfiguration() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringAopClasses.ASPECT}
            class SpringBean {
                @${SpringAopClasses.POINTCUT}("@annotation(javax.annotation.PostConstruct)")
                fun annotated() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testInvalidAspectAnnotation() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}            
            class SpringBean {
                <warning>@${SpringAopClasses.POINTCUT}("@annotation(javax.annotation.PostConstruct)")</warning>
                fun annotated() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")

        val quickFix = myFixture.getAllQuickFixes().firstOrNull()
        TestCase.assertNotNull(quickFix)
        myFixture.launchAction(quickFix!!)
        myFixture.checkResult(
            """
            import org.aspectj.lang.annotation.Aspect

            @Aspect
            @${SpringCoreClasses.COMPONENT}            
            class SpringBean {
                @${SpringAopClasses.POINTCUT}("@annotation(javax.annotation.PostConstruct)")
                fun annotated() {}
            }
        """.trimIndent(), true
        )
    }

    fun testInvalidComponentAnnotation() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            <warning>@${SpringAopClasses.ASPECT}</warning>            
            class SpringBean {
                @${SpringAopClasses.POINTCUT}("@annotation(javax.annotation.PostConstruct)")
                fun annotated() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")

        val quickFix = myFixture.getAllQuickFixes().firstOrNull()
        TestCase.assertNotNull(quickFix)
        myFixture.launchAction(quickFix!!)
        myFixture.checkResult(
            """
            import org.springframework.stereotype.Component

            @Component
            @${SpringAopClasses.ASPECT}            
            class SpringBean {
                @${SpringAopClasses.POINTCUT}("@annotation(javax.annotation.PostConstruct)")
                fun annotated() {}
            }
        """.trimIndent(), true
        )
    }
}