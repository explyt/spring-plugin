package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

class SpringCacheableAnnotationInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(com.explyt.spring.core.inspections.SpringCacheableAnnotationInspection::class.java)
        myFixture.addClass(
            """
            import org.springframework.cache.CacheManager;
            import org.springframework.cache.interceptor.SimpleCacheResolver;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            public class CustomResolver extends SimpleCacheResolver {
            	public CustomResolver(CacheManager cacheManager) {
            		super(cacheManager);
            	}
            }
        """.trimIndent()
        )
        myFixture.addClass(
            """
            import org.springframework.cache.support.SimpleCacheManager;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            public class CustomManager extends SimpleCacheManager {
            }
        """.trimIndent()
        )
        myFixture.addClass(
            """
            import org.springframework.cache.interceptor.SimpleKeyGenerator;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            public class CustomKeyGenerator extends SimpleKeyGenerator {}
        """.trimIndent()
        )
    }

    fun testCorrectCacheableClass() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.CACHEABLE}(cacheResolver = "customResolver", cacheManager = "customManager", keyGenerator = "customKeyGenerator")
            class SpringBean {}
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testCorrectCacheableMethod() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}            
            class SpringBean {
                @${SpringCoreClasses.CACHEABLE}(cacheResolver = "customResolver")
                fun test():String { return ""}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testPrivateCacheableMethod() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}            
            public class SpringBean {
                @${SpringCoreClasses.CACHEABLE}(cacheResolver = "customResolver")
                private fun <warning>test</warning>():String { return ""}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testCacheableIncorrectResolverMethod() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}            
            public class SpringBean {
                @${SpringCoreClasses.CACHEABLE}(cacheResolver = <warning>"myResolver"</warning>, 
                                                cacheManager = <warning>"myManager"</warning>, 
                                                keyGenerator = <warning>"myGenerator"</warning>)
                fun test():String { return ""}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }
}
