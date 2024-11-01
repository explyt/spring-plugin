package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.CallBeanMethodFromSomeClassInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary


class CallBeanMethodFromSomeClassInspectionTest : ExplytInspectionKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(CallBeanMethodFromSomeClassInspection::class.java)
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
