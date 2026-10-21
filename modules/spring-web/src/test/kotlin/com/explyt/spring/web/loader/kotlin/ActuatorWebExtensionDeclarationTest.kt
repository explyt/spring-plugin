/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.web.loader.ActuatorWebExtensionDeclarationTestCase

class ActuatorWebExtensionDeclarationTest : ActuatorWebExtensionDeclarationTestCase() {

    override fun getTestDataPath() = "testdata/kotlin/"

    override fun addApplicationWithEndpointsXAndY() {
        addKotlin("DemoApplication", """
            @SpringBootApplication
            class DemoApplication
        """)
        addKotlin("XEndpoint", """
            @Component
            @Endpoint(id = "x")
            class XEndpoint {
                @ReadOperation
                fun read(): String = "x-endpoint"
            }
        """)
        addKotlin("YEndpoint", """
            @Component
            @Endpoint(id = "y")
            class YEndpoint {
                @ReadOperation
                fun read(): String = "y-endpoint"
            }
        """)
    }

    override fun addDirectExtension() = addKotlin("DirectExtension", """
        @EndpointWebExtension(endpoint = XEndpoint::class)
        @Component
        class DirectExtension {
            @ReadOperation
            fun read(): String = "ext"
        }
    """)

    override fun addComposedExtensionWithFixedEndpoint() {
        addFixedExt()
        addKotlin("ComposedExtension", """
            @FixedExt
            @Component
            class ComposedExtension {
                @ReadOperation
                fun read(): String = "ext"
            }
        """)
    }

    override fun addAliasExtensionUsingTheAliasDefault() {
        addKotlin("AliasExt", """
            @Target(AnnotationTarget.CLASS)
            @Retention(AnnotationRetention.RUNTIME)
            @EndpointWebExtension(endpoint = YEndpoint::class)
            annotation class AliasExt(
                @get:AliasFor(annotation = EndpointWebExtension::class, attribute = "endpoint")
                val endpoint: KClass<*> = XEndpoint::class,
            )
        """)
        addKotlin("AliasDefaultExtension", """
            @AliasExt
            @Component
            class AliasDefaultExtension {
                @ReadOperation
                fun read(): String = "ext"
            }
        """)
    }

    override fun addAliasExtensionSettingTheAliasAtUseSite() {
        addKotlin("AliasExtDefaultY", """
            @Target(AnnotationTarget.CLASS)
            @Retention(AnnotationRetention.RUNTIME)
            @EndpointWebExtension(endpoint = YEndpoint::class)
            annotation class AliasExtDefaultY(
                @get:AliasFor(annotation = EndpointWebExtension::class, attribute = "endpoint")
                val endpoint: KClass<*> = YEndpoint::class,
            )
        """)
        addKotlin("AliasUseSiteExtension", """
            @AliasExtDefaultY(endpoint = XEndpoint::class)
            @Component
            class AliasUseSiteExtension {
                @ReadOperation
                fun read(): String = "ext"
            }
        """)
    }

    override fun addRootAliasExtensionUsingTheAliasDefault() {
        addKotlin("RootAliasExt", """
            @Target(AnnotationTarget.CLASS)
            @Retention(AnnotationRetention.RUNTIME)
            @EndpointWebExtension(endpoint = YEndpoint::class)
            annotation class RootAliasExt(
                @get:AliasFor(annotation = EndpointExtension::class, attribute = "endpoint")
                val endpoint: KClass<*> = XEndpoint::class,
            )
        """)
        addKotlin("RootAliasExtension", """
            @RootAliasExt
            @Component
            class RootAliasExtension {
                @ReadOperation
                fun read(): String = "ext"
            }
        """)
    }

    override fun addUnaliasedSameNamedAttributeExtension() {
        addKotlin("ConventionExt", """
            @Target(AnnotationTarget.CLASS)
            @Retention(AnnotationRetention.RUNTIME)
            @EndpointWebExtension(endpoint = YEndpoint::class)
            annotation class ConventionExt(val endpoint: KClass<*> = XEndpoint::class)
        """)
        addKotlin("ConventionExtension", """
            @ConventionExt
            @Component
            class ConventionExtension {
                @ReadOperation
                fun read(): String = "ext"
            }
        """)
    }

    override fun addExtensionInheritingADirectAnnotation() {
        addKotlin("AbstractDirectExtension", """
            @EndpointWebExtension(endpoint = XEndpoint::class)
            abstract class AbstractDirectExtension
        """)
        addKotlin("InheritedDirectExtension", """
            @Component
            class InheritedDirectExtension : AbstractDirectExtension() {
                @ReadOperation
                fun read(): String = "ext"
            }
        """)
    }

    override fun addExtensionInheritingAComposedAnnotation() {
        addFixedExt()
        addKotlin("AbstractComposedExtension", """
            @FixedExt
            abstract class AbstractComposedExtension
        """)
        addKotlin("InheritedComposedExtension", """
            @Component
            class InheritedComposedExtension : AbstractComposedExtension() {
                @ReadOperation
                fun read(): String = "ext"
            }
        """)
    }

    private fun addFixedExt() = addKotlin("FixedExt", """
        @Target(AnnotationTarget.CLASS)
        @Retention(AnnotationRetention.RUNTIME)
        @EndpointWebExtension(endpoint = XEndpoint::class)
        annotation class FixedExt
    """)

    private fun addKotlin(className: String, declaration: String) {
        myFixture.addFileToProject("com/app/$className.kt", KOTLIN_HEADER + declaration.trimIndent())
    }

    private companion object {
        val KOTLIN_HEADER = """
            package com.app

            import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
            import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension
            import org.springframework.boot.autoconfigure.SpringBootApplication
            import org.springframework.core.annotation.AliasFor
            import org.springframework.stereotype.Component
            import kotlin.reflect.KClass
        """.trimIndent() + "\n\n"
    }
}
