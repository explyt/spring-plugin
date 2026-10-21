/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.data.inspection

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.assertAvailableOnlyInSmartMode
import com.intellij.psi.PsiFile

class InspectionAvailabilityInDumbModeTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springDataJpa_3_1_0,
        TestLibrary.springJdbc_6_2_5,
    )

    fun testSpringDataBaseUastLocalInspectionTool() =
        object : SpringDataBaseUastLocalInspectionTool() {}.assertAvailableOnlyInSmartMode(sampleFile())

    fun testSpringJdbcClientParamInspection() =
        SpringJdbcClientParamInspection().assertAvailableOnlyInSmartMode(sampleFile())

    private fun sampleFile(): PsiFile = myFixture.configureByText(
        "OrderDao.java",
        """
        import org.springframework.jdbc.core.simple.JdbcClient;

        public class OrderDao {
            JdbcClient jdbcClient;
        }
        """.trimIndent()
    )
}
