/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections.kotlin

import com.explyt.spring.web.inspections.DuplicateActuatorWebExtensionInspectionTestCase

class DuplicateActuatorWebExtensionInspectionTest : DuplicateActuatorWebExtensionInspectionTestCase() {

    override val fileExtension = "kt"

    override fun application() = """
        package com.app

        import org.springframework.boot.autoconfigure.SpringBootApplication

        @SpringBootApplication
        class DemoApplication
    """.trimIndent()

    override fun header() = """
        package com.app

        import org.springframework.boot.actuate.endpoint.annotation.Endpoint
        import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
        import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension
        import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
        import org.springframework.stereotype.Component
    """.trimIndent()

    override fun endpoint(id: String, className: String) = """
        @Component
        @Endpoint(id = "$id")
        class $className { @ReadOperation fun read(): String = "endpoint" }
    """.trimIndent()

    override fun extension(className: String, endpointClass: String, bean: Boolean, gated: Boolean) = listOfNotNull(
        "@Component".takeIf { bean },
        "@ConditionalOnProperty(name = [\"$EXT_GATING_KEY\"], havingValue = \"true\")".takeIf { gated },
        "@EndpointWebExtension(endpoint = $endpointClass::class)",
        "open class $className { @ReadOperation fun read(): String = \"$className\" }"
    ).joinToString("\n")

    override fun composedAnnotation(name: String, endpointClass: String) = """
        @Target(AnnotationTarget.CLASS)
        @Retention(AnnotationRetention.RUNTIME)
        @EndpointWebExtension(endpoint = $endpointClass::class)
        annotation class $name
    """.trimIndent()

    override fun composedExtension(className: String, annotation: String) = """
        @Component
        @$annotation
        class $className { @ReadOperation fun read(): String = "$className" }
    """.trimIndent()

    override fun beanSubclass(className: String, baseClass: String) = """
        @Component
        class $className : $baseClass()
    """.trimIndent()
}
