/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.base.LibraryClassCache
import com.explyt.inspection.SpringBaseLocalInspectionTool
import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.assertAvailableOnlyInSmartMode
import com.explyt.spring.test.util.computeInDumbModeWithColdLibraryCaches
import com.explyt.util.SpringBaseClasses.CORE_ENVIRONMENT
import com.intellij.psi.PsiFile

class InspectionAvailabilityInDumbModeTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_4_0_0,
        TestLibrary.javax_annotation_1_3_2,
    )

    fun testSpringBaseUastLocalInspectionTool() =
        object : SpringBaseUastLocalInspectionTool() {}.assertAvailableOnlyInSmartMode(sampleFile())

    fun testSpringBaseLocalInspectionTool() =
        object : SpringBaseLocalInspectionTool() {}.assertAvailableOnlyInSmartMode(sampleFile())

    fun testSpring4UastLocalInspectionTool() {
        myFixture.addClass("package com.fasterxml.jackson.databind; public class ObjectMapper { }")
        myFixture.addClass("package tools.jackson.databind.json; public class JsonMapper { }")
        SpringBoot4Jackson2ObjectMapperInspection().assertAvailableOnlyInSmartMode(sampleFile())
    }

    fun testSpring4LocalInspectionTool() =
        SpringBoot4PropertyMigrationInspection().assertAvailableOnlyInSmartMode(sampleFile())

    fun testSpringBoot3JakartaMigrationInspection() =
        SpringBoot3JakartaMigrationInspection().assertAvailableOnlyInSmartMode(sampleFile())

    fun testLibraryClassCacheFindsNothingInDumbModeAndResolvesAfterIndexing() {
        assertNotNull(
            "the fixture must provide $CORE_ENVIRONMENT in smart mode",
            LibraryClassCache.searchForLibraryClass(project, CORE_ENVIRONMENT)
        )
        assertNull(
            "a dumb-mode lookup must answer nothing instead of touching indexes",
            project.computeInDumbModeWithColdLibraryCaches {
                LibraryClassCache.searchForLibraryClass(project, CORE_ENVIRONMENT)
            }
        )
        assertNotNull(
            "the dumb-mode answer must not outlive indexing",
            LibraryClassCache.searchForLibraryClass(project, CORE_ENVIRONMENT)
        )
    }

    private fun sampleFile(): PsiFile = myFixture.configureByText(
        "Sample.java",
        """
        import javax.annotation.PostConstruct;
        
        public class Sample {
            @PostConstruct
            void init() {}
        }
        """.trimIndent()
    )
}
