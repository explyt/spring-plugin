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

package com.explyt.spring.core.providers.java

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import junit.framework.TestCase

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

class SpringBeanLineMarkerProviderPriorityTest : ExplytJavaLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    private fun getFooBeanA(): String {
        return """
                    import org.springframework.beans.factory.annotation.Autowired;
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.stereotype.Component;
    
                    @Component
                    class FooBaseA {
                        public String foo;
                        public FooBaseA(String value) {
                            foo = value;
                        }
                    }
                    
                    @Configuration
                    class FooBean {
                        @Bean FooBaseA fooBaseA() { return new FooBaseA("from bean FooBaseA"); }
                    }
                    
                    @Component
                    class FooCandidate {
                        @Autowired FooCandidate(FooBaseA fooBaseA) {}
                    }
                    
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
                    class FooBaseB {
                        public String foo;
                        public FooBaseB(String value) {
                            foo = value;
                        }
                    }
                                        
                    @Configuration
                    class FooBean {
                        @Bean FooBaseB fooBaseB() { return new FooBaseB("from bean FooBaseB"); }
                    }
                    
                    @Component
                    class FooCandidate {
                        @Autowired FooCandidate(FooBaseB fooBaseB) {}
                    }
                    
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
                    class FooBaseC {
                        public String foo;
                        public FooBaseC(String value) {
                            foo = value;
                        }
                    }

                    @Configuration
                    class FooBean {
                        @Bean FooBaseC fooBaseC() { return new FooBaseC("from bean FooBaseC"); }
                    }

                    @Component
                    class FooCandidate {
                        @Autowired FooCandidate(FooBaseC nameBeanC) {}
                    }
                                        
                """.trimIndent()
    }

    fun testLineMarkerPriorityA_toAutowired() {
        myFixture.configureByText("FooComponent.java", getFooBeanA() + getStringBean())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean, SpringIcons.springBeanInactive)
        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "fooBaseA" } }.size, 2
        )
    }

    fun testLineMarkerPriorityA_toBean() {
        myFixture.configureByText("FooComponent.java", getFooBeanA() + getStringBean())
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
        myFixture.configureByText("FooComponent.java", getFooBeanB() + getStringBean())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean, SpringIcons.springBeanInactive)
        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "fooBaseB" } }.size, 2
        )
    }

    fun testLineMarkerPriorityB_toBean() {
        myFixture.configureByText("FooComponent.java", getFooBeanB() + getStringBean())
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "FooBaseB" } }.size, 1
        )
    }

    fun testLineMarkerPriorityC_toAutowired() {
        myFixture.configureByText("FooComponent.java", getFooBeanC() + getStringBean())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean, SpringIcons.springBeanInactive)
        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "nameBeanC" } }.size, 2
        )
    }

    fun testLineMarkerPriorityC_toBean() {
        myFixture.configureByText("FooComponent.java", getFooBeanC() + getStringBean())
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
            class FooStringBean {
                @org.springframework.context.annotation.Bean
                String text() { return new String("text"); }
            }
        """.trimIndent()
    }
}