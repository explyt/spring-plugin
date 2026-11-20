/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.inspections.SpringBasePropertyInspection
import com.explyt.spring.core.inspections.SpringPropertiesInspection
import com.explyt.spring.core.inspections.SpringYamlInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl
import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.psi.YAMLAlias
import org.jetbrains.yaml.psi.YAMLAnchor
import org.jetbrains.yaml.psi.YAMLFile
import java.io.StringReader
import java.util.Properties

class SpringPropertyFilesCodeCleanupTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_3_1_1,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringYamlInspection::class.java, SpringPropertiesInspection::class.java)
        myFixture.addClass(
            """
            @org.springframework.context.annotation.Configuration
            @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "explyt.cleanup")
            public class CleanupProperties {
                private String someKey;
                private String stableKey;
                private String sharedKey;
                public String getSharedKey() { return sharedKey; }
                public void setSharedKey(String value) { sharedKey = value; }
                public String getSomeKey() { return someKey; }
                public void setSomeKey(String value) { someKey = value; }
                public String getStableKey() { return stableKey; }
                public void setStableKey(String value) { stableKey = value; }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "META-INF/additional-spring-configuration-metadata.json",
            """
            {
              "properties": [
                {"name":"explyt.cleanup.old-key","type":"java.lang.String",
                 "deprecation":{"level":"warning","replacement":"explyt.cleanup.new-key","reason":"Use new key"}},
                {"name":"explyt.cleanup.new-key","type":"java.lang.String"},
                {"name":"explyt.cleanup.retired-key","type":"java.lang.String",
                 "deprecation":{"level":"warning","reason":"No replacement"}},
                {"name":"explyt.cleanup.duplicate-key","type":"java.lang.String"},
                {"name":"explyt.cleanup.resource","type":"java.lang.String"}
              ],
              "hints": [
                {"name":"explyt.cleanup.resource",
                 "providers":[{"name":"handle-as","parameters":{"target":"org.springframework.core.io.Resource"}}]}
              ]
            }
            """.trimIndent()
        )
    }

    fun testYamlInspectionIsRegisteredForCodeCleanup() = assertCleanupRegistration("SpringYamlInspection")

    fun testPropertiesInspectionIsRegisteredForCodeCleanup() = assertCleanupRegistration("SpringPropertiesInspection")

    fun testCleanupCanonicalizesYamlAndPreservesCanonicalKey() {
        configure(true)
        assertCanonicalWarning()
        cleanup()
        myFixture.checkResult(canonicalText(true))
    }

    fun testCleanupCanonicalizesPropertiesAndPreservesCanonicalKey() {
        configure(false)
        assertCanonicalWarning()
        cleanup()
        myFixture.checkResult(canonicalText(false))
    }

    fun testCleanupKeepsDeprecatedPropertiesKeyWithKnownReplacement() {
        val entry = "explyt.cleanup.old-key=preserved"
        configure(false, entry)
        assertEquals(1, problemsContaining("Deprecated:").size)
        cleanup()
        assertPositiveCleanup(false)
        assertPreservedEntries(false, entry)
    }

    fun testDeprecatedPropertiesReplacementIsInteractiveOnly() {
        configure(false, "explyt.cleanup.<caret>old-key=preserved")
        val replacementName = SpringCoreBundle.message(
            "explyt.spring.inspection.properties.quick.fix.replacement", "explyt.cleanup.new-key"
        )
        assertEquals(replacementName, myFixture.findSingleIntention(replacementName).text)
        val descriptors = problemsContaining("Deprecated:")
        assertEquals(1, descriptors.size)
        assertNotBatchSafe(descriptors.single(), "Deprecated key replacement")
    }

    fun testCleanupPreservesPropertiesBracketedMapEntry() = assertMapEntryPreserved(false)

    fun testCleanupPreservesYamlBracketedMapEntry() = assertMapEntryPreserved(true)

    fun testCleanupPreservesPropertiesLoggingLevelMapEntry() = assertLoggingEntryPreserved(false)

    fun testCleanupPreservesYamlLoggingLevelMapEntry() = assertLoggingEntryPreserved(true)

    fun testCleanupPreservesUnknownPropertiesKey() = assertUnknownKeyPreserved(false)

    fun testCleanupPreservesUnknownYamlKey() = assertUnknownKeyPreserved(true)

    fun testCleanupPreservesCollidingPropertiesKeys() = assertCollisionPreserved(false)

    fun testCleanupPreservesCollidingYamlKeys() = assertCollisionPreserved(true)

    fun testCleanupPreservesPropertiesBooleanAlias() = assertBooleanAliasPreserved(false)

    fun testCleanupPreservesYamlBooleanAlias() = assertBooleanAliasPreserved(true)

    fun testCleanupPreservesYamlKeyCollidingWithBlockScalarInAnotherDocument() {
        val entries = """
            spring.application.Name: first
            ---
            spring.application.name: >-
              second
        """.trimIndent()
        configure(true, entries)
        assertEquals("Expected two YAML documents in one file", 2, (myFixture.file as YAMLFile).documents.size)
        cleanup()
        assertPositiveCleanup(true)
        assertPreservedEntries(true, entries)
    }

    fun testCleanupPreservesPropertiesKeyCollidingWithUnicodeEscapedKey() {
        addServerProperties()
        val entries = """
            explyt.server.maxSize=first
            explyt.server.max\u002dsize=second
        """.trimIndent()
        val properties = Properties().apply { load(StringReader(entries)) }
        assertEquals(setOf("explyt.server.maxSize", "explyt.server.max-size"), properties.stringPropertyNames())
        configure(false, entries)
        cleanup()
        assertPositiveCleanup(false)
        assertPreservedEntries(false, entries)
    }

    fun testCleanupPreservesYamlMappingAliasedAsLoggingGroup() {
        val entries = """
            spring.application: &settings
              Name: first
            logging:
              group: *settings
        """.trimIndent()
        configure(true, entries)
        val anchor = PsiTreeUtil.findChildrenOfType(myFixture.file, YAMLAnchor::class.java).single()
        val alias = PsiTreeUtil.findChildrenOfType(myFixture.file, YAMLAlias::class.java).single()
        assertEquals("The logging group alias must resolve to the settings anchor", anchor, alias.reference?.resolve())
        cleanup()
        assertPositiveCleanup(true)
        assertPreservedEntries(true, entries)
    }

    fun testCleanupDoesNotRenamePropertiesKeyAlsoDeclaredInProfileYaml() {
        val key = "explyt.cleanup.sharedKey"
        val entry = "$key=base"
        val profileText = """
            explyt:
              cleanup:
                sharedKey: dev
            """.trimIndent()
        val profileFile = myFixture.addFileToProject("application-dev.yml", profileText)
        configure(false, entry)
        val definitions = DefinedConfigurationPropertiesSearch.getInstance(project).findProperties(module, key)
        assertEquals(
            "Both definitions must be discoverable by the rename search",
            setOf(myFixture.file.virtualFile, profileFile.virtualFile),
            definitions.mapNotNull { it.psiElement?.containingFile?.virtualFile }.toSet()
        )
        cleanup()
        assertPositiveCleanup(false)
        assertEquals("Code Cleanup edited application-dev.yml", profileText, profileFile.text)
        assertPreservedEntries(false, entry)
    }

    fun testCleanupDoesNotCreateMissingPropertiesResource() = assertMissingResourceIsKeptMissing(false)

    fun testCleanupDoesNotCreateMissingYamlResource() = assertMissingResourceIsKeptMissing(true)

    fun testCleanupPreservesDeprecatedPropertiesKeyWithoutReplacement() = assertRetiredKeyIsPreserved(false)

    fun testCleanupPreservesDeprecatedYamlKeyWithoutReplacement() = assertRetiredKeyIsPreserved(true)

    fun testCleanupPreservesAllRelaxedBindingPropertiesDuplicates() {
        val duplicates = "explyt.cleanup.duplicate-key=first\nexplyt.cleanup.duplicatekey=second"
        configure(false, duplicates)
        assertEquals(2, problemsContaining("Duplicate").size)
        cleanup()
        assertTrue("First duplicate was deleted", myFixture.file.text.contains("explyt.cleanup.duplicate-key=first"))
        assertTrue("Second duplicate was deleted", myFixture.file.text.contains("explyt.cleanup.duplicatekey=second"))
        assertPositiveCleanup(false)
    }

    fun testCleanupPreservesAllRelaxedBindingYamlDuplicates() {
        configure(true, "explyt.cleanup.duplicate-key: first\nexplyt.cleanup.duplicatekey: second")
        assertEquals(2, problemsContaining("Duplicate").size)
        cleanup()
        assertTrue("First duplicate was deleted", myFixture.file.text.contains("explyt.cleanup.duplicate-key: first"))
        assertTrue("Second duplicate was deleted", myFixture.file.text.contains("explyt.cleanup.duplicatekey: second"))
        assertPositiveCleanup(true)
    }

    fun testKnownLimitationCleanupDoesNotReplaceDeprecatedYamlKey() {
        configure(true, "explyt.cleanup.old-key: preserved")
        assertEquals(1, problemsContaining("Deprecated:").size)
        cleanup()
        assertTrue("Known YAML limitation changed", myFixture.file.text.contains("explyt.cleanup.old-key: preserved"))
        assertFalse(myFixture.file.text.contains("explyt.cleanup.new-key:"))
        assertPositiveCleanup(true)
    }

    fun testMissingPropertiesResourceCreationFixIsNotAvailableInBatchMode() = assertResourceFixIsNotBatchSafe(false)

    fun testMissingYamlResourceCreationFixIsNotAvailableInBatchMode() = assertResourceFixIsNotBatchSafe(true)

    fun testDeprecatedPropertiesKeyWithoutReplacementFixIsNotAvailableInBatchMode() = assertRetiredFixIsNotBatchSafe(false)

    fun testDeprecatedYamlKeyWithoutReplacementFixIsNotAvailableInBatchMode() = assertRetiredFixIsNotBatchSafe(true)

    fun testPropertiesDuplicateDeletionFixesAreNotAvailableInBatchMode() {
        configure(false, "explyt.cleanup.duplicate-key=first\nexplyt.cleanup.duplicatekey=second")
        val descriptors = problemsContaining("Duplicate")
        assertEquals(2, descriptors.size)
        descriptors.forEach { assertNotBatchSafe(it, "Duplicate deletion") }
    }

    private fun assertMapEntryPreserved(yaml: Boolean) {
        myFixture.addClass(
            """
            @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "explyt.binding")
            public class MapProperties {
                private java.util.Map<String, String> properties;
                public java.util.Map<String, String> getProperties() { return properties; }
                public void setProperties(java.util.Map<String, String> value) { properties = value; }
            }
            """.trimIndent()
        )
        val entry = if (yaml) "explyt.binding:\n  Properties[show_sql]: true"
        else "explyt.binding.Properties[show_sql]=true"
        configure(yaml, entry)
        cleanup()
        assertPositiveCleanup(yaml)
        assertPreservedEntries(yaml, entry)
    }

    private fun assertLoggingEntryPreserved(yaml: Boolean) {
        val entry = if (yaml) "Logging.level.com.Example: DEBUG" else "Logging.level.com.Example=DEBUG"
        configure(yaml, entry)
        cleanup()
        assertPositiveCleanup(yaml)
        assertPreservedEntries(yaml, entry)
    }

    private fun assertUnknownKeyPreserved(yaml: Boolean) {
        val entry = if (yaml) "explyt.featureFlag: true" else "explyt.featureFlag=true"
        configure(yaml, entry)
        val unresolved = problemsContaining("Cannot resolve key property")
        assertTrue("The fixture key must be unresolved", unresolved.any { "explyt.featureFlag" in it.descriptionTemplate })
        cleanup()
        assertPositiveCleanup(yaml)
        assertPreservedEntries(yaml, entry)
    }

    private fun assertBooleanAliasPreserved(yaml: Boolean) {
        val entry = if (yaml) "spring.main.is-lazy_Initialization: true" else "spring.main.is-lazy_Initialization=true"
        configure(yaml, entry)
        cleanup()
        assertPositiveCleanup(yaml)
        assertPreservedEntries(yaml, entry)
    }

    private fun assertCollisionPreserved(yaml: Boolean) {
        addServerProperties()
        val entries = if (yaml) "explyt.server:\n  maxSize: 10\n  max-size: 20"
        else "explyt.server.maxSize=10\nexplyt.server.max-size=20"
        configure(yaml, entries)
        assertEquals("Both aliases must be reported as duplicates", 2, problemsContaining("Duplicate").size)
        cleanup()
        assertPositiveCleanup(yaml)
        assertPreservedEntries(yaml, entries)
    }

    private fun addServerProperties() {
        myFixture.addClass(
            """
            @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "explyt.server")
            public class ServerProperties {
                private int maxSize;
                public int getMaxSize() { return maxSize; }
                public void setMaxSize(int value) { maxSize = value; }
            }
            """.trimIndent()
        )
    }

    private fun assertPreservedEntries(yaml: Boolean, entries: String) {
        assertEquals("Code Cleanup changed protected entries", canonicalText(yaml) + "\n" + entries, myFixture.file.text)
    }

    private fun assertCleanupRegistration(shortName: String) {
        val wrapper = registeredWrapper(shortName)
        assertTrue("$shortName must be a Code Cleanup tool", wrapper.isCleanupTool)
    }

    private fun registeredWrapper(shortName: String): LocalInspectionToolWrapper {
        val extension = LocalInspectionEP.LOCAL_INSPECTION.extensionList.singleOrNull { it.shortName == shortName }
        assertNotNull("Registered inspection wrapper missing: $shortName", extension)
        return LocalInspectionToolWrapper(extension!!)
    }

    private fun configure(yaml: Boolean, extra: String = "") {
        myFixture.configureByText(
            if (yaml) "application.yml" else "application.properties",
            initialText(yaml) + if (extra.isEmpty()) "" else "\n$extra"
        )
    }

    private fun initialText(yaml: Boolean): String = if (yaml) {
        """
        explyt.cleanup:
          someKey: normalized
          stable-key: untouched
        """.trimIndent()
    } else {
        "explyt.cleanup.someKey=normalized\nexplyt.cleanup.stable-key=untouched"
    }

    private fun canonicalText(yaml: Boolean) = initialText(yaml).replace("someKey", "some-key")

    private fun assertCanonicalWarning() {
        val message = SpringCoreBundle.message("explyt.spring.inspection.properties.value.should.be.kebab")
        val text = myFixture.file.text
        val highlights = myFixture.doHighlighting().filter {
            it.description == message && text.substring(it.startOffset, it.endOffset) == "someKey"
        }
        assertEquals("Expected a canonical-form warning on the positive control", 1, highlights.size)
        assertEquals(HighlightSeverity.WEAK_WARNING, highlights.single().severity)
    }

    private fun cleanup() {
        assertCanonicalWarning()
        val profile = InspectionProfileImpl("Spring property file cleanup")
        profile.disableAllTools(project)
        val wrapper = registeredWrapper(inspection().javaClass.simpleName)
        profile.addTool(project, wrapper, null)
        profile.enableTool(wrapper.shortName, project)
        val context = InspectionManager.getInstance(project).createNewGlobalContext() as GlobalInspectionContextImpl
        var finished = false
        try {
            context.codeCleanup(AnalysisScope(myFixture.file), profile, null, { finished = true }, true) { true }
            assertTrue("Code Cleanup did not finish", finished)
            PsiDocumentManager.getInstance(project).commitAllDocuments()
        } finally {
            context.cleanup()
        }
    }

    private fun inspection(): SpringBasePropertyInspection =
        if (myFixture.file.name.endsWith(".yml")) SpringYamlInspection() else SpringPropertiesInspection()

    private fun problemsContaining(message: String): List<ProblemDescriptor> =
        inspection().checkFile(myFixture.file, InspectionManager.getInstance(project), true)
            .orEmpty().filter { it.descriptionTemplate.contains(message, ignoreCase = true) }

    private fun assertPositiveCleanup(yaml: Boolean) {
        val text = myFixture.file.text
        assertTrue("Code Cleanup did not canonicalize the positive control:\n$text", text.startsWith(canonicalText(yaml)))
        assertFalse("Non-canonical positive control survived", text.contains("someKey"))
    }

    private fun assertMissingResourceIsKeptMissing(yaml: Boolean) {
        configure(yaml, resourceEntry(yaml))
        assertCanonicalWarning()
        assertEquals("Missing-resource inspection did not run", 1, problemsContaining("Cannot resolve file").size)
        assertMissingResource()
        cleanup()
        assertMissingResource()
        assertTrue(myFixture.file.text.contains(resourceEntry(yaml)))
        assertPositiveCleanup(yaml)
    }

    private fun assertMissingResource() {
        val roots = ProjectRootManager.getInstance(project).contentRoots
        assertTrue("A project content root is required", roots.isNotEmpty())
        roots.forEach { root ->
            VfsUtilCore.iterateChildrenRecursively(root, null) { file ->
                assertFalse("Cleanup created a missing classpath resource: ${file.path}", file.name == "explyt-missing.properties")
                true
            }
        }
    }

    private fun resourceEntry(yaml: Boolean) =
        if (yaml) "explyt.cleanup.resource: classpath:explyt-missing.properties"
        else "explyt.cleanup.resource=classpath:explyt-missing.properties"

    private fun assertRetiredKeyIsPreserved(yaml: Boolean) {
        val entry = if (yaml) "explyt.cleanup.retired-key: preserved" else "explyt.cleanup.retired-key=preserved"
        configure(yaml, entry)
        assertCanonicalWarning()
        assertEquals(1, problemsContaining("Deprecated:").size)
        cleanup()
        assertTrue("Deprecated key without replacement was changed", myFixture.file.text.contains(entry))
        assertPositiveCleanup(yaml)
    }

    private fun assertResourceFixIsNotBatchSafe(yaml: Boolean) {
        configure(yaml, resourceEntry(yaml))
        val descriptors = problemsContaining("Cannot resolve file")
        assertEquals("Missing-resource inspection did not run", 1, descriptors.size)
        assertNotBatchSafe(descriptors.single(), "Resource creation")
    }

    private fun assertRetiredFixIsNotBatchSafe(yaml: Boolean) {
        configure(yaml, if (yaml) "explyt.cleanup.retired-key: preserved" else "explyt.cleanup.retired-key=preserved")
        val descriptors = problemsContaining("Deprecated:")
        assertEquals(1, descriptors.size)
        assertNotBatchSafe(descriptors.single(), "Deprecated key without replacement")
    }

    private fun assertNotBatchSafe(descriptor: ProblemDescriptor, label: String) {
        val fixes = descriptor.fixes.orEmpty()
        assertEquals("$label must offer one interactive fix", 1, fixes.size)
        val fix = fixes.single()
        assertTrue("$label must be a local quick fix", fix is LocalQuickFix)
        assertFalse("$label must not be available in batch mode", (fix as LocalQuickFix).availableInBatchMode())
    }
}
