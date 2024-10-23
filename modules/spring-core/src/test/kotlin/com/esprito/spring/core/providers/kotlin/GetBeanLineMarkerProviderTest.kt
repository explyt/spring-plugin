package com.esprito.spring.core.providers.kotlin

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.test.EspritoKotlinLightTestCase
import com.esprito.spring.test.TestLibrary
import com.esprito.spring.test.util.SpringGutterTestUtil

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

class GetBeanLineMarkerProviderTest : EspritoKotlinLightTestCase() {
    override fun getTestDataPath(): String =
        super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testGetByName() {
        myFixture.configureByText(
            "FooComponent.kt",
            """
                import org.springframework.context.ApplicationContext
                import org.springframework.stereotype.Component
                
                @Component
                class Foo

                @Component
                 class JustTest {
                    fun justTest(context: ApplicationContext) {
                        val foo = context.getBean("foo")
                        val foo1 = context.getBean("foo") as Foo
                        val foo2 = context.getBean(Foo::class.java)
                        val foo3: Any = context.getBean("foo", Foo::class.java)
                        val foo4 = context.getBean("foo", "")
                        val foo5 = context.getBean(Foo::class.java, "")
                    }
                }
            """.trimIndent()
        )
        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        assertEquals(
            6,
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it == "Foo" }
            }.size
        )
    }

}