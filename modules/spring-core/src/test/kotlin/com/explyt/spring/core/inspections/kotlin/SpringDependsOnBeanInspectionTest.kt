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

import com.explyt.spring.core.inspections.SpringDependsOnBeanInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language

class SpringDependsOnBeanInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringDependsOnBeanInspection::class.java)
    }

    fun testMethodsAliasFor() {
        @Language("kotlin")
        val testClass = """          
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

interface I

@Component
internal class A : I

@Service
internal class B : I

@Configuration
open class DependsOnConfiguration {
    @DependsOn("a")
    @Bean
    open fun bDependentOnA(): I {
        return B()
    }
    
    // Ultimate handles it wrong. There is no navigation, nor error bean highlight
    @DependsOn(<error descr="Incorrect bean reference at @DependsOn">"wrongBeanNameAtMethod"</error>) 
    @Bean
    open fun bDependentOnWrong(): I {
        return B()
    }
}

//region Valid
@Component
@DependsOn("a")
internal class DependentOnABean

@Component
@DependsOn("a", "b", E.CONST_TO_VALID_BEAN)
internal class DependentOnMultipleBeans

//endregion
//region Invalid
@Component
@DependsOn(<error descr="Incorrect bean reference at @DependsOn">"wrongBeanNameAtClass"</error>)
internal class DependentOnABeanErr

@Component
@DependsOn(<error descr="Incorrect bean reference at @DependsOn">"wrongBeanNameListedAtClass"</error>, "a", <error descr="Incorrect bean reference at @DependsOn">E.CONST_TO_NON_EXISTING_BEAN</error>)
internal class DependentOnMultipleBeansErr

//endregion
@Component
internal object E : I {
    const val CONST_TO_VALID_BEAN: String = "b"
    const val CONST_TO_NON_EXISTING_BEAN: String = "nope"
}
        """.trimIndent()
        myFixture.configureByText("DependsOnConfiguration.kt", testClass)
        myFixture.testHighlighting("DependsOnConfiguration.kt")
    }
}