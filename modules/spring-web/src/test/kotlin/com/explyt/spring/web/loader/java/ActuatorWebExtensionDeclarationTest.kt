/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.java

import com.explyt.spring.web.loader.ActuatorWebExtensionDeclarationTestCase

open class ActuatorWebExtensionDeclarationTest : ActuatorWebExtensionDeclarationTestCase() {

    override fun getTestDataPath() = "testdata/java/"

    override fun addApplicationWithEndpointsXAndY() {
        addJava("DemoApplication", """
            @SpringBootApplication
            public class DemoApplication {}
        """)
        addJava("XEndpoint", """
            @Component
            @Endpoint(id = "x")
            public class XEndpoint {
                @ReadOperation
                public String read() { return "x-endpoint"; }
            }
        """)
        addJava("YEndpoint", """
            @Component
            @Endpoint(id = "y")
            public class YEndpoint {
                @ReadOperation
                public String read() { return "y-endpoint"; }
            }
        """)
    }

    override fun addDirectExtension() = addJava("DirectExtension", """
        @EndpointWebExtension(endpoint = XEndpoint.class)
        @Component
        public class DirectExtension {
            @ReadOperation
            public String read() { return "ext"; }
        }
    """)

    override fun addComposedExtensionWithFixedEndpoint() {
        addFixedExt()
        addJava("ComposedExtension", """
            @FixedExt
            @Component
            public class ComposedExtension {
                @ReadOperation
                public String read() { return "ext"; }
            }
        """)
    }

    override fun addAliasExtensionUsingTheAliasDefault() {
        addJava("AliasExt", """
            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.RUNTIME)
            @EndpointWebExtension(endpoint = YEndpoint.class)
            public @interface AliasExt {
                @AliasFor(annotation = EndpointWebExtension.class, attribute = "endpoint")
                Class<?> endpoint() default XEndpoint.class;
            }
        """)
        addJava("AliasDefaultExtension", """
            @AliasExt
            @Component
            public class AliasDefaultExtension {
                @ReadOperation
                public String read() { return "ext"; }
            }
        """)
    }

    override fun addAliasExtensionSettingTheAliasAtUseSite() {
        addJava("AliasExtDefaultY", """
            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.RUNTIME)
            @EndpointWebExtension(endpoint = YEndpoint.class)
            public @interface AliasExtDefaultY {
                @AliasFor(annotation = EndpointWebExtension.class, attribute = "endpoint")
                Class<?> endpoint() default YEndpoint.class;
            }
        """)
        addJava("AliasUseSiteExtension", """
            @AliasExtDefaultY(endpoint = XEndpoint.class)
            @Component
            public class AliasUseSiteExtension {
                @ReadOperation
                public String read() { return "ext"; }
            }
        """)
    }

    override fun addRootAliasExtensionUsingTheAliasDefault() {
        addJava("RootAliasExt", """
            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.RUNTIME)
            @EndpointWebExtension(endpoint = YEndpoint.class)
            public @interface RootAliasExt {
                @AliasFor(annotation = EndpointExtension.class, attribute = "endpoint")
                Class<?> endpoint() default XEndpoint.class;
            }
        """)
        addJava("RootAliasExtension", """
            @RootAliasExt
            @Component
            public class RootAliasExtension {
                @ReadOperation
                public String read() { return "ext"; }
            }
        """)
    }

    override fun addUnaliasedSameNamedAttributeExtension() {
        addJava("ConventionExt", """
            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.RUNTIME)
            @EndpointWebExtension(endpoint = YEndpoint.class)
            public @interface ConventionExt {
                Class<?> endpoint() default XEndpoint.class;
            }
        """)
        addJava("ConventionExtension", """
            @ConventionExt
            @Component
            public class ConventionExtension {
                @ReadOperation
                public String read() { return "ext"; }
            }
        """)
    }

    override fun addExtensionInheritingADirectAnnotation() {
        addJava("AbstractDirectExtension", """
            @EndpointWebExtension(endpoint = XEndpoint.class)
            public abstract class AbstractDirectExtension {}
        """)
        addJava("InheritedDirectExtension", """
            @Component
            public class InheritedDirectExtension extends AbstractDirectExtension {
                @ReadOperation
                public String read() { return "ext"; }
            }
        """)
    }

    override fun addExtensionInheritingAComposedAnnotation() {
        addFixedExt()
        addJava("AbstractComposedExtension", """
            @FixedExt
            public abstract class AbstractComposedExtension {}
        """)
        addJava("InheritedComposedExtension", """
            @Component
            public class InheritedComposedExtension extends AbstractComposedExtension {
                @ReadOperation
                public String read() { return "ext"; }
            }
        """)
    }

    private fun addFixedExt() = addJava("FixedExt", """
        @Target(ElementType.TYPE)
        @Retention(RetentionPolicy.RUNTIME)
        @EndpointWebExtension(endpoint = XEndpoint.class)
        public @interface FixedExt {}
    """)

    private fun addJava(className: String, declaration: String) {
        myFixture.addFileToProject("com/app/$className.java", JAVA_HEADER + declaration.trimIndent())
    }

    private companion object {
        val JAVA_HEADER = """
            package com.app;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
            import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
            import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            import org.springframework.core.annotation.AliasFor;
            import org.springframework.stereotype.Component;
        """.trimIndent() + "\n\n"
    }
}
