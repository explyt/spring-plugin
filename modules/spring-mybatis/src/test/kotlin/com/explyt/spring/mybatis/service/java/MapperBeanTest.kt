/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.mybatis.service.java

import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope

private const val MAPPER = "org.apache.ibatis.annotations.Mapper"
private const val MAPPER_SCAN = "org.mybatis.spring.annotation.MapperScan"
private const val MYBATIS_AUTO_CONFIGURATION = "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
private const val ORDER_MAPPER = "com.app.mapper.OrderMapper"

class MapperBeanMybatis3Test : MapperBeanTest() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.mybatis_3_5_19,
        TestLibrary.mybatisSpring_3_0_5,
        TestLibrary.mybatisSpringBootAutoConfigure_3_0_5,
        TestLibrary.springBootAutoConfigure_3_5_0
    )
}

class MapperBeanMybatis4Test : MapperBeanTest() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.mybatis_3_5_19,
        TestLibrary.mybatisSpring_4_1_0,
        TestLibrary.mybatisSpringBootAutoConfigure_4_1_0,
        TestLibrary.springBootAutoConfigure_4_1_0
    )
}

class MapperBeanWithoutBootAutoConfigurationMybatis3Test : MapperBeanWithoutBootAutoConfigurationTest() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.mybatis_3_5_19, TestLibrary.mybatisSpring_3_0_5, TestLibrary.springBootAutoConfigure_3_5_0
    )
}

class MapperBeanWithoutBootAutoConfigurationMybatis4Test : MapperBeanWithoutBootAutoConfigurationTest() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.mybatis_3_5_19, TestLibrary.mybatisSpring_4_1_0, TestLibrary.springBootAutoConfigure_4_1_0
    )
}

abstract class MapperBeanTest : MybatisJavaLightTestCase() {

    fun testMapperScanRegistersEveryInterfaceInItsPackage() {
        addConfiguration("com.app", "@MapperScan(\"com.app.mapper\")")
        addMapper(ORDER_MAPPER)
        assertMapperFixture(ORDER_MAPPER)

        assertBeanNames(ORDER_MAPPER, setOf("orderMapper"))
    }

    fun testInterfaceOutsideMapperScanPackagesIsNotBean() {
        addConfiguration("com.app", "@MapperScan(\"com.app.mapper\")")
        addMapper(ORDER_MAPPER)
        addMapper("com.app.other.OtherMapper")
        assertMapperFixture("com.app.other.OtherMapper")

        assertBeanNames(ORDER_MAPPER, setOf("orderMapper"))
        assertBeanNames("com.app.other.OtherMapper", emptySet())
    }

    fun testMapperScanAnnotationClassNarrowsToAnnotatedInterfaces() {
        addConfiguration(
            "com.app",
            "@MapperScan(value = \"com.app.mapper\", annotationClass = org.apache.ibatis.annotations.Mapper.class)"
        )
        addMapper(ORDER_MAPPER, "@Mapper")
        addMapper("com.app.mapper.PlainMapper")
        assertMapperFixture(ORDER_MAPPER)
        assertMapperFixture("com.app.mapper.PlainMapper")
        assertNotNull("OrderMapper must carry a resolved @Mapper", mapperClass(ORDER_MAPPER).getAnnotation(MAPPER))

        assertBeanNames(ORDER_MAPPER, setOf("orderMapper"))
        assertBeanNames("com.app.mapper.PlainMapper", emptySet())
    }

    fun testMapperScanMarkerInterfaceNarrowsToSubInterfaces() {
        myFixture.addFileToProject(
            "com/app/mapper/BaseMapper.java",
            "package com.app.mapper;\npublic interface BaseMapper {}"
        )
        addConfiguration(
            "com.app",
            "@MapperScan(value = \"com.app.mapper\", markerInterface = com.app.mapper.BaseMapper.class)"
        )
        addMapper(ORDER_MAPPER, extends = "BaseMapper")
        addMapper("com.app.mapper.PlainMapper")
        assertMapperFixture(ORDER_MAPPER)
        assertMapperFixture("com.app.mapper.PlainMapper")
        assertTrue(
            "OrderMapper must extend BaseMapper",
            mapperClass(ORDER_MAPPER).isInheritor(mapperClass("com.app.mapper.BaseMapper"), false)
        )

        assertBeanNames(ORDER_MAPPER, setOf("orderMapper"))
        assertBeanNames("com.app.mapper.PlainMapper", emptySet())
    }

    fun testMapperScanBasePackageClassesBoundsMappers() {
        myFixture.addFileToProject("com/app/mapper/Marker.java", "package com.app.mapper;\npublic class Marker {}")
        addConfiguration("com.app", "@MapperScan(basePackageClasses = com.app.mapper.Marker.class)")
        addMapper(ORDER_MAPPER)
        addMapper("com.app.other.OtherMapper")
        assertMapperFixture(ORDER_MAPPER)
        assertMapperFixture("com.app.other.OtherMapper")

        assertBeanNames(ORDER_MAPPER, setOf("orderMapper"))
        assertBeanNames("com.app.other.OtherMapper", emptySet())
    }

    fun testBootAutoConfigurationRegistersOnlyAnnotatedMappersInApplicationPackage() {
        addBootApplication("com.app")
        addMapper(ORDER_MAPPER, "@Mapper")
        addMapper("com.app.mapper.PlainMapper")
        assertMapperFixture(ORDER_MAPPER)
        assertMapperFixture("com.app.mapper.PlainMapper")
        findClass(MYBATIS_AUTO_CONFIGURATION, GlobalSearchScope.allScope(project))

        assertBeanNames(ORDER_MAPPER, setOf("orderMapper"))
        assertBeanNames("com.app.mapper.PlainMapper", emptySet())
    }
}

abstract class MapperBeanWithoutBootAutoConfigurationTest : MybatisJavaLightTestCase() {

    fun testAnnotatedMapperIsNotBeanWithoutMapperScanOrBootAutoConfiguration() {
        addBootApplication("com.app")
        addMapper(ORDER_MAPPER, "@Mapper")
        assertMapperFixture(ORDER_MAPPER)
        assertNull(
            "$MYBATIS_AUTO_CONFIGURATION must be absent",
            JavaPsiFacade.getInstance(project).findClass(MYBATIS_AUTO_CONFIGURATION, GlobalSearchScope.allScope(project))
        )

        assertBeanNames(ORDER_MAPPER, emptySet())
    }
}

abstract class MybatisJavaLightTestCase : ExplytJavaLightTestCase() {

    override fun setUp() {
        super.setUp()
        Registry.get("explyt.spring.root.runConfiguration").setValue(false)
    }

    override fun tearDown() {
        super.tearDown()
        Registry.get("explyt.spring.root.runConfiguration").resetToDefault()
    }

    protected fun addConfiguration(packageName: String, annotations: String) {
        myFixture.addFileToProject(
            "${packageName.replace('.', '/')}/App.java", """
            package $packageName;
            import org.mybatis.spring.annotation.MapperScan;
            import org.springframework.context.annotation.ComponentScan;
            import org.springframework.context.annotation.Configuration;
            @Configuration
            @ComponentScan
            $annotations
            public class App {}
            """.trimIndent()
        )
    }

    protected fun addBootApplication(packageName: String) {
        myFixture.addFileToProject(
            "${packageName.replace('.', '/')}/App.java", """
            package $packageName;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            @SpringBootApplication
            public class App {}
            """.trimIndent()
        )
    }

    protected fun addMapper(qualifiedName: String, annotation: String = "", extends: String? = null) {
        val packageName = qualifiedName.substringBeforeLast('.')
        val simpleName = qualifiedName.substringAfterLast('.')
        val extendsClause = extends?.let { " extends $it" }.orEmpty()
        myFixture.addFileToProject(
            "${qualifiedName.replace('.', '/')}.java", """
            package $packageName;
            import org.apache.ibatis.annotations.Mapper;
            $annotation
            public interface $simpleName$extendsClause {
                String findOrder(long id);
            }
            """.trimIndent()
        )
    }

    protected fun assertMapperFixture(mapperName: String) {
        findClass(MAPPER, GlobalSearchScope.allScope(project))
        findClass(MAPPER_SCAN, GlobalSearchScope.allScope(project))
        assertTrue("$mapperName must be an interface", mapperClass(mapperName).isInterface)
    }

    protected fun mapperClass(qualifiedName: String): PsiClass =
        findClass(qualifiedName, GlobalSearchScope.projectScope(project))

    protected fun assertBeanNames(qualifiedName: String, expected: Set<String>) {
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
        val actual = beans.filter { it.psiClass.qualifiedName == qualifiedName }.map { it.name }.toSet()
        assertEquals("bean names of $qualifiedName", expected, actual)
    }

    protected fun findClass(qualifiedName: String, scope: GlobalSearchScope): PsiClass {
        val psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName, scope)
        assertNotNull("$qualifiedName must resolve", psiClass)
        return psiClass!!
    }
}
