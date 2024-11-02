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
