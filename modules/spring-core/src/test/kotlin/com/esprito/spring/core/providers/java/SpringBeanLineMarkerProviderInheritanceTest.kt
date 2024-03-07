package com.esprito.spring.core.providers.java

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.util.SpringGutterTestUtil.getAllBeanGuttersByIcon
import com.esprito.spring.core.util.SpringGutterTestUtil.getGutterTargetString
import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import junit.framework.TestCase

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

class SpringBeanLineMarkerProviderInheritanceTest : EspritoJavaLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.javax_inject_1,
        TestLibrary.jakarta_annotation_2_1_1
    )

    fun testLineMarkerInheritance_toAutowired_I_a() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired I a; 
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "a" }
        }.size, 3)
    }

    fun testLineMarkerInheritance_toBean_I_a() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired I a;
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "A" }
        }.size, 1)
    }

    fun testLineMarkerInheritance_toAutowired_I_ab() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired I ab; 
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "ab" }
        }.size, 3)
    }

    fun testLineMarkerInheritance_toBean_I_ab() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired I ab; 
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "A" || it == "B" }
        }.size, 2)
    }

    fun testLineMarkerInheritance_toAutowired_I_c() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired I c;
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "c" }
        }.size, 3)
    }

    fun testLineMarkerInheritance_toBean_I_c() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired I c;
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "A" || it == "B" }
        }.size, 2)
    }

    fun testLineMarkerInheritance_toAutowired_I_e() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired I e;
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "e" }
        }.size, 3)
    }

    fun testLineMarkerInheritance_toBean_I_e() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired I e;
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "A" || it == "B" }
        }.size, 2)
    }

    fun testLineMarkerInheritance_toAutowired_E_abe() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired E abe;
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "abe" }
        }.size, 3)
    }

    fun testLineMarkerInheritance_toBean_E_abe() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired E abe;
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "A" || it == "B" || it == "E" }
        }.size, 3)
    }

    fun testLineMarkerInheritance_toAutowired_E_a() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired E a;
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "a" }
        }.size, 3)
    }

    fun testLineMarkerInheritance_toBean_E_a() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired E a;
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "A" }
        }.size, 1)
    }

    fun testLineMarkerInheritance_toAutowired_E_b() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired E b;
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "b" }
        }.size, 3)
    }

    fun testLineMarkerInheritance_toBean_E_b() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired E b;
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "B" }
        }.size, 1)
    }

    fun testLineMarkerInheritance_toAutowired_C_c() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired C c;
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerInheritance_toBean_C_c() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired C c;
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerInheritance_toAutowired_Inject_E_b() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @javax.inject.Inject E b;
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "b" }
        }.size, 3)
    }

    fun testLineMarkerInheritance_toBean_Inject_E_b() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @javax.inject.Inject E b;
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "B" }
        }.size, 1)
    }

    fun testLineMarkerInheritance_toAutowired_Resource_E_e() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @jakarta.annotation.Resource E e;
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "e" }
        }.size, 3)
    }

    fun testLineMarkerInheritance_toBean_Resource_E_e() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @jakarta.annotation.Resource E e;
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "E" }
        }.size, 1)
    }

    private fun getInheritanceClasses(): String {
        return """
            import org.springframework.stereotype.Component;

            interface I {}

            @Component
            class E {}

            @Component
            class A extends E implements I {}

            @Component
            class B extends E implements I {}

            class C implements I {}

        """.trimIndent()
    }

}