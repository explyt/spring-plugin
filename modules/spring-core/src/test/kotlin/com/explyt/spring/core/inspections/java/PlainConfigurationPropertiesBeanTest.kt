/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.PlainConfigurationPropertiesBeanTestCase

class PlainConfigurationPropertiesBeanTest : PlainConfigurationPropertiesBeanTestCase() {

    override val extension = "java"

    override fun applicationSource(propertiesScan: Boolean) = """
        package app;

        import org.springframework.boot.autoconfigure.SpringBootApplication;
        import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

        @SpringBootApplication
        ${if (propertiesScan) "@ConfigurationPropertiesScan" else ""}
        public class App {}
    """.trimIndent()

    override fun propertiesSource(component: Boolean, packageName: String, className: String, prefix: String) = """
        package $packageName;

        import org.springframework.boot.context.properties.ConfigurationProperties;
        import org.springframework.stereotype.Component;

        ${if (component) "@Component" else ""}
        @ConfigurationProperties(prefix = "$prefix")
        public class $className {}
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

    override fun consumerSource(packageName: String) = """
        package app;

        import $packageName.Props;

        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.stereotype.Component;

        @Component
        public class Consumer {
            @Autowired
            Props $NEUTRAL_FIELD;
        }
    """.trimIndent()
}
