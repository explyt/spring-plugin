/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.assertAvailableOnlyInSmartMode

class InspectionAvailabilityInDumbModeTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWeb_6_0_7)

    fun testSpringWebBaseUastLocalInspectionTool() =
        object : SpringWebBaseUastLocalInspectionTool() {}.assertAvailableOnlyInSmartMode(
            myFixture.configureByText(
                "OrderController.java",
                """
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class OrderController {
                }
                """.trimIndent()
            )
        )
}
