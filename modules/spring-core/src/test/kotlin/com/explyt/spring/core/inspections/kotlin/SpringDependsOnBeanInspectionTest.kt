/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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