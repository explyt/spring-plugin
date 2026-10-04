/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service

import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.module.Module
import org.intellij.lang.annotations.Language

/**
 * An application scans a package of a module it depends on, and a configuration found there scans further.
 *
 * Both hops are what Spring does at run time: `scanBasePackageClasses = [LibConfig::class]` registers `LibConfig`, and
 * `LibConfig`'s own `@ComponentScan` then registers every component under `com.example.lib`. The model must give the
 * application module both sets of beans, because the autowiring inspection, the bean gutters and the MCP bean tools all
 * read it.
 */
class ComponentScanFromDependencyModuleTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.kotlin_1_9_22,
    )

    fun testKotlinApplicationGetsTheComponentsALibraryConfigurationScans() {
        val library = addDependencyModule("lib")
        addFileToModule(library, "com/example/lib/config/LibConfig.kt", KOTLIN_LIB_CONFIG)
        addFileToModule(library, "com/example/lib/orders/OrderImportService.kt", KOTLIN_ORDER_IMPORT_SERVICE)
        addFileToModule(module, "com/example/app/ShopApplication.kt", KOTLIN_APPLICATION)
        addFileToModule(module, "com/example/app/OrdersController.kt", KOTLIN_CONTROLLER)

        assertApplicationSeesTheLibraryBeans(module)
    }

    fun testJavaApplicationGetsTheComponentsALibraryConfigurationScans() {
        val library = addDependencyModule("lib")
        addFileToModule(library, "com/example/lib/config/LibConfig.java", JAVA_LIB_CONFIG)
        addFileToModule(library, "com/example/lib/orders/OrderImportService.java", JAVA_ORDER_IMPORT_SERVICE)
        addFileToModule(module, "com/example/app/ShopApplication.java", JAVA_APPLICATION)
        addFileToModule(module, "com/example/app/OrdersController.java", JAVA_CONTROLLER)

        assertApplicationSeesTheLibraryBeans(module)
    }

    /**
     * A library that is itself a Spring Boot application keeps its own scope: its root packages, and the scans of the
     * configurations only *that* application reaches, are not where an application depending on it scans.
     */
    fun testADependencyApplicationRootDoesNotLeakIntoTheApplicationThatDependsOnIt() {
        val library = addDependencyModule("lib")
        addFileToModule(
            library, "com/example/tool/ToolApplication.kt", """
            package com.example.tool

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class ToolApplication
            """.trimIndent()
        )
        addFileToModule(
            library, "com/example/tool/ToolOnlyService.kt", """
            package com.example.tool

            import org.springframework.stereotype.Service

            @Service
            class ToolOnlyService
            """.trimIndent()
        )
        addFileToModule(
            library, "com/example/tool/ToolConfig.kt", """
            package com.example.tool

            import org.springframework.context.annotation.ComponentScan
            import org.springframework.context.annotation.Configuration

            @Configuration
            @ComponentScan(basePackages = ["com.example.toolextra"])
            class ToolConfig
            """.trimIndent()
        )
        addFileToModule(
            library, "com/example/toolextra/ToolExtraService.kt", """
            package com.example.toolextra

            import org.springframework.stereotype.Service

            @Service
            class ToolExtraService
            """.trimIndent()
        )
        addFileToModule(
            module, "com/example/app/ShopApplication.kt", """
            package com.example.app

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class ShopApplication
            """.trimIndent()
        )
        assertNotNull(
            "Precondition: the library's own application must resolve",
            findClass("com.example.tool.ToolApplication")
        )

        val packages = readAction { PackageScanService.getInstance(project).getAllPackages().getPackages(module) }
        assertFalse(
            "The library application's root must not become a scan root of the application depending on it, got $packages",
            packages.contains("com.example.tool.")
        )
        assertTrue(
            "Precondition: the library application itself reaches its configuration's scan",
            readAction { PackageScanService.getInstance(project).getAllPackages().getPackages(library) }
                .contains("com.example.toolextra.")
        )
        assertFalse(
            "A configuration the depending application never scans must not add its roots, got $packages",
            packages.contains("com.example.toolextra.")
        )
        val beans = activeBeanNames(module)
        assertFalse(
            "A bean only the library application scans must not be a bean of the depending application, got $beans",
            beans.contains("com.example.tool.ToolOnlyService") || beans.contains("com.example.toolextra.ToolExtraService")
        )
    }

    /**
     * The application's own controller lives in the package `scanBasePackages` names, and one of the libraries' classes
     * shares that package. A library application whose root package encloses the depending application's root must not
     * hand its root over just because the depending application's scan reaches the library application class.
     */
    fun testALibraryApplicationReachedByTheScanDoesNotAddItsRoot() {
        val library = addDependencyModule("lib")
        addFileToModule(
            library, "com/example/app/plugin/PluginApplication.kt", """
            package com.example.app.plugin

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication(scanBasePackages = ["com.example.pluginonly"])
            class PluginApplication
            """.trimIndent()
        )
        addFileToModule(
            library, "com/example/pluginonly/PluginOnlyService.kt", """
            package com.example.pluginonly

            import org.springframework.stereotype.Service

            @Service
            class PluginOnlyService
            """.trimIndent()
        )
        addFileToModule(
            module, "com/example/app/ShopApplication.kt", """
            package com.example.app

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class ShopApplication
            """.trimIndent()
        )
        assertNotNull(
            "Precondition: the library application must resolve",
            findClass("com.example.app.plugin.PluginApplication")
        )

        val packages = readAction { PackageScanService.getInstance(project).getAllPackages().getPackages(module) }
        assertFalse(
            "Another application's root is never a scan root of this one, got $packages",
            packages.contains("com.example.pluginonly.")
        )
    }

    private fun assertApplicationSeesTheLibraryBeans(application: Module) {
        assertNotNull(
            "Precondition: the library configuration must resolve, otherwise the fixture proves nothing",
            findClass("com.example.lib.config.LibConfig")
        )
        assertNotNull(
            "Precondition: the library service must resolve, otherwise the fixture proves nothing",
            findClass("com.example.lib.orders.OrderImportService")
        )

        val packages = readAction { PackageScanService.getInstance(project).getAllPackages().getPackages(application) }
        assertTrue(
            "scanBasePackageClasses names the configuration's package, got $packages",
            packages.contains("com.example.lib.config.")
        )
        val beans = activeBeanNames(application)
        assertTrue("LibConfig is a bean of the application, got $beans", beans.contains("com.example.lib.config.LibConfig"))
        assertTrue(
            "The configuration's own @ComponentScan reaches the whole library package, got $packages",
            packages.contains("com.example.lib.")
        )
        assertTrue(
            "OrderImportService is a bean of the application, got $beans",
            beans.contains("com.example.lib.orders.OrderImportService")
        )
        assertTrue(
            "The application's own controller stays a bean, got $beans",
            beans.contains("com.example.app.OrdersController")
        )
    }

    private fun activeBeanNames(application: Module): Set<String> = readAction {
        SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(application)
            .mapNotNullTo(mutableSetOf()) { it.psiClass.qualifiedName }
    }

    private fun findClass(qualifiedName: String) = readAction {
        com.intellij.psi.JavaPsiFacade.getInstance(project)
            .findClass(qualifiedName, com.intellij.psi.search.GlobalSearchScope.allScope(project))
    }

    private fun <T> readAction(action: () -> T): T =
        com.intellij.openapi.application.ReadAction.compute<T, RuntimeException> { action() }

    private companion object {
        @Language("kotlin")
        val KOTLIN_LIB_CONFIG = """
            package com.example.lib.config

            import org.springframework.context.annotation.ComponentScan
            import org.springframework.context.annotation.Configuration

            @Configuration
            @ComponentScan(basePackages = ["com.example.lib"])
            class LibConfig
            """.trimIndent()

        @Language("kotlin")
        val KOTLIN_ORDER_IMPORT_SERVICE = """
            package com.example.lib.orders

            import org.springframework.stereotype.Service

            @Service
            class OrderImportService
            """.trimIndent()

        @Language("kotlin")
        val KOTLIN_APPLICATION = """
            package com.example.app

            import com.example.lib.config.LibConfig
            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication(
                scanBasePackages = ["com.example.app"],
                scanBasePackageClasses = [LibConfig::class]
            )
            class ShopApplication
            """.trimIndent()

        @Language("kotlin")
        val KOTLIN_CONTROLLER = """
            package com.example.app

            import com.example.lib.orders.OrderImportService
            import org.springframework.stereotype.Controller

            @Controller
            class OrdersController(private val importer: OrderImportService)
            """.trimIndent()

        @Language("java")
        val JAVA_LIB_CONFIG = """
            package com.example.lib.config;

            import org.springframework.context.annotation.ComponentScan;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @ComponentScan(basePackages = "com.example.lib")
            public class LibConfig {}
            """.trimIndent()

        @Language("java")
        val JAVA_ORDER_IMPORT_SERVICE = """
            package com.example.lib.orders;

            import org.springframework.stereotype.Service;

            @Service
            public class OrderImportService {}
            """.trimIndent()

        @Language("java")
        val JAVA_APPLICATION = """
            package com.example.app;

            import com.example.lib.config.LibConfig;
            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication(scanBasePackages = "com.example.app", scanBasePackageClasses = LibConfig.class)
            public class ShopApplication {}
            """.trimIndent()

        @Language("java")
        val JAVA_CONTROLLER = """
            package com.example.app;

            import com.example.lib.orders.OrderImportService;
            import org.springframework.stereotype.Controller;

            @Controller
            public class OrdersController {
                public OrdersController(OrderImportService importer) {}
            }
            """.trimIndent()
    }
}
