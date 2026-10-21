/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.EnabledPropertiesSecondBeanTestCase

class EnabledPropertiesSecondBeanTest : EnabledPropertiesSecondBeanTestCase() {

    override val extension = "java"

    override fun applicationSource() = """
        package app;

        import org.springframework.boot.autoconfigure.SpringBootApplication;

        @SpringBootApplication
        public class App {}
    """.trimIndent()

    override fun propertiesSource(scanned: Boolean, packageName: String) = """
        package $packageName;

        import org.springframework.boot.context.properties.ConfigurationProperties;
        import org.springframework.stereotype.Component;

        ${if (scanned) "@Component" else ""}
        @ConfigurationProperties(prefix = "$PREFIX")
        public class Props {}
    """.trimIndent()

    override fun enablerSource(packageName: String) = """
        package app;

        import $packageName.Props;

        import org.springframework.boot.context.properties.EnableConfigurationProperties;
        import org.springframework.context.annotation.Configuration;

        @Configuration
        @EnableConfigurationProperties(Props.class)
        public class PropsConfig {}
    """.trimIndent()

    override fun consumerSource(qualifier: String?, packageName: String, fieldName: String) = """
        package app;

        import $packageName.Props;

        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.beans.factory.annotation.Qualifier;
        import org.springframework.stereotype.Component;

        @Component
        public class Consumer {
            @Autowired
            ${qualifier?.let { "@Qualifier(\"$it\")" } ?: ""}
            Props $fieldName;
        }
    """.trimIndent()
}
