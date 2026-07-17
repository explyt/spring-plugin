/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringBoot3JakartaMigrationInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language

class SpringBoot3JakartaMigrationInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1,
        // Both the legacy javax.* and the new jakarta.* artifacts are on the classpath so the original imports
        // resolve (as in a real mid-migration project) and the jakarta.* migration target exists.
        TestLibrary.javax_annotation_1_3_2,
        TestLibrary.jakarta_annotation_2_1_1,
        TestLibrary.javax_persistence_2_2,
        TestLibrary.jakarta_persistence_3_1_0,
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringBoot3JakartaMigrationInspection::class.java)
    }

    fun testJavaxAnnotationImportReported() {
        @Language("kotlin") val code = """
            <warning>import javax.annotation.PostConstruct</warning>
            import org.springframework.stereotype.Component
            
            @Component
            class SomeBean {
                @PostConstruct
                fun init() {}
            }
        """.trimIndent()
        myFixture.configureByText("SomeBean.kt", code)
        myFixture.testHighlighting("SomeBean.kt")
    }

    fun testJavaxPersistenceImportReported() {
        @Language("kotlin") val code = """
            <warning>import javax.persistence.Entity</warning>
            
            @Entity
            class SomeEntity
        """.trimIndent()
        myFixture.configureByText("SomeEntity.kt", code)
        myFixture.testHighlighting("SomeEntity.kt")
    }

    fun testNonMigratedImportNotReported() {
        @Language("kotlin") val code = """
            import java.util.UUID
            import org.springframework.stereotype.Component
            
            @Component
            class SomeBean {
                val id: UUID = UUID.randomUUID()
            }
        """.trimIndent()
        myFixture.configureByText("SomeBean.kt", code)
        myFixture.testHighlighting("SomeBean.kt")
    }

    fun testQuickFixRewritesImportToJakarta() {
        @Language("kotlin") val code = """
            import javax.annotation.Post<caret>Construct
            import org.springframework.stereotype.Component
            
            @Component
            class SomeBean {
                @PostConstruct
                fun init() {}
            }
        """.trimIndent()
        myFixture.configureByText("SomeBean.kt", code)
        val fixName = SpringCoreBundle.message(
            "explyt.spring.inspection.jakarta.migration.fix",
            "jakarta.annotation.PostConstruct"
        )
        val intention = myFixture.findSingleIntention(fixName)
        myFixture.launchAction(intention)

        @Language("kotlin") val expected = """
            import jakarta.annotation.PostConstruct
            import org.springframework.stereotype.Component
            
            @Component
            class SomeBean {
                @PostConstruct
                fun init() {}
            }
        """.trimIndent()
        myFixture.checkResult(expected)
    }
}
