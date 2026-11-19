/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.InspectionEngine
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemDescriptorBase
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.PlatformTestUtil
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

abstract class RedundantEnvironmentPlaceholderInspectionTestCase : ExplytInspectionKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_1_1)

    protected abstract val fileName: String
    protected abstract fun configuration(vararg entries: Pair<String, String>): String
    private lateinit var tool: LocalInspectionTool

    override fun setUp() {
        super.setUp()
        val extension = LocalInspectionEP.LOCAL_INSPECTION.extensionList.singleOrNull { it.shortName == SHORT_NAME }
        assertNotNull("inspection $SHORT_NAME is registered", extension)
        tool = extension!!.instantiateTool() as LocalInspectionTool
        assertEquals("inspection defaults to weak warning", "WEAK WARNING", tool.defaultLevel.severity.name)
        myFixture.enableInspections(tool)
    }

    fun testOnlyPlaceholderSyntaxIsGreyAndDefaultIsNotHighlighted() {
        configure("explyt.server.host" to placeholder("EXPLYT_SERVER_HOST", "0.0.0.0"))
        assertHighlighted("\${EXPLYT_SERVER_HOST:", "}")
    }

    fun testEditorQuickFixReplacesPlaceholderWithDefault() {
        assertEditorFix("0.0.0.0")
    }

    fun testEmptyDefaultQuickFixLeavesAnEmptyValue() {
        assertEditorFix("")
    }

    fun testEditorlessFixReplacesPlaceholderWithDefault() {
        configure("explyt.server.host" to placeholder("EXPLYT_SERVER_HOST", "0.0.0.0"))
        val problems = assertHighlighted("\${EXPLYT_SERVER_HOST:", "}")
        val problem = problems.firstOrNull { !it.fixes.isNullOrEmpty() }
        assertNotNull("a redundant placeholder with a literal default has a batch fix", problem)
        val fixes = problem!!.fixes!!
        assertEquals(1, fixes.size)
        WriteCommandAction.runWriteCommandAction(project) {
            fixes.single().applyFix(project, problem)
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        myFixture.checkResult(configuration("explyt.server.host" to "0.0.0.0"))
    }

    fun testMixedTextIsNotReported() {
        assertNegative("explyt.db.url", "jdbc:h2:\${EXPLYT_DB_URL}/db")
    }

    fun testMultiplePlaceholdersAreNotReported() {
        assertNegative("explyt.server.host", "\${EXPLYT_SERVER_HOST}-\${EXPLYT_OTHER_HOST}")
    }

    fun testDifferentEnvironmentVariableIsNotReported() {
        assertNegative("explyt.server.host", placeholder("OTHER_HOST", "0.0.0.0"))
    }

    fun testParentKeyEnvironmentVariableIsNotReported() {
        assertNegative("explyt.server.host", placeholder("EXPLYT_SERVER", "x"))
    }

    fun testKebabKeyUsesEnvironmentFormWithoutHyphens() {
        assertEquals("EXPLYT_SERVER_MAXSIZE", PropertyUtil.toSystemEnvironmentForm("explyt.server.max-size"))
        configure("explyt.server.max-size" to placeholder("EXPLYT_SERVER_MAXSIZE", "16"))
        assertHighlighted("\${EXPLYT_SERVER_MAXSIZE:", "}")
    }

    fun testUnderscoreInPlaceOfRemovedHyphenIsNotReported() {
        assertNegative("explyt.server.max-size", placeholder("EXPLYT_SERVER_MAX_SIZE", "16"))
    }

    fun testPlaceholderWithoutDefaultIsGreyButHasNoFix() {
        configure("explyt.server.host" to "\${EXPLYT_SERVER_HOST}")
        assertNoFix(assertHighlighted("\${EXPLYT_SERVER_HOST}"))
    }

    fun testNestedPlaceholderDefaultHasNoFix() {
        configure("explyt.server.host" to placeholder("EXPLYT_SERVER_HOST", "\${explyt.fallback}"))
        assertNoFix(assertHighlighted("\${EXPLYT_SERVER_HOST:", "}"))
    }

    fun testSpelDefaultHasNoFix() {
        configure("explyt.server.host" to placeholder("EXPLYT_SERVER_HOST", "#{null}"))
        assertNoFix(assertHighlighted("\${EXPLYT_SERVER_HOST:", "}"))
    }

    fun testProfileConfigurationIsInspectedAndFixed() {
        assertEditorFix("0.0.0.0", fileName.replace("application", "application-dev"))
    }

    fun testRegistrationAllowsCodeCleanupAndCleanupAppliesFix() {
        configure("explyt.server.host" to placeholder("EXPLYT_SERVER_HOST", "0.0.0.0"))
        assertHighlighted("\${EXPLYT_SERVER_HOST:", "}")
        val profile = InspectionProjectProfileManager.getInstance(project).currentProfile
        val wrapper = profile.getInspectionTool(SHORT_NAME, project)
        assertNotNull("registered inspection is in the active profile", wrapper)
        assertTrue("inspection is registered with cleanupTool=true", wrapper!!.isCleanupTool)
        val context = InspectionManager.getInstance(project).createNewGlobalContext(false) as GlobalInspectionContextImpl
        val finished = AtomicBoolean()
        try {
            context.codeCleanup(AnalysisScope(myFixture.file), profile, null, { finished.set(true) }, true) { true }
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
            assertTrue("Code Cleanup completed", finished.get())
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            myFixture.checkResult(configuration("explyt.server.host" to "0.0.0.0"))
        } finally {
            context.cleanup()
        }
    }

    private fun assertEditorFix(default: String, name: String = fileName) {
        val original = configuration("explyt.server.host" to placeholder("EXPLYT_SERVER_HOST", default))
        myFixture.configureByText(name, original)
        assertHighlighted("\${EXPLYT_SERVER_HOST:", "}")
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("\${") + 3)
        val fixes = myFixture.getAllQuickFixes()
        assertEquals("one fix is offered for a literal default", 1, fixes.size)
        myFixture.launchAction(fixes.single())
        myFixture.checkResult(original.replace(placeholder("EXPLYT_SERVER_HOST", default), default))
    }

    private fun assertNegative(key: String, value: String) {
        configure(key to value, "explyt.control.host" to placeholder("EXPLYT_CONTROL_HOST", "control"))
        assertHighlighted("\${EXPLYT_CONTROL_HOST:", "}")
    }

    private fun configure(vararg entries: Pair<String, String>) {
        myFixture.configureByText(fileName, configuration(*entries))
    }

    private fun assertHighlighted(vararg expected: String): List<ProblemDescriptor> {
        val file = myFixture.file
        val problems = InspectionEngine.inspectEx(
            listOf(LocalInspectionToolWrapper(tool)), file, file.textRange, file.textRange,
            true, false, true, EmptyProgressIndicator()
        ) { _, _ -> true }.values.flatten().sortedBy { (it as ProblemDescriptorBase).textRange!!.startOffset }
        assertEquals("exact redundant placeholder ranges", expected.toList(), problems.map {
            file.text.substring((it as ProblemDescriptorBase).textRange!!.startOffset, it.textRange!!.endOffset)
        })
        problems.forEach { assertEquals(ProblemHighlightType.LIKE_UNUSED_SYMBOL, it.highlightType) }
        return problems
    }

    private fun assertNoFix(problems: List<ProblemDescriptor>) {
        assertTrue("unsafe placeholders have no descriptor fixes", problems.all { it.fixes.isNullOrEmpty() })
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("\${") + 3)
        assertEquals("unsafe placeholders have no editor fixes", emptyList<Any>(), myFixture.getAllQuickFixes())
    }

    private fun placeholder(variable: String, default: String): String = "\${$variable:$default}"

    private companion object {
        const val SHORT_NAME = "SpringRedundantEnvironmentPlaceholderInspection"
    }
}

class SpringRedundantEnvironmentPlaceholderYamlInspectionTest : RedundantEnvironmentPlaceholderInspectionTestCase() {
    override val fileName = "application.yml"

    override fun configuration(vararg entries: Pair<String, String>): String =
        render(entries.map { (key, value) -> key.split('.') to value }, 0)

    private fun render(entries: List<Pair<List<String>, String>>, depth: Int): String =
        entries.groupBy { it.first.first() }.entries.joinToString("\n") { (segment, children) ->
            val prefix = "  ".repeat(depth) + segment + ":"
            if (children.singleOrNull()?.first?.size == 1) {
                prefix + " " + children.single().second
            } else {
                prefix + "\n" + render(children.map { (key, value) -> key.drop(1) to value }, depth + 1)
            }
        }
}

class SpringRedundantEnvironmentPlaceholderPropertiesInspectionTest : RedundantEnvironmentPlaceholderInspectionTestCase() {
    override val fileName = "application.properties"

    override fun configuration(vararg entries: Pair<String, String>): String =
        entries.joinToString("\n") { (key, value) -> "$key=$value" }
}

class SpringRedundantEnvironmentPlaceholderEnvironmentFormTest {
    @Test
    fun testFullKeyEnvironmentForms() {
        assertEquals("EXPLYT_SERVER_HOST", PropertyUtil.toSystemEnvironmentForm("explyt.server.host"))
        assertEquals("EXPLYT_SERVER", PropertyUtil.toSystemEnvironmentForm("explyt.server"))
        assertEquals("EXPLYT_SERVER_MAXSIZE", PropertyUtil.toSystemEnvironmentForm("explyt.server.max-size"))
        assertEquals("EXPLYT_CONTROL_HOST", PropertyUtil.toSystemEnvironmentForm("explyt.control.host"))
        assertEquals("EXPLYT_DB_URL", PropertyUtil.toSystemEnvironmentForm("explyt.db.url"))
    }
}
