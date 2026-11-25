/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.PsiFileReferenceInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.quickFix.CreateFilePathFix
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.test.TestMetadata

class PsiFileReferenceInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springTest_6_0_7
    )

    @TestMetadata("fileResource")
    fun testFileResource() = doTest(PsiFileReferenceInspection())

    fun testMissingClasspathFileCreationRemainsAvailableInBatchMode() {
        val bean = myFixture.addClass(
            """
            public class ResourceConsumer {
                @org.springframework.beans.factory.annotation.Value("classpath:explyt-missing.properties")
                private org.springframework.core.io.Resource resource;
            }
            """.trimIndent()
        )
        val annotation = bean.fields.single().annotations.single()
        val holder = ProblemsHolder(InspectionManager.getInstance(project), bean.containingFile, true)
        annotation.accept(PsiFileReferenceInspection().buildVisitor(holder, true))
        assertEquals("Expected the missing classpath file diagnostic", 1, holder.results.size)
        val fixes = holder.results.single().fixes.orEmpty().filterIsInstance<CreateFilePathFix>()
        assertEquals("Expected the create-file fix", 1, fixes.size)
        assertTrue("PsiFileReferenceInspection creation fix must remain available in batch mode", fixes.single().availableInBatchMode())
    }
}
