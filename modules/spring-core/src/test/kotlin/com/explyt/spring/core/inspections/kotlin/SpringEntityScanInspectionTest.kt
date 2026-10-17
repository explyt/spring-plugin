/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.inspections.SpringEntityScanInspection
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.util.registry.Registry

class SpringEntityScanInspectionTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springContext_6_0_7,
        TestLibrary.jakarta_persistence_3_1_0,
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringEntityScanInspection::class.java)
        Registry.get("explyt.spring.root.runConfiguration").setValue(false)
    }

    override fun tearDown() {
        try {
            Registry.get("explyt.spring.root.runConfiguration").resetToDefault()
        } finally {
            super.tearDown()
        }
    }

    fun testEntityInNamedPackageIsScannedByDefaultPackageApplication() {
        myFixture.addFileToProject(
            "DemoApplication.kt",
            """
            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class DemoApplication
            """.trimIndent()
        )

        assertEmpty(entityScanProblemsOf("com/x/Person.kt", "com.x"))
    }

    fun testEntityOutsideNamedPackageApplicationIsReported() {
        myFixture.addFileToProject(
            "com/app/AppApplication.kt",
            """
            package com.app

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class AppApplication
            """.trimIndent()
        )

        assertSize(1, entityScanProblemsOf("com/x/Person.kt", "com.x"))
    }

    private fun entityScanProblemsOf(path: String, packageName: String): List<HighlightInfo> {
        val entity = myFixture.addFileToProject(
            path,
            """
            package $packageName

            @jakarta.persistence.Entity
            class Person {
                @jakarta.persistence.Id
                var id: Long? = null
            }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(entity.virtualFile)
        return myFixture.doHighlighting().filter { it.inspectionToolId == INSPECTION_ID }
    }

    private companion object {
        const val INSPECTION_ID = "SpringEntityScanInspection"
    }
}
