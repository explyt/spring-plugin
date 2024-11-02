/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.aop.inspections.java

import com.explyt.spring.aop.SpringAopClasses
import com.explyt.spring.aop.inspections.SpringAopAnnotationInspection
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import junit.framework.TestCase

class SpringAopAnnotationInspectionTest : ExplytInspectionJavaTestCase() {
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