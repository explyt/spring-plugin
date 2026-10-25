/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.test.util.TestCompiledLibrary
import com.explyt.spring.test.util.runBlockingReleasingWriteIntent
import com.explyt.spring.web.util.WebApplicationStack
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.explyt.spring.core.service.conditional.ConditionPropertyValue
import com.explyt.spring.core.service.conditional.PropertyConditionValues
import com.intellij.openapi.application.readAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PsiTestUtil

/**
 * Edges of the declared `PagedModel` return and of a composed `@EnableSpringDataWebSupport`.
 *
 * Only Spring Data's `org.springframework.data.web.PagedModel` is written as `content` + `page`; Spring HATEOAS's
 * `PagedModel` and a project class of the same simple name are other types. A blank serialization mode binds as no
 * value at all: Boot's `LenientObjectToEnumConverterFactory` trims the text and converts an empty one to `null`, and
 * `JavaBeanBinder` then leaves `SpringDataWebProperties.Pageable.serializationMode` at its `DIRECT` initializer.
 */
class SpringBootApplicationMcpToolsetPageSchemaEdgeTest : PageSchemaTestCase() {

    override val stack = WebApplicationStack.SERVLET

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springDataJpa_3_4_0,
        TestLibrary.jacksonAnnotations_2_15_2,
        TestLibrary.jacksonDatabind_2_15_2,
        TestLibrary.kotlin_1_9_22,
        TestLibrary(HATEOAS_COORDINATES, false),
    )

    fun testHateoasPagedModelIsNotGivenTheSpringDataShape() = runBlockingReleasingWriteIntent<Unit> {
        addController(
            "HateoasController", """
            @GetMapping("/orders/hateoas")
            public org.springframework.hateoas.PagedModel<OrderDto> hateoas() { return null; }
            """
        )

        assertNotTheSpringDataShape(schemaOf("/orders/hateoas", "$HATEOAS_PAGED_MODEL<$ORDER_DTO>"))
    }

    fun testKotlinHateoasPagedModelIsNotGivenTheSpringDataShape() = runBlockingReleasingWriteIntent<Unit> {
        addFile(
            "com/example/shop/KotlinHateoasController.kt", """
            package com.example.shop

            import org.springframework.hateoas.PagedModel
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class KotlinHateoasController {
                @GetMapping("/orders/kotlin-hateoas")
                fun hateoas(): PagedModel<OrderDto> = TODO()
            }
            """.trimIndent()
        )

        assertNotTheSpringDataShape(schemaOf("/orders/kotlin-hateoas", "$HATEOAS_PAGED_MODEL<$ORDER_DTO>"))
    }

    fun testProjectClassNamedPagedModelIsDescribedByItsOwnFields() = runBlockingReleasingWriteIntent<Unit> {
        addFile(
            "com/example/web/PagedModel.java", """
            package com.example.web;

            import java.util.List;

            public class PagedModel<T> {
                public List<T> getItems() { return null; }
                public String getCursor() { return null; }
            }
            """.trimIndent()
        )
        assertResolves(PROJECT_PAGED_MODEL)
        addController(
            "ProjectPagedModelController", """
            @GetMapping("/orders/project-model")
            public com.example.web.PagedModel<OrderDto> projectModel() { return null; }
            """
        )

        val schema = schemaOf("/orders/project-model", "$PROJECT_PAGED_MODEL<$ORDER_DTO>")

        assertEquals(schema.toString(), PROJECT_PAGED_MODEL, schema["className"].asText())
        assertEquals(schema.toString(), setOf("items", "cursor"), names(schema).toSet())
        assertNull(schema.toString(), schema["wireShape"])
        assertNull(schema.toString(), schema["wireShapeSource"])
    }

    fun testRawPagedModelKeepsTheShapeWithAnUntypedContent() = runBlockingReleasingWriteIntent<Unit> {
        addController(
            "RawPagedModelController", """
            @GetMapping("/orders/raw-model")
            @SuppressWarnings("rawtypes")
            public PagedModel rawModel() { return null; }
            """
        )

        val schema = schemaOf("/orders/raw-model", PAGED_MODEL)

        assertEquals(schema.toString(), PAGED_MODEL, schema["className"].asText())
        assertEquals("PAGED_MODEL", schema["wireShape"]?.asText())
        assertSource("DECLARED_TYPE", schema)
        assertEquals(listOf("content", "page"), names(schema))
        val content = field(schema, "content")
        assertEquals("java.util.List", content["type"].asText())
        assertTrue("a raw page has no element schema: $content", content["nested"].isNull)
        assertNull("a declared PagedModel is not an assumption: $schema", schema["wireShapeAssumed"])
    }

    fun testPagedModelsInsideAListKeepTheirDeclaredShape() = runBlockingReleasingWriteIntent<Unit> {
        addController(
            "PagedModelListController", """
            @GetMapping("/orders/models")
            public java.util.List<PagedModel<OrderDto>> models() { return null; }
            """
        )

        val schema = schemaOf("/orders/models", "java.util.List<$PAGED_MODEL<$ORDER_DTO>>")

        assertPagedModel(schema)
        assertSource("DECLARED_TYPE", schema)
    }

    fun testKotlinPagedModelsInsideAListKeepTheirDeclaredShape() = runBlockingReleasingWriteIntent<Unit> {
        addFile(
            "com/example/shop/KotlinPagedModelListController.kt", """
            package com.example.shop

            import org.springframework.data.web.PagedModel
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class KotlinPagedModelListController {
                @GetMapping("/orders/kotlin-models")
                fun models(): List<PagedModel<OrderDto>> = emptyList()
            }
            """.trimIndent()
        )

        val schema = schemaOf("/orders/kotlin-models", "java.util.List<$PAGED_MODEL<$ORDER_DTO>>")

        assertPagedModel(schema)
        assertSource("DECLARED_TYPE", schema)
    }

    fun testWhitespaceSerializationModeUsesBootDirectDefault() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=   ")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("DEFAULT", schema)
    }

    fun testPlaceholderWithAnEmptyDefaultUsesBootDirectDefault() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=\${PAGE_MODE:}")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("DEFAULT", schema)
    }

    fun testAliasedModeOnTheUsingClassOutranksTheMetaDefaultViaDto() = runBlockingReleasingWriteIntent<Unit> {
        addAliasedWebSupport(metaMode = "DIRECT")
        addAliasedWebConfig("ComposedWebConfig", "VIA_DTO")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource("@EnableSpringDataWebSupport on com.example.config.ComposedWebConfig", schema)
    }

    fun testAliasedModeOnTheUsingClassOutranksTheMetaDefaultDirect() = runBlockingReleasingWriteIntent<Unit> {
        addAliasedWebSupport(metaMode = "VIA_DTO")
        addAliasedWebConfig("ComposedWebConfig", "DIRECT")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("@EnableSpringDataWebSupport on com.example.config.ComposedWebConfig", schema)
    }

    fun testAliasedModeSetFromKotlinOutranksTheMetaDefault() = runBlockingReleasingWriteIntent<Unit> {
        addAliasedWebSupport(metaMode = "DIRECT")
        addFile(
            "com/example/config/KotlinComposedWebConfig.kt", """
            package com.example.config

            import org.springframework.context.annotation.Configuration
            import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode

            @Configuration
            @EnableAliasedWeb(mode = PageSerializationMode.VIA_DTO)
            class KotlinComposedWebConfig
            """.trimIndent()
        )
        assertResolves("com.example.config.KotlinComposedWebConfig")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource("@EnableSpringDataWebSupport on com.example.config.KotlinComposedWebConfig", schema)
    }

    fun testComposedViaDtoUseWinsOverAComposedDirectUse() = runBlockingReleasingWriteIntent<Unit> {
        addAliasedWebSupport(metaMode = "DIRECT")
        addAliasedWebConfig("AlphaWebConfig", "DIRECT")
        addAliasedWebConfig("ZuluWebConfig", "VIA_DTO")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource("@EnableSpringDataWebSupport on com.example.config.ZuluWebConfig", schema)
    }

    fun testComposedAnnotationUsedByNoClassHasNoEffect() = runBlockingReleasingWriteIntent<Unit> {
        addAliasedWebSupport(metaMode = "VIA_DTO")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("DEFAULT", schema)
    }

    fun testComposedAnnotationUsedOnlyInTestSourcesHasNoEffect() = runBlockingReleasingWriteIntent<Unit> {
        addAliasedWebSupport(metaMode = "VIA_DTO")
        val testRoot = myFixture.tempDirFixture.findOrCreateDir("src/test/java")
        PsiTestUtil.addSourceRoot(module, testRoot, true)
        try {
            addFile(
                "src/test/java/com/example/config/TestComposedWebConfig.java", """
                package com.example.config;

                import org.springframework.context.annotation.Configuration;

                @Configuration
                @EnableAliasedWeb
                public class TestComposedWebConfig {
                }
                """.trimIndent()
            )
            assertResolves("com.example.config.TestComposedWebConfig")

            val schema = pageSchema("/orders/page")

            assertDirectPage(schema)
            assertSource("DEFAULT", schema)
        } finally {
            PsiTestUtil.removeSourceRoot(module, testRoot)
        }
    }

    fun testComposedAliasDefaultViaDtoUsedPlainlyIsUnknown() = runBlockingReleasingWriteIntent<Unit> {
        addComposedWebSupport(attribute = "mode", aliased = true, default = "VIA_DTO")
        addPlainComposedUse("ComposedWebConfig")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("UNKNOWN", schema)
    }

    fun testKotlinPlainUseOfComposedAliasDefaultViaDtoIsUnknown() = runBlockingReleasingWriteIntent<Unit> {
        addComposedWebSupport(attribute = "mode", aliased = true, default = "VIA_DTO")
        addFile(
            "com/example/config/KotlinComposedWebConfig.kt", """
            package com.example.config

            import org.springframework.context.annotation.Configuration

            @Configuration
            @EnableComposedWeb
            class KotlinComposedWebConfig
            """.trimIndent()
        )
        assertUsedWithoutAttributes("com.example.config.KotlinComposedWebConfig")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("UNKNOWN", schema)
    }

    fun testComposedConventionDefaultViaDtoUsedPlainlyIsUnknown() = runBlockingReleasingWriteIntent<Unit> {
        addComposedWebSupport(attribute = "pageSerializationMode", aliased = false, default = "VIA_DTO")
        addPlainComposedUse("ComposedWebConfig")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("UNKNOWN", schema)
    }

    fun testComposedAliasDefaultDirectUsedPlainlyIsAlsoUnknown() = runBlockingReleasingWriteIntent<Unit> {
        addComposedWebSupport(attribute = "mode", aliased = true, default = "DIRECT")
        addPlainComposedUse("ComposedWebConfig")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("UNKNOWN", schema)
    }

    fun testComposedAliasSetToDirectAtTheUseSiteIsDirect() = runBlockingReleasingWriteIntent<Unit> {
        addComposedWebSupport(attribute = "mode", aliased = true, default = "VIA_DTO")
        addFile(
            "com/example/config/ComposedWebConfig.java", """
            package com.example.config;

            import org.springframework.context.annotation.Configuration;
            import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

            @Configuration
            @EnableComposedWeb(mode = PageSerializationMode.DIRECT)
            public class ComposedWebConfig {
            }
            """.trimIndent()
        )
        assertResolves("com.example.config.ComposedWebConfig")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("@EnableSpringDataWebSupport on com.example.config.ComposedWebConfig", schema)
    }

    fun testComposedMetaFixedViaDtoWithoutOwnAttributeIsPagedModel() = runBlockingReleasingWriteIntent<Unit> {
        addFile(
            "com/example/config/EnableComposedWeb.java", """
            package com.example.config;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            import org.springframework.data.web.config.EnableSpringDataWebSupport;
            import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.RUNTIME)
            @EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
            public @interface EnableComposedWeb {
            }
            """.trimIndent()
        )
        assertEquals("no own attribute", 0, composedWebSupport().methods.size)
        addPlainComposedUse("ComposedWebConfig")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource("@EnableSpringDataWebSupport on com.example.config.ComposedWebConfig", schema)
    }

    fun testConventionModeSetToViaDtoAtTheUseSiteIsUnknown() = runBlockingReleasingWriteIntent<Unit> {
        addWebSupportAnnotation("EnableComposedWeb", "@EnableSpringDataWebSupport", CONVENTION_MODE_DIRECT)
        assertDeclaresUnaliased(COMPOSED_WEB_SUPPORT, "pageSerializationMode")
        addComposedUse("ComposedWebConfig", "@EnableComposedWeb(pageSerializationMode = PageSerializationMode.VIA_DTO)")

        val schema = pageSchema("/orders/page")

        assertSource("UNKNOWN", schema)
        assertDirectPage(schema)
    }

    fun testKotlinConventionModeSetToViaDtoAtTheUseSiteIsUnknown() = runBlockingReleasingWriteIntent<Unit> {
        addWebSupportAnnotation("EnableComposedWeb", "@EnableSpringDataWebSupport", CONVENTION_MODE_DIRECT)
        assertDeclaresUnaliased(COMPOSED_WEB_SUPPORT, "pageSerializationMode")
        addKotlinComposedUse("@EnableComposedWeb(pageSerializationMode = PageSerializationMode.VIA_DTO)")

        val schema = pageSchema("/orders/page")

        assertSource("UNKNOWN", schema)
        assertDirectPage(schema)
    }

    fun testConventionModeSetToDirectOverAViaDtoMetaIsUnknown() = runBlockingReleasingWriteIntent<Unit> {
        addWebSupportAnnotation("EnableComposedWeb", META_VIA_DTO, CONVENTION_MODE_DIRECT)
        assertDeclaresUnaliased(COMPOSED_WEB_SUPPORT, "pageSerializationMode")
        addComposedUse("ComposedWebConfig", "@EnableComposedWeb(pageSerializationMode = PageSerializationMode.DIRECT)")

        val schema = pageSchema("/orders/page")

        assertSource("UNKNOWN", schema)
        assertDirectPage(schema)
    }

    fun testNestedComposedAliasDefaultViaDtoUsedPlainlyIsUnknown() = runBlockingReleasingWriteIntent<Unit> {
        addWebSupportAnnotation("EnableComposedWeb", "@EnableSpringDataWebSupport", ALIASED_MODE_VIA_DTO)
        addOuterWebSupport()
        addComposedUse("ComposedWebConfig", "@EnableOuterWeb")

        val schema = pageSchema("/orders/page")

        assertSource("UNKNOWN", schema)
        assertDirectPage(schema)
    }

    fun testKotlinNestedComposedAliasDefaultViaDtoUsedPlainlyIsUnknown() = runBlockingReleasingWriteIntent<Unit> {
        addWebSupportAnnotation("EnableComposedWeb", "@EnableSpringDataWebSupport", ALIASED_MODE_VIA_DTO)
        addOuterWebSupport()
        addKotlinComposedUse("@EnableOuterWeb")

        val schema = pageSchema("/orders/page")

        assertSource("UNKNOWN", schema)
        assertDirectPage(schema)
    }

    fun testNestedComposedConventionDefaultViaDtoUsedPlainlyIsUnknown() = runBlockingReleasingWriteIntent<Unit> {
        addWebSupportAnnotation(
            "EnableComposedWeb", "@EnableSpringDataWebSupport",
            "PageSerializationMode pageSerializationMode() default PageSerializationMode.VIA_DTO;"
        )
        assertDeclaresUnaliased(COMPOSED_WEB_SUPPORT, "pageSerializationMode")
        addOuterWebSupport()
        addComposedUse("ComposedWebConfig", "@EnableOuterWeb")

        val schema = pageSchema("/orders/page")

        assertSource("UNKNOWN", schema)
        assertDirectPage(schema)
    }

    fun testNestedComposedAliasSetToViaDtoOnTheOuterAnnotationIsPagedModel() = runBlockingReleasingWriteIntent<Unit> {
        addWebSupportAnnotation(
            "EnableComposedWeb", "@EnableSpringDataWebSupport",
            "$ALIAS_TO_MODE PageSerializationMode mode() default PageSerializationMode.DIRECT;"
        )
        addOuterWebSupport(meta = "@EnableComposedWeb(mode = PageSerializationMode.VIA_DTO)")
        addComposedUse("ComposedWebConfig", "@EnableOuterWeb")

        val schema = pageSchema("/orders/page")

        assertSource("@EnableSpringDataWebSupport on com.example.config.ComposedWebConfig", schema)
        assertPagedModel(schema)
    }

    fun testNestedComposedWithoutAttributesOverAViaDtoMetaIsPagedModel() = runBlockingReleasingWriteIntent<Unit> {
        addWebSupportAnnotation("EnableComposedWeb", META_VIA_DTO, "")
        assertEquals("no own attribute", 0, composedWebSupport().methods.size)
        addOuterWebSupport()
        addComposedUse("ComposedWebConfig", "@EnableOuterWeb")

        val schema = pageSchema("/orders/page")

        assertSource("@EnableSpringDataWebSupport on com.example.config.ComposedWebConfig", schema)
        assertPagedModel(schema)
    }

    fun testPositionalUnrelatedValueOfAComposedAnnotationKeepsTheDirectDefault() = runBlockingReleasingWriteIntent<Unit> {
        addWebSupportAnnotation("EnableMyWeb", "@EnableSpringDataWebSupport", "String value() default \"\";")
        assertDeclaresUnaliased("com.example.config.EnableMyWeb", "value")
        addComposedUse("ComposedWebConfig", "@EnableMyWeb(\"x\")", "com.example.config.EnableMyWeb")

        val schema = pageSchema("/orders/page")

        assertSource("@EnableSpringDataWebSupport on com.example.config.ComposedWebConfig", schema)
        assertDirectPage(schema)
    }

    private fun addWebSupportAnnotation(
        simpleName: String,
        meta: String,
        members: String,
        metaFqn: String = ENABLE_WEB_SUPPORT,
    ) {
        addFile(
            "com/example/config/$simpleName.java", """
            package com.example.config;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            import org.springframework.core.annotation.AliasFor;
            import org.springframework.data.web.config.EnableSpringDataWebSupport;
            import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.RUNTIME)
            $meta
            public @interface $simpleName {
                $members
            }
            """.trimIndent()
        )
        assertResolves("org.springframework.core.annotation.AliasFor")
        assertMetaAnnotated("com.example.config.$simpleName", metaFqn)
    }

    private fun addOuterWebSupport(meta: String = "@EnableComposedWeb") {
        addWebSupportAnnotation("EnableOuterWeb", meta, "", COMPOSED_WEB_SUPPORT)
        assertEquals("the outer annotation declares no attribute", 0, annotationType(OUTER_WEB_SUPPORT).methods.size)
    }

    private fun addComposedUse(className: String, use: String, usedFqn: String = useFqnOf(use)) {
        addFile(
            "com/example/config/$className.java", """
            package com.example.config;

            import org.springframework.context.annotation.Configuration;
            import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

            @Configuration
            $use
            public class $className {
            }
            """.trimIndent()
        )
        assertAnnotatedWith("com.example.config.$className", usedFqn)
    }

    private fun addKotlinComposedUse(use: String) {
        addFile(
            "com/example/config/KotlinComposedWebConfig.kt", """
            package com.example.config

            import org.springframework.context.annotation.Configuration
            import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode

            @Configuration
            $use
            class KotlinComposedWebConfig
            """.trimIndent()
        )
        assertAnnotatedWith("com.example.config.KotlinComposedWebConfig", useFqnOf(use))
    }

    private fun useFqnOf(use: String): String = "com.example.config." + use.removePrefix("@").substringBefore('(')

    private fun assertAnnotatedWith(fqn: String, annotationFqn: String) {
        assertNotNull("$fqn is annotated with $annotationFqn", annotationType(fqn).getAnnotation(annotationFqn))
    }

    private fun assertMetaAnnotated(annotationFqn: String, metaFqn: String) {
        val annotation = annotationType(annotationFqn)
        assertTrue("$annotationFqn is an annotation type", annotation.isAnnotationType)
        assertNotNull("$annotationFqn is meta-annotated with $metaFqn", annotation.getAnnotation(metaFqn))
    }

    private fun assertDeclaresUnaliased(annotationFqn: String, attribute: String) {
        val method = annotationType(annotationFqn).findMethodsByName(attribute, false).singleOrNull()
        assertNotNull("$annotationFqn declares '$attribute'", method)
        assertFalse(
            "'$attribute' carries no @AliasFor",
            method!!.hasAnnotation("org.springframework.core.annotation.AliasFor")
        )
    }

    private fun annotationType(fqn: String) =
        JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))
            ?: throw AssertionError("the fixture must resolve $fqn")

    private fun addComposedWebSupport(attribute: String, aliased: Boolean, default: String) {
        val alias = if (aliased) {
            "@AliasFor(annotation = EnableSpringDataWebSupport.class, attribute = \"pageSerializationMode\")"
        } else {
            ""
        }
        addFile(
            "com/example/config/EnableComposedWeb.java", """
            package com.example.config;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            import org.springframework.core.annotation.AliasFor;
            import org.springframework.data.web.config.EnableSpringDataWebSupport;
            import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.RUNTIME)
            @EnableSpringDataWebSupport
            public @interface EnableComposedWeb {
                $alias
                PageSerializationMode $attribute() default PageSerializationMode.$default;
            }
            """.trimIndent()
        )
        assertResolves("org.springframework.core.annotation.AliasFor")
        val method = composedWebSupport().findMethodsByName(attribute, false).singleOrNull()
        assertNotNull("the composed annotation declares '$attribute'", method)
        assertEquals(
            "'$attribute' carries @AliasFor: $aliased",
            aliased, method!!.hasAnnotation("org.springframework.core.annotation.AliasFor")
        )
        val meta = composedWebSupport().getAnnotation(ENABLE_WEB_SUPPORT)
        assertNotNull("the composed annotation is meta-annotated with @EnableSpringDataWebSupport", meta)
        assertEmpty("the meta-annotation sets no mode itself", meta!!.parameterList.attributes.toList())
    }

    private fun addPlainComposedUse(className: String) {
        addFile(
            "com/example/config/$className.java", """
            package com.example.config;

            import org.springframework.context.annotation.Configuration;

            @Configuration
            @EnableComposedWeb
            public class $className {
            }
            """.trimIndent()
        )
        assertUsedWithoutAttributes("com.example.config.$className")
    }

    private fun assertUsedWithoutAttributes(fqn: String) {
        val using = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))
        assertNotNull("the fixture must resolve $fqn", using)
        val use = using!!.getAnnotation(COMPOSED_WEB_SUPPORT)
        assertNotNull("$fqn uses @EnableComposedWeb", use)
        assertEmpty("$fqn sets no attribute at the use site", use!!.parameterList.attributes.toList())
    }

    private fun composedWebSupport() =
        JavaPsiFacade.getInstance(project).findClass(COMPOSED_WEB_SUPPORT, GlobalSearchScope.allScope(project))
            ?: throw AssertionError("the fixture must resolve $COMPOSED_WEB_SUPPORT")

    private fun assertNotTheSpringDataShape(schema: JsonNode) {
        assertResolves(HATEOAS_PAGED_MODEL)
        assertResolves(PAGED_MODEL)
        assertFalse(schema.toString(), schema["className"]?.asText() == PAGED_MODEL)
        assertNull("HATEOAS writes _embedded/_links, not the Spring Data shape: $schema", schema["wireShape"])
        assertNull(schema.toString(), schema["wireShapeSource"])
        assertFalse("no Spring Data content: $schema", "content" in names(schema))
        assertTrue("HATEOAS writes its links: $schema", "_links" in names(schema))
    }

    private fun addController(className: String, methods: String) {
        addFile(
            "com/example/shop/$className.java", """
            package com.example.shop;

            import org.springframework.data.web.PagedModel;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class $className {
            ${methods.trimIndent()}
            }
            """.trimIndent()
        )
    }

    private fun addAliasedWebSupport(metaMode: String) {
        addFile("com/example/config/EnableAliasedWeb.java", aliasedWebSupport(metaMode))
        assertResolves(ALIASED_WEB_SUPPORT)
        assertResolves("org.springframework.core.annotation.AliasFor")
    }

    private fun addAliasedWebConfig(className: String, mode: String) {
        addFile("com/example/config/$className.java", aliasedWebConfig(className, mode))
        assertResolves("com.example.config.$className")
    }

    private companion object {
        const val HATEOAS_COORDINATES = "org.springframework.hateoas:spring-hateoas:2.2.0"
        const val HATEOAS_PAGED_MODEL = "org.springframework.hateoas.PagedModel"
        const val PROJECT_PAGED_MODEL = "com.example.web.PagedModel"
        const val COMPOSED_WEB_SUPPORT = "com.example.config.EnableComposedWeb"
        const val OUTER_WEB_SUPPORT = "com.example.config.EnableOuterWeb"
        const val META_VIA_DTO = "@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)"
        const val ALIAS_TO_MODE =
            "@AliasFor(annotation = EnableSpringDataWebSupport.class, attribute = \"pageSerializationMode\")"
        const val ALIASED_MODE_VIA_DTO = "$ALIAS_TO_MODE PageSerializationMode mode() default PageSerializationMode.VIA_DTO;"
        const val CONVENTION_MODE_DIRECT = "PageSerializationMode pageSerializationMode() default PageSerializationMode.DIRECT;"
    }
}

/**
 * A composed `@EnableSpringDataWebSupport` declared in a dependency module and used by the application module. The
 * annotation type is on the application's compile classpath, so the using class decides the mode as a direct
 * declaration would.
 */
class SpringBootApplicationMcpToolsetPageSchemaModulesTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springDataJpa_3_4_0,
        TestLibrary.jacksonAnnotations_2_15_2,
        TestLibrary.jacksonDatabind_2_15_2,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    fun testComposedAnnotationFromADependencyModuleControlsTheApplicationMode() = runBlockingReleasingWriteIntent<Unit> {
        val shared = addDependencyModule("shared")
        addFileToModule(shared, "com/example/config/EnableAliasedWeb.java", aliasedWebSupport("DIRECT"))
        addFileToModule(module, "com/example/shop/ShopApplication.java", SHOP_APPLICATION)
        addFileToModule(module, "com/example/shop/OrderDto.java", ORDER_DTO_SOURCE)
        addFileToModule(module, "com/example/shop/OrderController.java", PAGE_CONTROLLER)
        addFileToModule(module, "com/example/config/ComposedWebConfig.java", aliasedWebConfig("ComposedWebConfig", "VIA_DTO"))

        assertEquals(WebApplicationStack.SERVLET, readAction { WebApplicationStack.of(module) })
        val annotation = readAction {
            JavaPsiFacade.getInstance(project).findClass(ALIASED_WEB_SUPPORT, GlobalSearchScope.moduleScope(shared))
        }
        assertNotNull("the composed annotation must live in the dependency module", annotation)

        val endpoints = mapper.readTree(
            toolset.getEndpointContract(urlPattern = "/orders/page", projectPath = project.basePath, httpMethod = "GET")
        )["endpoints"]
        assertEquals(endpoints.toString(), 1, endpoints.size())
        val schema = endpoints.single()["responseSchema"]

        assertEquals(schema.toString(), "org.springframework.data.web.PagedModel", schema["className"]?.asText())
        assertEquals(schema.toString(), "PAGED_MODEL", schema["wireShape"]?.asText())
        assertEquals(
            schema.toString(),
            "@EnableSpringDataWebSupport on com.example.config.ComposedWebConfig",
            schema["wireShapeSource"]?.asText()
        )
    }

    fun testViaDtoConfigurationInADependentApplicationModuleControlsTheHandlerMode() =
        runBlockingReleasingWriteIntent<Unit> {
            val app = addApplicationModuleDependingOnTheHandlers()
            addFileToModule(app, "com/example/config/WebConfig.java", VIA_DTO_WEB_CONFIG)
            val declaration = readAction {
                JavaPsiFacade.getInstance(project).findClass(WEB_CONFIG, GlobalSearchScope.moduleScope(app))
            }
            assertNotNull("$WEB_CONFIG must live in the application module 'app'", declaration)
            assertTrue(
                "$WEB_CONFIG must carry @EnableSpringDataWebSupport",
                declaration!!.hasAnnotation(ENABLE_WEB_SUPPORT)
            )

            assertPagedModelFrom("@EnableSpringDataWebSupport on $WEB_CONFIG", handlerPageSchema())
        }

    fun testViaDtoPropertyOfADependentApplicationModuleControlsTheHandlerMode() =
        runBlockingReleasingWriteIntent<Unit> {
            val app = addApplicationModuleDependingOnTheHandlers()
            addFileToModule(app, "application.properties", "$SERIALIZATION_MODE_KEY=via_dto")
            assertEquals(
                "precondition: the application module 'app' configures the mode",
                ConditionPropertyValue.Known("via_dto"),
                readAction { PropertyConditionValues(app).valueOf(SERIALIZATION_MODE_KEY) }
            )

            assertPagedModelFrom(SERIALIZATION_MODE_KEY, handlerPageSchema())
        }

    fun testLibraryViaDtoConfigurationImportedByAnotherApplicationLeavesThisApplicationDirect() {
        val vendor = TestCompiledLibrary.attach(
            module, testRootDisposable, "vendor-web-support", mapOf(VENDOR_WEB_CONFIG_PATH to VENDOR_WEB_CONFIG_SOURCE)
        )
        val beta = addStandaloneModule("beta")
        vendor.attachTo(beta, testRootDisposable)
        IndexingTestUtil.waitUntilIndexesAreReady(project)
        runBlockingReleasingWriteIntent<Unit> {
            addApplication(module, "alpha", "/alpha/orders/page")
            addFileToModule(module, "com/example/alpha/VendorImports.java", ALPHA_VENDOR_IMPORTS)
            addApplication(beta, "beta", "/beta/orders/page")

            assertFalse("precondition: 'beta' does not depend on the main module", dependsOn(beta, module))
            assertFalse("precondition: the main module does not depend on 'beta'", dependsOn(module, beta))
            readAction {
                assertCompiledLibraryClassSeenBy(VENDOR_WEB_CONFIG, module, beta)
                assertEquals(
                    "precondition: only the main module's application imports $VENDOR_WEB_CONFIG",
                    setOf("com.example.alpha.VendorImports"), importersOf(VENDOR_WEB_CONFIG)
                )
                val importer = JavaPsiFacade.getInstance(project)
                    .findClass("com.example.alpha.VendorImports", GlobalSearchScope.allScope(project))!!
                assertEquals(module, ModuleUtilCore.findModuleForPsiElement(importer))
            }

            assertPagedModelFrom("@EnableSpringDataWebSupport on $VENDOR_WEB_CONFIG", pageSchemaOf("/alpha/orders/page"))
            assertDirectByDefault(pageSchemaOf("/beta/orders/page"))
        }
    }

    fun testLibraryViaDtoConfigurationImportedOnlyByATestConfigurationLeavesTheApplicationDirect() {
        TestCompiledLibrary.attach(
            module, testRootDisposable, "vendor-web-support", mapOf(VENDOR_WEB_CONFIG_PATH to VENDOR_WEB_CONFIG_SOURCE)
        )
        addLibraries(module, listOf(SPRING_BOOT_TEST), DependencyScope.TEST)
        runBlockingReleasingWriteIntent<Unit> {
            addFileToModule(module, "com/example/shop/ShopApplication.java", SHOP_APPLICATION)
            addFileToModule(module, "com/example/shop/OrderDto.java", ORDER_DTO_SOURCE)
            addFileToModule(module, "com/example/shop/OrderController.java", PAGE_CONTROLLER)
            val importerFile = addTestSourceFileToModule(
                module, "com/example/shop/VendorTestImports.java", vendorTestImports()
            )

            readAction {
                assertCompiledLibraryClassSeenBy(VENDOR_WEB_CONFIG, module)
                assertTrue(
                    "precondition: the importer lives in a test source root",
                    ProjectFileIndex.getInstance(project).isInTestSourceContent(importerFile.virtualFile)
                )
                val importer = JavaPsiFacade.getInstance(project)
                    .findClass(VENDOR_TEST_IMPORTS, GlobalSearchScope.allScope(project))
                assertNotNull("the fixture must resolve $VENDOR_TEST_IMPORTS", importer)
                assertTrue("$VENDOR_TEST_IMPORTS must carry @TestConfiguration", importer!!.hasAnnotation(TEST_CONFIGURATION))
                assertNotNull(
                    "precondition: $TEST_CONFIGURATION resolves from the test sources",
                    JavaPsiFacade.getInstance(project).findClass(TEST_CONFIGURATION, module.getModuleWithDependenciesAndLibrariesScope(true))
                )
                val vendorConfig = JavaPsiFacade.getInstance(project)
                    .findClass(VENDOR_WEB_CONFIG, GlobalSearchScope.allScope(project))!!
                assertTrue("$VENDOR_TEST_IMPORTS must carry @Import", importer.hasAnnotation(IMPORT))
                assertNotNull(
                    "precondition: $VENDOR_TEST_IMPORTS imports $VENDOR_WEB_CONFIG",
                    ReferencesSearch.search(vendorConfig, GlobalSearchScope.fileScope(importerFile)).findFirst()
                )
                assertEquals(
                    "precondition: the production bean model records no importer of $VENDOR_WEB_CONFIG",
                    emptySet<String>(), importersOf(VENDOR_WEB_CONFIG)
                )
                assertNull(
                    "precondition: no production class references $VENDOR_WEB_CONFIG",
                    ReferencesSearch.search(vendorConfig, module.getModuleScope(false)).findFirst()
                )
            }

            assertDirectByDefault(pageSchemaOf("/orders/page"))
        }
    }

    fun testSharedModuleImporterReachedOnlyByAnotherApplicationLeavesThisApplicationDirect() {
        val vendor = TestCompiledLibrary.attach(
            module, testRootDisposable, "vendor-web-support", mapOf(VENDOR_WEB_CONFIG_PATH to VENDOR_WEB_CONFIG_SOURCE)
        )
        val common = addDependencyModule("common")
        val b = addStandaloneModule("b")
        ModuleRootModificationUtil.addDependency(b, common)
        vendor.attachTo(common, testRootDisposable)
        vendor.attachTo(b, testRootDisposable)
        IndexingTestUtil.waitUntilIndexesAreReady(project)
        runBlockingReleasingWriteIntent<Unit> {
            addFileToModule(common, "com/company/common/CommonWeb.java", COMMON_WEB_SOURCE)
            addApplication(module, "a", "/a/orders/page", applicationPackage = "com.company")
            addApplication(b, "b", "/b/orders/page", applicationPackage = "com.company.b")

            assertTrue("precondition: the main module (app A) depends on 'common'", dependsOn(module, common))
            assertTrue("precondition: 'b' depends on 'common'", dependsOn(b, common))
            assertFalse("precondition: 'b' does not depend on the main module", dependsOn(b, module))
            assertFalse("precondition: the main module does not depend on 'b'", dependsOn(module, b))
            assertFalse("precondition: 'common' depends on no application", dependsOn(common, module) || dependsOn(common, b))
            readAction {
                assertCompiledLibraryClassSeenBy(VENDOR_WEB_CONFIG, module, common, b)
                assertEquals(
                    "precondition: only the shared configuration imports $VENDOR_WEB_CONFIG",
                    setOf(COMMON_WEB), importersOf(VENDOR_WEB_CONFIG)
                )
                val importer = JavaPsiFacade.getInstance(project).findClass(COMMON_WEB, GlobalSearchScope.allScope(project))!!
                assertEquals(common, ModuleUtilCore.findModuleForPsiElement(importer))
                val scan = PackageScanService.getInstance(project).getAllPackages()
                val scannedByA = scan.getPackages(module)
                val scannedByB = scan.getPackages(b)
                assertTrue("precondition: app A's scan covers $COMMON_WEB: $scannedByA", scannedByA.any(COMMON_WEB::startsWith))
                assertTrue("precondition: app B scans its own package: $scannedByB", scannedByB.isNotEmpty())
                assertFalse("precondition: app B's scan misses $COMMON_WEB: $scannedByB", scannedByB.any(COMMON_WEB::startsWith))
            }

            assertPagedModelFrom("@EnableSpringDataWebSupport on $VENDOR_WEB_CONFIG", pageSchemaOf("/a/orders/page"))
            assertDirectByDefault(pageSchemaOf("/b/orders/page"))
        }
    }

    private suspend fun addApplication(
        target: Module, name: String, url: String, applicationPackage: String = "com.example.$name",
    ) {
        val packageName = applicationPackage
        val path = packageName.replace('.', '/')
        addFileToModule(target, "$path/Application.java", """
            package $packageName;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class Application {
            }
        """.trimIndent())
        addFileToModule(target, "$path/OrderDto.java", """
            package $packageName;

            public class OrderDto {
                public long getId() { return 0L; }
            }
        """.trimIndent())
        addFileToModule(target, "$path/OrderController.java", """
            package $packageName;

            import org.springframework.data.domain.Page;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class OrderController {
                @GetMapping("$url")
                public Page<OrderDto> page() { return null; }
            }
        """.trimIndent())
        assertEquals(
            "the application stack of '${target.name}'",
            WebApplicationStack.SERVLET,
            readAction { WebApplicationStack.of(target) }
        )
        assertEquals(
            "precondition: '${target.name}' declares its own application",
            listOf(target.name to "$packageName.Application"),
            readAction { applicationsIn(target) }
        )
    }

    private fun dependsOn(dependent: Module, dependency: Module): Boolean =
        ModuleRootManager.getInstance(dependent).isDependsOn(dependency)

    private fun importersOf(fqn: String): Set<String> =
        PackageScanService.getInstance(project).getAllPackages().importersByImported[fqn].orEmpty()

    private fun assertCompiledLibraryClassSeenBy(fqn: String, vararg modules: Module) {
        val declaration = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))
        assertNotNull("the fixture must resolve the library class $fqn, or the test proves nothing", declaration)
        val file = declaration!!.containingFile.virtualFile
        assertTrue("$fqn must come from compiled library classes", ProjectFileIndex.getInstance(project).isInLibraryClasses(file))
        assertFalse("$fqn must be outside the project scope", ProjectScope.getProjectScope(project).contains(file))
        assertTrue("$fqn must carry @EnableSpringDataWebSupport", declaration.hasAnnotation(ENABLE_WEB_SUPPORT))
        modules.forEach {
            assertTrue(
                "precondition: '${it.name}' sees $fqn",
                it.getModuleWithDependenciesAndLibrariesScope(false).contains(file)
            )
        }
    }

    private fun assertDirectByDefault(schema: JsonNode) {
        assertEquals(schema.toString(), "org.springframework.data.domain.PageImpl", schema["className"]?.asText())
        assertEquals(schema.toString(), "PAGE_DIRECT", schema["wireShape"]?.asText())
        assertEquals(schema.toString(), "DEFAULT", schema["wireShapeSource"]?.asText())
    }

    private fun assertPagedModelFrom(source: String, schema: JsonNode) {
        assertEquals(schema.toString(), "org.springframework.data.web.PagedModel", schema["className"]?.asText())
        assertEquals(schema.toString(), "PAGED_MODEL", schema["wireShape"]?.asText())
        assertEquals(schema.toString(), source, schema["wireShapeSource"]?.asText())
    }

    private suspend fun addApplicationModuleDependingOnTheHandlers(): Module {
        addFileToModule(module, "com/example/shop/OrderDto.java", ORDER_DTO_SOURCE)
        addFileToModule(module, "com/example/shop/OrderController.java", PAGE_CONTROLLER)
        val app = addDependentModule("app")
        addFileToModule(app, "com/example/ShopApplication.java", ROOT_APPLICATION)

        assertTrue(
            "precondition: 'app' depends on the handler module",
            ModuleRootManager.getInstance(app).isDependsOn(module)
        )
        assertFalse(
            "precondition: the handler module does not depend on 'app'",
            ModuleRootManager.getInstance(module).isDependsOn(app)
        )
        assertEquals(
            "the application stack of the handler module",
            WebApplicationStack.SERVLET,
            readAction { WebApplicationStack.of(module) }
        )
        assertEquals(
            "precondition: only 'app' declares the application",
            listOf(app.name to "com.example.ShopApplication"),
            readAction { applicationsIn(module) + applicationsIn(app) }
        )
        return app
    }

    private fun applicationsIn(target: Module): List<Pair<String, String?>> {
        val annotation = JavaPsiFacade.getInstance(project)
            .findClass(SPRING_BOOT_APPLICATION, GlobalSearchScope.allScope(project))
            ?: throw AssertionError("the fixture must resolve $SPRING_BOOT_APPLICATION")
        return AnnotatedElementsSearch.searchPsiClasses(annotation, GlobalSearchScope.moduleScope(target)).findAll()
            .map { target.name to it.qualifiedName }
    }

    private suspend fun handlerPageSchema(): JsonNode = pageSchemaOf("/orders/page")

    private suspend fun pageSchemaOf(url: String): JsonNode {
        val endpoints = mapper.readTree(
            toolset.getEndpointContract(urlPattern = url, projectPath = project.basePath, httpMethod = "GET")
        )["endpoints"]
        assertEquals("exactly one handler for GET $url: $endpoints", 1, endpoints.size())
        val schema = endpoints.single()["responseSchema"]
        assertTrue("a response schema is expected, got $endpoints", schema != null && !schema.isNull)
        return schema
    }

    private companion object {
        const val ENABLE_WEB_SUPPORT = "org.springframework.data.web.config.EnableSpringDataWebSupport"
        const val SPRING_BOOT_APPLICATION = "org.springframework.boot.autoconfigure.SpringBootApplication"
        const val SERIALIZATION_MODE_KEY = "spring.data.web.pageable.serialization-mode"
        const val WEB_CONFIG = "com.example.config.WebConfig"
        const val VENDOR_WEB_CONFIG = "com.vendor.web.VendorWebConfig"
        const val VENDOR_WEB_CONFIG_PATH = "com/vendor/web/VendorWebConfig.java"
        const val VENDOR_TEST_IMPORTS = "com.example.shop.VendorTestImports"
        const val TEST_CONFIGURATION = "org.springframework.boot.test.context.TestConfiguration"
        const val IMPORT = "org.springframework.context.annotation.Import"
        val SPRING_BOOT_TEST = TestLibrary("org.springframework.boot:spring-boot-test:3.1.1", false)

        val VENDOR_WEB_CONFIG_SOURCE = """
            package com.vendor.web;

            import org.springframework.context.annotation.Configuration;
            import org.springframework.data.web.config.EnableSpringDataWebSupport;
            import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

            @Configuration
            @EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
            public class VendorWebConfig {
            }
        """.trimIndent()

        const val COMMON_WEB = "com.company.common.CommonWeb"

        val COMMON_WEB_SOURCE = """
            package com.company.common;

            import com.vendor.web.VendorWebConfig;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.context.annotation.Import;

            @Configuration
            @Import(VendorWebConfig.class)
            public class CommonWeb {
            }
        """.trimIndent()

        val ALPHA_VENDOR_IMPORTS = """
            package com.example.alpha;

            import com.vendor.web.VendorWebConfig;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.context.annotation.Import;

            @Configuration
            @Import(VendorWebConfig.class)
            public class VendorImports {
            }
        """.trimIndent()

        fun vendorTestImports() = """
            package com.example.shop;

            import com.vendor.web.VendorWebConfig;
            import org.springframework.boot.test.context.TestConfiguration;
            import org.springframework.context.annotation.Import;

            @TestConfiguration
            @Import(VendorWebConfig.class)
            public class VendorTestImports {
            }
        """.trimIndent()

        val ROOT_APPLICATION = """
            package com.example;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class ShopApplication {
            }
        """.trimIndent()

        val VIA_DTO_WEB_CONFIG = """
            package com.example.config;

            import org.springframework.context.annotation.Configuration;
            import org.springframework.data.web.config.EnableSpringDataWebSupport;
            import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

            @Configuration
            @EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
            public class WebConfig {
            }
        """.trimIndent()

        val SHOP_APPLICATION = """
            package com.example.shop;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class ShopApplication {
            }
        """.trimIndent()

        val ORDER_DTO_SOURCE = """
            package com.example.shop;

            public class OrderDto {
                public long getId() { return 0L; }
            }
        """.trimIndent()

        val PAGE_CONTROLLER = """
            package com.example.shop;

            import org.springframework.data.domain.Page;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class OrderController {
                @GetMapping("/orders/page")
                public Page<OrderDto> page() { return null; }
            }
        """.trimIndent()
    }
}

private const val ALIASED_WEB_SUPPORT = "com.example.config.EnableAliasedWeb"

private fun aliasedWebSupport(metaMode: String): String = """
    package com.example.config;

    import java.lang.annotation.ElementType;
    import java.lang.annotation.Retention;
    import java.lang.annotation.RetentionPolicy;
    import java.lang.annotation.Target;
    import org.springframework.core.annotation.AliasFor;
    import org.springframework.data.web.config.EnableSpringDataWebSupport;
    import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.$metaMode)
    public @interface EnableAliasedWeb {
        @AliasFor(annotation = EnableSpringDataWebSupport.class, attribute = "pageSerializationMode")
        PageSerializationMode mode() default PageSerializationMode.$metaMode;
    }
""".trimIndent()

private fun aliasedWebConfig(className: String, mode: String): String = """
    package com.example.config;

    import org.springframework.context.annotation.Configuration;
    import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

    @Configuration
    @EnableAliasedWeb(mode = PageSerializationMode.$mode)
    public class $className {
    }
""".trimIndent()
