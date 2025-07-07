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

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.CallBeanMethodFromSameClassInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary


class CallBeanMethodFromSameClassInspectionTest : ExplytInspectionKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(CallBeanMethodFromSameClassInspection::class.java)
    }

    fun testValidSimpleMethodCall() {
        myFixture.configureByText(
            "SpringComponent.kt",
            """
            @${SpringCoreClasses.COMPONENT}            
            class SpringComponent {
                fun method():String {
		            return methodSimple()
	            }
                	            
	            fun methodSimple():String {
		            return "simple"
	            }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringComponent.kt")
    }

    fun testCacheableMethodCall() {
        myFixture.configureByText(
            "SpringComponent.kt",
            """
            @${SpringCoreClasses.COMPONENT}            
            class SpringComponent {
                fun method():String {
		            return <warning>cacheableMethod()</warning>
	            }

                @${SpringCoreClasses.CACHEABLE}	            
	            fun cacheableMethod():String {
		            return "cacheable"
	            }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringComponent.kt")
    }

    fun testCacheableOnClassMethodCall() {
        myFixture.configureByText(
            "SpringComponent.kt",
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.CACHEABLE}
            class SpringComponent {
                fun method():String {
		            return <warning>cacheableMethod()</warning>; 
	            }
                	            
	            fun cacheableMethod():String {
		            return "cacheable";
	            }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringComponent.kt")
    }

    fun testCacheableFromQualifierCall() {
        myFixture.configureByText(
            "SpringComponent.kt",
            """
            @${SpringCoreClasses.COMPONENT}            
            class SpringComponent {
            
                @${SpringCoreClasses.AUTOWIRED}
                val self: SpringComponent? = null
            
                fun method():String? {
		            return self?.cacheableMethod()
	            }

                @${SpringCoreClasses.CACHEABLE}	            
	            fun cacheableMethod():String {
		            return "cacheable"
	            }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringComponent.kt")
    }
}
