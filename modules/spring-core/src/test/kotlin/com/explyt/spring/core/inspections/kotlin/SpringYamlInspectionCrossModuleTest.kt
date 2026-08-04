/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.inspections.SpringYamlInspection
import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language

/**
 * Regression for issue #276.
 *
 * A property consumed only from a **dependency module** is still a real, resolvable property:
 * Spring injects it at runtime, and an unresolved required `@Value` or `@Scheduled` cron
 * placeholder would fail context startup. `SpringYamlInspection` used to report
 * `Cannot resolve key property` for such keys, because the reference fallback in
 * `SpringBasePropertyInspection.getProblemKey` searched a scope that excludes the modules the
 * configuration file's module depends on.
 *
 * This needs a multi-module fixture: with a single module the keys resolve and the bug is invisible.
 */
class SpringYamlInspectionCrossModuleTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_3_1_1
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringYamlInspection::class.java)
    }

    fun testKeysConsumedOnlyFromDependencyModuleResolve() {
        val library = addDependencyModule("library")
        addFileToModule(library, "com/example/library/KnownProperties.kt", KNOWN_PROPERTIES)

        @Language("kotlin") val libraryConsumers = """
            package com.example.library

            import org.springframework.beans.factory.annotation.Value
            import org.springframework.scheduling.annotation.Scheduled
            import org.springframework.stereotype.Component

            @Component
            class LibrarySecretConsumer(
                @Value("\${'$'}{explyt.notifications.secret-key}") val secretKey: String
            )

            @Component
            class LibraryCleanupJob {
                @Scheduled(cron = "\${'$'}{explyt.cron.cleanup-notifications}")
                fun cleanup() = Unit
            }
        """.trimIndent()
        addFileToModule(library, "com/example/library/LibraryConsumers.kt", libraryConsumers)

        // Control living in the application module: proves the fixture resolves same-module keys,
        // so a failure below is really about the cross-module boundary.
        @Language("kotlin") val applicationConsumer = """
            package com.example.app

            import org.springframework.beans.factory.annotation.Value
            import org.springframework.stereotype.Component

            @Component
            class AppConsumer(
                @Value("\${'$'}{explyt.local.enabled}") val enabled: String
            )
        """.trimIndent()
        addFileToModule(module, "com/example/app/AppConsumer.kt", applicationConsumer)

        myFixture.configureByText(
            "application.yaml",
            """
explyt.known:
  enabled: true
explyt.local:
  enabled: true
explyt.notifications:
  secret-key: my-secret-key
explyt.cron:
  cleanup-notifications: 0 */5 * * * *
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    /**
     * Guards the guard: if this test ever stops reporting a problem for a genuinely unknown key,
     * the fixture has stopped exercising the inspection and
     * [testKeysConsumedOnlyFromDependencyModuleResolve] would pass for the wrong reason.
     */
    fun testUnknownKeyUnderKnownPrefixIsStillReported() {
        val library = addDependencyModule("library")
        addFileToModule(library, "com/example/library/KnownProperties.kt", KNOWN_PROPERTIES)

        myFixture.configureByText(
            "application.yaml",
            """
explyt.known:
  enabled: true
  <warning descr="Cannot resolve key property 'explyt.known.never-declared'">never-declared</warning>: someValue
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    private companion object {
        /**
         * `@ConfigurationProperties` under the same `explyt.` base prefix is REQUIRED for these
         * tests to be meaningful, not decoration. `checkKey` bails out entirely when the module has
         * no configuration properties, and `getProblemKey` skips any key whose base prefix matches
         * no known property ("defined directly in the file"). Without this class both gates short
         * circuit and the tests pass vacuously. It also mirrors the real-world report, where a
         * dependency-module `@ConfigurationProperties` resolved cleanly while the placeholder-only
         * keys did not - the asymmetry that localizes the bug to placeholder discovery.
         */
        @Language("kotlin")
        val KNOWN_PROPERTIES = """
            package com.example.library

            import org.springframework.boot.context.properties.ConfigurationProperties

            @ConfigurationProperties(prefix = "explyt.known")
            class KnownProperties {
                var enabled: Boolean = false
            }
        """.trimIndent()
    }
}
