package com.esprito.spring.core.providers.kotlin

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.util.SpringGutterTestUtil
import com.esprito.spring.test.EspritoKotlinLightTestCase
import com.esprito.spring.test.TestLibrary
import junit.framework.TestCase

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

class SpringBeanLineMarkerProviderPriorityTest : EspritoKotlinLightTestCase() {
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

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
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
            // esprito show two beans, but spring starts
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("fooBaseA", true) }
            }.size, 2
        )
    }

    fun testLineMarkerPriorityB_toAutowired() {
        myFixture.configureByText("FooComponent.kt", getFooBeanB() + getStringBean())
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
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

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
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