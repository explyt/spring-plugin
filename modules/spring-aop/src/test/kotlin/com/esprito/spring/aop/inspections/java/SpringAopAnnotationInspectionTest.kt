package com.esprito.spring.aop.inspections.java

import com.esprito.spring.aop.SpringAopClasses
import com.esprito.spring.aop.inspections.SpringAopAnnotationInspection
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.test.EspritoInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary
import junit.framework.TestCase

class SpringAopAnnotationInspectionTest : EspritoInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7, TestLibrary.aspectjWeaver_1_9_7, TestLibrary.springAop_6_0_7
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringAopAnnotationInspection::class.java)
    }

    fun testCorrectConfiguration() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringAopClasses.ASPECT}
            public class SpringBean {
                @${SpringAopClasses.POINTCUT}("@annotation(javax.annotation.PostConstruct)")
                public void annotated() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testInvalidAspectAnnotation() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}            
            public class SpringBean {
                <warning>@${SpringAopClasses.POINTCUT}("@annotation(javax.annotation.PostConstruct)")</warning>
                public void annotated() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")

        val quickFix = myFixture.getAllQuickFixes().firstOrNull()
        TestCase.assertNotNull(quickFix)
        myFixture.launchAction(quickFix!!)
        myFixture.checkResult(
            """
            import org.aspectj.lang.annotation.Aspect;

            @Aspect
            @${SpringCoreClasses.COMPONENT}            
            public class SpringBean {
                @${SpringAopClasses.POINTCUT}("@annotation(javax.annotation.PostConstruct)")
                public void annotated() {}
            }
        """.trimIndent(), true
        )
    }

    fun testInvalidComponentAnnotation() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            <warning>@${SpringAopClasses.ASPECT}</warning>            
            public class SpringBean {
                @${SpringAopClasses.POINTCUT}("@annotation(javax.annotation.PostConstruct)")
                public void annotated() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")

        val quickFix = myFixture.getAllQuickFixes().firstOrNull()
        TestCase.assertNotNull(quickFix)
        myFixture.launchAction(quickFix!!)
        myFixture.checkResult(
            """
            import org.springframework.stereotype.Component;

            @Component
            @${SpringAopClasses.ASPECT}            
            public class SpringBean {
                @${SpringAopClasses.POINTCUT}("@annotation(javax.annotation.PostConstruct)")
                public void annotated() {}
            }
        """.trimIndent(), true
        )
    }
}