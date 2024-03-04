package com.esprito.spring.core.inspections.kotlin

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.inspections.CallBeanMethodFromSomeClassInspection
import com.esprito.spring.test.EspritoInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary


class CallBeanMethodFromSomeClassInspectionTest : EspritoInspectionKotlinTestCase() {
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
