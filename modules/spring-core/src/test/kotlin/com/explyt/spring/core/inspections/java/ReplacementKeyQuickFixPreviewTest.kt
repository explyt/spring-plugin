/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringPropertiesInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

/**
 * The Alt+Enter preview invokes the fix on a file copy inside a read action, where starting a write action is
 * forbidden. These tests drive the real preview pipeline, which a highlighting or `launchAction` test never reaches.
 */
class ReplacementKeyQuickFixPreviewTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_3_1_1,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject(
            "properties/src/resources/META-INF/additional-spring-configuration-metadata.json",
            "META-INF/additional-spring-configuration-metadata.json"
        )
        myFixture.enableInspections(SpringPropertiesInspection::class.java)
    }

    fun testPreviewRenamesDeprecatedKey() {
        myFixture.configureByText("application.properties", "$DEPRECATED_KEY=1")

        val previewText = myFixture.getIntentionPreviewText(myFixture.findSingleIntention(fixName()))

        assertEquals("$REPLACEMENT_KEY=1", previewText)
    }

    fun testPreviewLeavesTheRealFileUntouched() {
        myFixture.configureByText("application.properties", "$DEPRECATED_KEY=1")

        myFixture.getIntentionPreviewText(myFixture.findSingleIntention(fixName()))

        myFixture.checkResult("$DEPRECATED_KEY=1")
    }

    fun testPreviewMatchesRealInvocation() {
        myFixture.configureByText("application.properties", "$DEPRECATED_KEY=1")

        myFixture.checkPreviewAndLaunchAction(myFixture.findSingleIntention(fixName()))

        myFixture.checkResult("$REPLACEMENT_KEY=1")
    }

    private fun fixName(): String =
        SpringCoreBundle.message("explyt.spring.inspection.properties.quick.fix.replacement", REPLACEMENT_KEY)

    private companion object {
        const val DEPRECATED_KEY = "main.foo2.notexist"
        const val REPLACEMENT_KEY = "my.try-integer"
    }
}
