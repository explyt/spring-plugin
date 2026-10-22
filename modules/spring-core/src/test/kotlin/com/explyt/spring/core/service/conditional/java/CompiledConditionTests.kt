/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.java

import com.explyt.spring.core.service.conditional.CompiledBeanConditionTestCase
import com.explyt.spring.core.service.conditional.CompiledConditionTestCase
import com.explyt.spring.core.service.conditional.ConditionFixtures
import com.explyt.spring.core.service.conditional.ConditionVerdict
import com.explyt.spring.core.service.conditional.WebApplicationTypeVerdictTestCase.Companion.DISPATCHER_HANDLER
import com.explyt.spring.core.service.conditional.WebApplicationTypeVerdictTestCase.Companion.DISPATCHER_SERVLET
import com.explyt.spring.core.service.conditional.WebApplicationTypeVerdictTestCase.Companion.SERVLET_API
import com.explyt.spring.test.TestLibrary

class CompiledBeanConditionTest : CompiledBeanConditionTestCase() {
    override val fixtures: ConditionFixtures by lazy { JavaConditionFixtures(myFixture) }

    override fun addProjectFactoryOf(libraryClass: String) {
        myFixture.addFileToProject(
            "com/app/FooConfiguration.java",
            """
            package com.app;

            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            public class FooConfiguration {
                @Bean
                public $libraryClass foo() { return new $libraryClass(); }
            }
            """.trimIndent()
        )
    }
}

class CompiledServletWebApplicationConditionTest : CompiledConditionTestCase() {
    override val fixtures: ConditionFixtures by lazy { JavaConditionFixtures(myFixture) }
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1, TestLibrary.springWebMvc_6_0_7, TestLibrary.jakartaServlet_6_0_0
    )

    fun testCompiledServletTypeInServletApplicationIsActive() {
        assertOnClasspath(DISPATCHER_SERVLET, SERVLET_API)
        val method = compiledBeanMethod(SERVLET_CONDITION, CONDITIONAL_ON_WEB_APPLICATION)

        assertEquals(ConditionVerdict.Active, verdictOf(method))
    }

    fun testCompiledReactiveTypeInServletApplicationIsInactive() {
        assertOnClasspath(DISPATCHER_SERVLET, SERVLET_API)
        assertNotOnClasspath(DISPATCHER_HANDLER)
        val method = compiledBeanMethod(REACTIVE_CONDITION, CONDITIONAL_ON_WEB_APPLICATION)

        assertInactiveWith(verdictOf(method), CONDITIONAL_ON_WEB_APPLICATION, "reactive web application")
    }
}

class CompiledNonWebApplicationConditionTest : CompiledConditionTestCase() {
    override val fixtures: ConditionFixtures by lazy { JavaConditionFixtures(myFixture) }

    fun testCompiledServletTypeInNonWebApplicationIsInactive() {
        assertNotOnClasspath(DISPATCHER_SERVLET, SERVLET_API, DISPATCHER_HANDLER)
        val method = compiledBeanMethod(SERVLET_CONDITION, CONDITIONAL_ON_WEB_APPLICATION)

        assertInactiveWith(verdictOf(method), CONDITIONAL_ON_WEB_APPLICATION, "servlet web application")
    }
}

class CompiledPropertyConditionAttributesTest : CompiledConditionTestCase() {
    override val fixtures: ConditionFixtures by lazy { JavaConditionFixtures(myFixture) }

    fun testCompiledComposedAnnotationWithUseSiteAliasIsActiveOnMatchingValue() {
        addProperties("vendor.composed.enabled=on")
        val method = compiledComposedBeanMethod()
        assertPropertyDefined("vendor.composed.enabled")

        assertEquals(ConditionVerdict.Active, verdictOf(method))
    }

    fun testCompiledComposedAnnotationReadsHavingValueOfItsMetaAnnotation() {
        addProperties("vendor.composed.enabled=true")
        val method = compiledComposedBeanMethod()
        assertPropertyDefined("vendor.composed.enabled")

        assertInactiveWith(verdictOf(method), COMPOSED_ANNOTATION, "vendor.composed.enabled=true, expected on")
    }

    fun testCompiledConstantPropertyNameIsActiveWhenSet() {
        addProperties("vendor.constant.enabled=true")
        val method = compiledBeanMethod(
            "@ConditionalOnProperty(name = LibConstants.KEY)", CONDITIONAL_ON_PROPERTY,
            mapOf(
                "LibConstants" to """
                    package $LIBRARY_PACKAGE;
                    public final class LibConstants {
                        public static final String KEY = "vendor.constant.enabled";
                    }
                """.trimIndent()
            )
        )
        assertPropertyDefined("vendor.constant.enabled")

        assertEquals(ConditionVerdict.Active, verdictOf(method))
    }

    fun testCompiledMatchIfMissingWithAbsentPropertyIsActive() {
        addProperties("vendor.other=present")
        val method = compiledBeanMethod(
            "@ConditionalOnProperty(name = \"vendor.optional.enabled\", matchIfMissing = true)", CONDITIONAL_ON_PROPERTY
        )
        assertPropertyDefined("vendor.other")
        assertPropertyUndefined("vendor.optional.enabled")

        assertEquals(ConditionVerdict.Active, verdictOf(method))
    }

    fun testCompiledHavingValueMatchesIgnoringCase() {
        addProperties("vendor.mode=ON")
        val method = compiledBeanMethod(
            "@ConditionalOnProperty(name = \"vendor.mode\", havingValue = \"on\")", CONDITIONAL_ON_PROPERTY
        )
        assertPropertyDefined("vendor.mode")

        assertEquals(ConditionVerdict.Active, verdictOf(method))
    }

    private fun compiledComposedBeanMethod() = compiledBeanMethod(
        "@ConditionalOnVendorFeature(\"vendor.composed.enabled\")", COMPOSED_ANNOTATION,
        mapOf(
            "ConditionalOnVendorFeature" to """
                package $LIBRARY_PACKAGE;

                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;
                import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
                import org.springframework.core.annotation.AliasFor;

                @Retention(RetentionPolicy.RUNTIME)
                @Target({ElementType.TYPE, ElementType.METHOD})
                @ConditionalOnProperty(havingValue = "on")
                public @interface ConditionalOnVendorFeature {
                    @AliasFor(annotation = ConditionalOnProperty.class, attribute = "name")
                    String[] value() default {};
                }
            """.trimIndent()
        )
    ).also { assertAnnotatedBy(compiledClass(COMPOSED_ANNOTATION), CONDITIONAL_ON_PROPERTY) }

    private companion object {
        const val COMPOSED_ANNOTATION = "$LIBRARY_PACKAGE.ConditionalOnVendorFeature"
    }
}

private const val SERVLET_CONDITION = "@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)"
private const val REACTIVE_CONDITION = "@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)"
