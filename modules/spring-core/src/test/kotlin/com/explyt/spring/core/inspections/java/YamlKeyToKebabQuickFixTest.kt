/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringYamlInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.core.inspections.quickfix.YamlKeyToKebabQuickFix
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl

/**
 * The fix rewrites every usage of the renamed key, and those usages are plain string literals in Java source.
 *
 * A usage written as a bare key — `@ConditionalOnProperty(name = "a.b.cD")` — is what makes rebuilding the literal
 * through a Java expression parser fail. The old code stripped the delimiters with
 * `text.substringAfter("{").substringBefore("}")`, which returns the *whole* text when the literal has no `${}`
 * around it, so the quotes ended up inside the replaced range and the parser was handed a bare, unquoted key.
 * Kebab-case then guarantees a syntax error: the dashes read as subtraction and a segment such as `default` is a
 * Java keyword.
 *
 * A `@Value("${a.b.cD}")` usage does **not** reproduce it: there the delimiters are present, the quotes survive, and
 * the parser receives a valid string literal. Fixtures built on `@Value` pass against the broken code and prove
 * nothing.
 */
class YamlKeyToKebabQuickFixTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_3_1_1,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringYamlInspection::class.java)
    }

    fun testRenamesKeyAndItsUsage() {
        val usageFile = addConditionalOnProperty("Billing", "explyt.billing.accountModeEnabled")
        myFixture.configureByText(
            "application.yaml",
            """
            explyt:
              billing:
                account<caret>ModeEnabled: true
            """.trimIndent()
        )

        myFixture.launchAction(myFixture.findSingleIntention(FIX_NAME))

        myFixture.checkResult(
            """
            explyt:
              billing:
                account-mode-enabled: true
            """.trimIndent()
        )
        assertUsageRenamedTo(usageFile, "explyt.billing.account-mode-enabled")
    }

    /**
     * A digit never starts a new word in Spring's convention, so the `v4` ancestor segment must survive untouched
     * while the flagged key is rewritten.
     */
    fun testRenamesKeyWithDigitBoundaryAndItsUsage() {
        val usageFile = addConditionalOnProperty("Digit", "explyt.billing.v4.accountModeEnabled")
        myFixture.configureByText(
            "application.yaml",
            """
            explyt:
              billing:
                v4:
                  account<caret>ModeEnabled: true
            """.trimIndent()
        )

        myFixture.launchAction(myFixture.findSingleIntention(FIX_NAME))

        myFixture.checkResult(
            """
            explyt:
              billing:
                v4:
                  account-mode-enabled: true
            """.trimIndent()
        )
        assertUsageRenamedTo(usageFile, "explyt.billing.v4.account-mode-enabled")
    }

    /**
     * `defaultRpm` kebab-cases into `default-rpm`, whose first segment is a Java keyword. This case has no digit
     * boundary at all, which is what proves the expression parser — not the dash position — was the problem.
     *
     * The caret sits on `rateLimit` because that is the first non-canonical segment and therefore where the problem
     * is reported; the fix still receives the leaf and renames every segment on the way up.
     */
    fun testRenamesKeyWhoseKebabFormStartsWithAJavaKeyword() {
        val usageFile = addConditionalOnProperty("Rate", "explyt.rateLimit.defaultRpm")
        myFixture.configureByText(
            "application.yaml",
            """
            explyt:
              rate<caret>Limit:
                defaultRpm: 60
            """.trimIndent()
        )

        myFixture.launchAction(myFixture.findSingleIntention(FIX_NAME))

        myFixture.checkResult(
            """
            explyt:
              rate-limit:
                default-rpm: 60
            """.trimIndent()
        )
        assertUsageRenamedTo(usageFile, "explyt.rate-limit.default-rpm")
    }

    /**
     * Batch runs (`Fix all`, `Code | Inspect Code`) invoke the fix without an editor. The whole body used to sit
     * inside `if (editor != null)`, so the fix reported success and changed nothing at all.
     */
    fun testRenamesWithoutAnEditor() {
        val usageFile = addConditionalOnProperty("Batch", "explyt.billing.accountModeEnabled")
        val yaml = myFixture.addFileToProject(
            "application.yaml",
            """
            explyt:
              billing:
                accountModeEnabled: true
            """.trimIndent()
        )

        val keyValue = keyValueOf(yaml, "accountModeEnabled")
        val quickFix = YamlKeyToKebabQuickFix(keyValue)
        WriteCommandAction.runWriteCommandAction(project) {
            quickFix.invoke(project, yaml, null, keyValue, keyValue)
        }

        assertEquals(
            """
            explyt:
              billing:
                account-mode-enabled: true
            """.trimIndent(),
            PsiDocumentManager.getInstance(project).getDocument(yaml)?.text
        )
        assertUsageRenamedTo(usageFile, "explyt.billing.account-mode-enabled")
    }

    /**
     * The reported shape: a key is declared in one place and referenced from another value in the **same**
     * `application.yaml` through a nested placeholder chain `${'$'}{ENV:${'$'}{the.key:false}}`.
     *
     * That chain is what produced `Unexpected token: ':'`. The old code took the key out of the usage with
     * `substringAfter("{").substringBefore("}")`, which on a nested chain returns the *outer* environment variable
     * instead of the referenced key, and then rewrote the whole line by blind `String.replace` — handing the Java
     * expression parser a complete YAML line.
     */
    fun testRenamesKeyReferencedByANestedPlaceholderInTheSameFile() {
        myFixture.configureByText(
            "application.yaml",
            """
            explyt:
              observability:
                tracing:
                  export<caret>_enabled: false

            management:
              otlp:
                tracing:
                  export:
                    enabled: ${'$'}{EXPLYT_TRACING_EXPORT_ENABLED:${'$'}{explyt.observability.tracing.export_enabled:false}}
            """.trimIndent()
        )

        myFixture.launchAction(myFixture.findSingleIntention(FIX_NAME))

        myFixture.checkResult(
            """
            explyt:
              observability:
                tracing:
                  export-enabled: false

            management:
              otlp:
                tracing:
                  export:
                    enabled: ${'$'}{EXPLYT_TRACING_EXPORT_ENABLED:${'$'}{explyt.observability.tracing.export-enabled:false}}
            """.trimIndent()
        )
        val text = myFixture.file.text
        assertFalse("A stale non-kebab spelling survived:\n$text", text.contains("export_enabled"))
    }

    private fun keyValueOf(yaml: PsiFile, keyText: String): YAMLKeyValueImpl =
        PsiTreeUtil.findChildrenOfType(yaml, YAMLKeyValueImpl::class.java)
            .first { it.keyText == keyText }

    private fun addConditionalOnProperty(name: String, key: String): PsiFile = myFixture.addFileToProject(
        "$name.java",
        """
        import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

        @ConditionalOnProperty(name = "$key")
        public class $name {
        }
        """.trimIndent()
    )

    /**
     * The usage rewrite is the part that used to throw, and the YAML file is renamed by a separate code path, so
     * checking only the YAML result stays green even when no usage is touched at all.
     */
    private fun assertUsageRenamedTo(usageFile: PsiFile, expectedKey: String) {
        val text = PsiDocumentManager.getInstance(project).getDocument(usageFile)?.text ?: usageFile.text
        assertTrue("Usage was not rewritten, actual text:\n$text", text.contains("\"$expectedKey\""))
    }

    private companion object {
        val FIX_NAME: String = SpringCoreBundle.message("explyt.spring.inspection.properties.key.yaml.fix.case")
    }
}
