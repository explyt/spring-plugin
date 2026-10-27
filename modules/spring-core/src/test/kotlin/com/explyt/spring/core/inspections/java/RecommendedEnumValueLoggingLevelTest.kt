/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringRecommendedEnumValueInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

/**
 * Acceptance test for the noise guard that matters most: `logging.level.*=INFO` is what every Spring project writes.
 *
 * The key is a `Map<String, String>` with a `logging.level.values` hint, so its values are not enum constants at all —
 * but the value type of a map is its declared value type, and a nearby enum would be enough to make the naive path
 * report it. Reporting here would make the inspection unusable, so this is a blocker-level assertion.
 */
class RecommendedEnumValueLoggingLevelTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_3_1_1,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    override fun setUp() {
        super.setUp()
        myFixture.addClass(
            """
            package com.explyt;

            public enum RecordingInclude {
                TIME_TAKEN, REQUEST_HEADERS, RESPONSE_HEADERS
            }
            """.trimIndent()
        )
        myFixture.copyFileToProject(
            "recommendedEnumValue/META-INF/additional-spring-configuration-metadata.json",
            "META-INF/additional-spring-configuration-metadata.json"
        )
        myFixture.enableInspections(SpringRecommendedEnumValueInspection::class.java)
    }

    fun testLoggingLevelRootNotReported() = assertNotReported("logging.level.root=INFO")

    fun testLoggingLevelPackageNotReported() = assertNotReported("logging.level.org.springframework=INFO")

    fun testLoggingLevelDebugNotReported() = assertNotReported("logging.level.web=DEBUG")

    fun testLoggingLevelYamlNotReported() {
        myFixture.configureByText(
            "application.yaml",
            """
            logging:
              level:
                root: INFO
                org.springframework: DEBUG
            """.trimIndent()
        )

        val problems = recommendationProblems()

        assertEmpty("log levels must never be reported: $problems", problems)
    }

    fun testHintBackedActuatorExposureNotReported() =
        assertNotReported("management.endpoints.web.exposure.include=*")

    /**
     * `logging.level` alone does not exercise the hint guard: its value type is `java.lang.String`, so the enum branch
     * never starts. An enum-typed property that also declares hints does, and its metadata already states which
     * literals to write.
     */
    fun testHintedEnumPropertyNotReported() = assertNotReported("explyt.hinted.include=REQUEST_HEADERS")

    private fun assertNotReported(propertyLine: String) {
        myFixture.configureByText("application.properties", propertyLine)

        val problems = recommendationProblems()

        assertEmpty("nothing must be reported here: $problems", problems)
    }

    private fun recommendationProblems(): List<String> = myFixture.doHighlighting()
        .mapNotNull { it.description }
        .filter { it.contains("Spring writes this enum value") }
}
