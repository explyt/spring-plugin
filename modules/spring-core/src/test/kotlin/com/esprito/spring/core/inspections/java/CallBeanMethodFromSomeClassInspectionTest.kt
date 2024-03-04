package com.esprito.spring.core.inspections.java

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.inspections.CallBeanMethodFromSomeClassInspection
import com.esprito.spring.test.EspritoInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary


class CallBeanMethodFromSomeClassInspectionTest : EspritoInspectionJavaTestCase() {
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
