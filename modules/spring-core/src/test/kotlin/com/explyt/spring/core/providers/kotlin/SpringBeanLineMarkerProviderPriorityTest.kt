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

package com.explyt.spring.core.providers.kotlin

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import junit.framework.TestCase

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

class SpringBeanLineMarkerProviderPriorityTest : ExplytKotlinLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    private fun getFooBeanA(): String {
        return """
                    import org.springframework.beans.factory.annotation.Autowired;
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.stereotype.Component;
    
                    @Component
                    internal class FooBaseA(var foo: String)

                    @Configuration
                    internal open class FooBean {
                        @Bean
                        open fun fooBaseA(): FooBaseA {
                            return FooBaseA("from bean FooBaseA")
                        }
                    }

                    @Component
                    internal class FooCandidate @Autowired constructor(fooBaseA: FooBaseA)
                    
                """.trimIndent()
    }

    private fun getFooBeanB(): String {
        return """
                    import org.springframework.beans.factory.annotation.Autowired;
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.context.annotation.Primary;
                    import org.springframework.stereotype.Component;
    
                    @Component
                    @Primary
                    internal class FooBaseB(var foo: String)

                    @Configuration
                    internal open class FooBean {
                        @Bean
                        open fun fooBaseB(): FooBaseB {
                            return FooBaseB("from bean FooBaseB")
                        }
                    }

                    @Component
                    internal class FooCandidate @Autowired constructor(fooBaseB: FooBaseB)
                    
                """.trimIndent()
    }

    private fun getFooBeanC(): String {
        return """
                    import org.springframework.beans.factory.annotation.Autowired;
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.context.annotation.Primary;
                    import org.springframework.stereotype.Component;
    
                    @Component("nameBeanC")
                    @Primary
                    internal class FooBaseC(var foo: String)

                    @Configuration
                    internal open class FooBean {
                        @Bean
                        open fun fooBaseC(): FooBaseC {
                            return FooBaseC("from bean FooBaseС")
                        }
                    }

                    @Component
                    internal class FooCandidate @Autowired constructor(nameBeanC: FooBaseC)
                                        
                """.trimIndent()
    }

    fun testLineMarkerPriorityA_toAutowired() {
        myFixture.configureByText("FooComponent.kt", getFooBeanA() + getStringBean())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean, SpringIcons.springBeanInactive)
        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "fooBaseA" } }.size, 2
        )
    }

    fun testLineMarkerPriorityA_toBean() {
        myFixture.configureByText("FooComponent.kt", getFooBeanA() + getStringBean())
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            // explyt show two beans, but spring starts
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("fooBaseA", true) }
            }.size, 2
        )
    }

    fun testLineMarkerPriorityB_toAutowired() {
        myFixture.configureByText("FooComponent.kt", getFooBeanB() + getStringBean())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean, SpringIcons.springBeanInactive)
        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "fooBaseB" } }.size, 2
        )
    }

    fun testLineMarkerPriorityB_toBean() {
        myFixture.configureByText("FooComponent.kt", getFooBeanB() + getStringBean())
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "FooBaseB" } }.size, 1
        )
    }

    fun testLineMarkerPriorityC_toAutowired() {
        myFixture.configureByText("FooComponent.kt", getFooBeanC() + getStringBean())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean, SpringIcons.springBeanInactive)
        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "nameBeanC" } }.size, 2
        )
    }

    fun testLineMarkerPriorityC_toBean() {
        myFixture.configureByText("FooComponent.kt", getFooBeanC() + getStringBean())
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "FooBaseC" } }.size, 1
        )
    }

    private fun getStringBean(): String {
        return """
            @org.springframework.context.annotation.Configuration
            internal open class FooStringBean {
                @Bean 
                open fun text(): String {
                    return "text"
                }
            }

        """.trimIndent()
    }
}