/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.CallBeanMethodFromSameClassInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary


class CallBeanMethodFromSameClassInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(CallBeanMethodFromSameClassInspection::class.java)
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
