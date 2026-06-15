/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.inspections.SpringUnknownBeanMethodInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language

class SpringUnknownBeanMethodInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringUnknownBeanMethodInspection::class.java)
    }

    fun testMethodsInitDestroy() {
        @Language("kotlin")
        val testClass = """
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

interface I {
    fun existingMethod()
}

abstract class A : I

@Component
class B : A() {
    override fun existingMethod() {}
}

@Configuration
open class MethodsInitDestroyConfiguration {
    @Bean
    open fun i(): I {
        return B()
    }

    @Bean
    open fun i1(): I {
        return B()
    }

    @Bean(initMethod = "")
    open fun i2(): I {
        return B()
    }

    @Bean(initMethod = "")
    open fun i3(): I {
        return B()
    }

    @Bean(destroyMethod = "")
    open fun i4(): I {
        return B()
    }

    @Bean(destroyMethod = EXISTING_METHOD)
    open fun i5(): I {
        return B()
    }

    @Bean(destroyMethod = <error descr="Cannot find method">NOT_EXISTING_METHOD</error>)
    open fun i6(): I {
        return B()
    } //expecting error

    @Bean(initMethod = EXISTING_METHOD, destroyMethod = EXISTING_METHOD)
    open fun a(): A {
        return B()
    }

    @Bean
    open fun b(): B {
        return B()
    }

    @Bean(initMethod = EXISTING_METHOD)
    open fun b1(): B {
        return B()
    }

    @Bean(initMethod = <error descr="Cannot find method">NOT_EXISTING_METHOD</error>)
    open fun b2(): B {
        return B()
    } //expecting error

    @Bean(destroyMethod = <error descr="Cannot find method">"foo"</error>)
    open fun b3(): B {
        return B()
    } //expecting error

    companion object {
        private const val EXISTING_METHOD = "existingMethod"
        private const val NOT_EXISTING_METHOD = "notExistingMethod"
    }
}
        """.trimIndent()

        myFixture.configureByText("MethodsInitDestroyConfiguration.kt", testClass)
        myFixture.testHighlighting("MethodsInitDestroyConfiguration.kt")
    }


}