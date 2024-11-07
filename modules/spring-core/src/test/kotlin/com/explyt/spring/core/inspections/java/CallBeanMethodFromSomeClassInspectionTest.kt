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

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.CallBeanMethodFromSomeClassInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary


class CallBeanMethodFromSomeClassInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(CallBeanMethodFromSomeClassInspection::class.java)
    }

    fun testValidSimpleMethodCall() {
        myFixture.configureByText(
            "SpringComponent.java",
            """
            @${SpringCoreClasses.COMPONENT}            
            public class SpringComponent {
                public String method() {
		            return methodSimple(); 
	            }
                	            
	            public String methodSimple() {
		            return "simple";
	            }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringComponent.java")
    }

    fun testCacheableMethodCall() {
        myFixture.configureByText(
            "SpringComponent.java",
            """
            @${SpringCoreClasses.COMPONENT}            
            public class SpringComponent {
                public String method() {
		            return <warning>cacheableMethod()</warning>; 
	            }

                @${SpringCoreClasses.CACHEABLE}	            
	            public String cacheableMethod() {
		            return "cacheable";
	            }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringComponent.java")
    }

    fun testCacheableOnClassMethodCall() {
        myFixture.configureByText(
            "SpringComponent.java",
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.CACHEABLE}
            public class SpringComponent {
                public String method() {
		            return <warning>cacheableMethod()</warning>; 
	            }
                	            
	            public String cacheableMethod() {
		            return "cacheable";
	            }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringComponent.java")
    }

    fun testCacheableFromQualifierCall() {
        myFixture.configureByText(
            "SpringComponent.java",
            """
            @${SpringCoreClasses.COMPONENT}            
            public class SpringComponent {
                @${SpringCoreClasses.AUTOWIRED}
                SpringComponent self;
            
                public String method() {
		            return self.cacheableMethod(); 
	            }
                	            
                @${SpringCoreClasses.CACHEABLE}
	            public String cacheableMethod() {
		            return "cacheable";
	            }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringComponent.java")
    }
}
