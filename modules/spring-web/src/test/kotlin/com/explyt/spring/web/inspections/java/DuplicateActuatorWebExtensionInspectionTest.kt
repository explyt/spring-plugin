/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections.java

import com.explyt.spring.web.inspections.DuplicateActuatorWebExtensionInspectionTestCase

class DuplicateActuatorWebExtensionInspectionTest : DuplicateActuatorWebExtensionInspectionTestCase() {

    override val fileExtension = "java"

    override fun application() = """
        package com.app;

        import org.springframework.boot.autoconfigure.SpringBootApplication;

        @SpringBootApplication
        public class DemoApplication {}
    """.trimIndent()

    override fun header() = """
        package com.app;

        import java.lang.annotation.ElementType;
        import java.lang.annotation.Retention;
        import java.lang.annotation.RetentionPolicy;
        import java.lang.annotation.Target;
        import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
        import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
        import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
        import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
        import org.springframework.stereotype.Component;
    """.trimIndent()

    override fun endpoint(id: String, className: String) = """
        @Component
        @Endpoint(id = "$id")
        class $className { @ReadOperation public String read() { return "endpoint"; } }
    """.trimIndent()

    override fun extension(className: String, endpointClass: String, bean: Boolean, gated: Boolean) = listOfNotNull(
        "@Component".takeIf { bean },
        "@ConditionalOnProperty(name = \"$EXT_GATING_KEY\", havingValue = \"true\")".takeIf { gated },
        "@EndpointWebExtension(endpoint = $endpointClass.class)",
        "class $className { @ReadOperation public String read() { return \"$className\"; } }"
    ).joinToString("\n")

    override fun composedAnnotation(name: String, endpointClass: String) = """
        @Target(ElementType.TYPE)
        @Retention(RetentionPolicy.RUNTIME)
        @EndpointWebExtension(endpoint = $endpointClass.class)
        @interface $name {}
    """.trimIndent()

    override fun composedExtension(className: String, annotation: String) = """
        @Component
        @$annotation
        class $className { @ReadOperation public String read() { return "$className"; } }
    """.trimIndent()

    override fun beanSubclass(className: String, baseClass: String) = """
        @Component
        class $className extends $baseClass {}
    """.trimIndent()
}
