/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.reference.java

import com.explyt.spring.core.properties.providers.ConfigKeyPsiElement
import com.explyt.spring.core.properties.references.HibernateSettingsMapKeyTargets
import com.explyt.spring.core.properties.references.PropertiesKeyMapValueReference
import com.explyt.spring.core.properties.references.YamlKeyMapValueReference
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.DumbModeTestUtils
import org.jetbrains.yaml.psi.YAMLKeyValue

abstract class ScalarMapKeyReferenceFixture : ExplytJavaLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + "reference/properties"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBoot_3_1_1,
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1,
    )

    protected fun configureProperties(text: String) {
        myFixture.configureByText("application.properties", text)
    }

    protected fun mapReferenceAtCaret(): PsiPolyVariantReference {
        val found = file.findReferenceAt(myFixture.caretOffset)
        val references = when (found) {
            is PsiMultiReference -> found.references.toList()
            null -> emptyList()
            else -> listOf(found)
        }.filter { it is PropertiesKeyMapValueReference || it is YamlKeyMapValueReference }
        assertEquals("one map reference must cover the caret", 1, references.size)
        return references.single() as PsiPolyVariantReference
    }

    protected fun resolvedTarget(): PsiElement {
        val results = mapReferenceAtCaret().multiResolve(false).mapNotNull { it.element }
        assertEquals("one map-key navigation target", 1, results.size)
        return results.single()
    }

    protected fun assertField(owner: String, name: String, key: String) {
        val expected = myFixture.findClass(owner).findFieldByName(name, false)
        assertNotNull("fixture must contain $owner.$name", expected)
        assertEquals("fixture constant value", key, expected!!.computeConstantValue())
        mapReferenceAtCaret().multiResolve(false)
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion()
        val target = resolvedTarget()
        val member = if (target is ConfigKeyPsiElement) target.parent else target
        assertTrue("map key must resolve to PsiField $owner.$name, got $member", member is PsiField)
        val field = member as PsiField
        assertEquals("Hibernate field owner", owner, field.containingClass?.qualifiedName)
        assertEquals("Hibernate field name", name, field.name)
        assertEquals("Hibernate setting value", key, field.computeConstantValue())
    }

    protected fun assertMapMember(owner: String, vararg names: String) {
        val target = resolvedTarget()
        val member = (if (target is ConfigKeyPsiElement) target.parent else target) as? PsiMember
        assertNotNull("map key must resolve to its declaring member", member)
        assertEquals("map member owner", owner, member!!.containingClass?.qualifiedName)
        assertTrue("expected map member ${names.toList()}, got ${member.name}", member.name in names)
    }

    protected fun assertJpaPropertiesMember() {
        assertMapMember("org.springframework.boot.autoconfigure.orm.jpa.JpaProperties", "properties", "setProperties")
    }

    protected fun assertSingleMapKeyRange(expectedText: String) {
        val reference = mapReferenceAtCaret()
        val element = reference.element
        val start = element.text.indexOf(expectedText)
        assertTrue("fixture contains the complete map key", start >= 0)
        val expectedRange = TextRange.from(start, expectedText.length)
        val mapKeyReferences = element.references.filterIsInstance<PropertiesKeyMapValueReference>()
            .filter { it.rangeInElement.intersects(expectedRange) }
        assertEquals("scalar map key must have one reference, not one per dotted segment", 1, mapKeyReferences.size)
        assertEquals("whole scalar map key range", expectedRange, reference.rangeInElement)
        assertEquals("whole scalar map key text", expectedText, reference.rangeInElement.substring(element.text))
    }
}

class ScalarMapKeyReferenceTest : ScalarMapKeyReferenceFixture() {
    override val libraries: Array<TestLibrary> = super.libraries + TestLibrary.hibernate_6_6_36

    fun testPropertiesHibernateShowSqlResolvesToJdbcSettings() {
        configureProperties("spring.jpa.properties.hibernate.sh<caret>ow_sql=true")
        assertField("org.hibernate.cfg.JdbcSettings", "SHOW_SQL", "hibernate.show_sql")
    }

    fun testPropertiesHibernateGloballyQuotedIdentifiersResolvesToMappingSettings() {
        configureProperties("spring.jpa.properties.hibernate.globally_quoted_<caret>identifiers=true")
        assertField(
            "org.hibernate.cfg.MappingSettings",
            "GLOBALLY_QUOTED_IDENTIFIERS",
            "hibernate.globally_quoted_identifiers"
        )
    }

    fun testPropertiesHibernateMetadataBuilderContributorResolvesToJpaSettings() {
        configureProperties("spring.jpa.properties.hibernate.metadata_builder_<caret>contributor=example.Contributor")
        assertField(
            "org.hibernate.jpa.boot.spi.JpaSettings",
            "METADATA_BUILDER_CONTRIBUTOR",
            "hibernate.metadata_builder_contributor"
        )
    }

    fun testUnknownPropertiesHibernateKeyFallsBackToJpaPropertiesMember() {
        configureProperties("spring.jpa.properties.hibernate.no_such_<caret>setting=true")
        assertJpaPropertiesMember()
    }

    fun testColdHibernateSettingsLookupOnEdtDefersResolution() {
        configureProperties("spring.jpa.properties.hibernate.sh<caret>ow_sql=true")
        val property = PropertyUtil.configurationProperty(module, "spring.jpa.properties.hibernate.show_sql")!!
        val settings = myFixture.findClass("org.hibernate.cfg.JdbcSettings")
        assertNotNull("fixture must contain Hibernate JdbcSettings", settings)
        assertNotNull("fixture must contain SHOW_SQL", settings.findFieldByName("SHOW_SQL", false))
        assertTrue("cold lookup must run on EDT", ApplicationManager.getApplication().isDispatchThread)

        val provider = HibernateSettingsMapKeyTargets()
        val targets = provider.findTargets(module, property, "hibernate.show_sql")

        assertEquals("cold EDT lookup must defer the settings scan", emptyList<PsiElement>(), targets)
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion()

        assertTrue("warmed lookup must still run on EDT", ApplicationManager.getApplication().isDispatchThread)
        val expected = settings.findFieldByName("SHOW_SQL", false)!!
        assertEquals("hibernate.show_sql", expected.computeConstantValue())
        assertEquals(listOf(expected), provider.findTargets(module, property, "hibernate.show_sql"))
    }

    fun testHibernateSettingsTargetsAreEmptyInDumbMode() {
        configureProperties("spring.jpa.properties.hibernate.sh<caret>ow_sql=true")
        val property = PropertyUtil.configurationProperty(module, "spring.jpa.properties.hibernate.show_sql")
        assertNotNull("fixture must register spring.jpa.properties", property)
        val expected = myFixture.findClass("org.hibernate.cfg.JdbcSettings").findFieldByName("SHOW_SQL", false)
        assertNotNull("fixture must contain Hibernate SHOW_SQL", expected)
        val provider = HibernateSettingsMapKeyTargets()

        DumbModeTestUtils.runInDumbModeSynchronously(project) {
            val targets = provider.findTargets(module, property!!, "hibernate.show_sql")
            assertTrue("Hibernate targets must not be resolved in dumb mode", targets.isEmpty())
        }

        provider.findTargets(module, property!!, "hibernate.show_sql")
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion()
        assertEquals(listOf(expected), provider.findTargets(module, property, "hibernate.show_sql"))
    }

    fun testUnrelatedEditDoesNotRebuildHibernateTargets() {
        configureProperties("spring.jpa.properties.hibernate.sh<caret>ow_sql=true")
        val property = PropertyUtil.configurationProperty(module, "spring.jpa.properties.hibernate.show_sql")!!
        val provider = HibernateSettingsMapKeyTargets()
        provider.findTargets(module, property, "hibernate.show_sql")
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion()
        val targets = provider.findTargets(module, property, "hibernate.show_sql")
        assertEquals(1, targets.size)
        val unrelated = myFixture.addFileToProject("Unrelated.java", "class Unrelated { int count; }")
        val beforeEdit = provider.findTargets(module, property, "hibernate.show_sql")
        WriteCommandAction.runWriteCommandAction(project) {
            val document = com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(unrelated)!!
            document.setText("class Unrelated { int total; }")
            com.intellij.psi.PsiDocumentManager.getInstance(project).commitDocument(document)
        }
        val afterEdit = provider.findTargets(module, property, "hibernate.show_sql")
        assertEquals(1, afterEdit.size)
        assertSame("unrelated edits must preserve the settings index", beforeEdit, afterEdit)
    }

    fun testPropertiesCaretInMapPrefixResolvesToJpaPropertiesMember() {
        configureProperties("spring.jpa.prop<caret>erties.hibernate.show_sql=true")
        assertJpaPropertiesMember()
    }

    fun testBracketPropertiesHibernateKeyResolvesToJdbcSettings() {
        configureProperties("spring.jpa.properties[hibernate.show_<caret>sql]=true")
        assertField("org.hibernate.cfg.JdbcSettings", "SHOW_SQL", "hibernate.show_sql")
    }

    fun testPropertiesScalarMapKeyIsOneReferenceRange() {
        configureProperties("spring.jpa.properties.hibernate.<caret>show_sql=true")
        assertSingleMapKeyRange("hibernate.show_sql")
    }

    fun testYamlNestedScalarMapKeyResolvesToJdbcSettings() {
        myFixture.configureByText(
            "application.yaml",
            """
            spring:
              jpa:
                properties:
                  hibernate:
                    sh<caret>ow_sql: true
            """.trimIndent()
        )
        assertField("org.hibernate.cfg.JdbcSettings", "SHOW_SQL", "hibernate.show_sql")
    }

    fun testYamlNestedMapPrefixResolvesToJpaPropertiesMember() {
        myFixture.configureByText(
            "application.yaml",
            """
            spring:
              jpa:
                properties:
                  hiber<caret>nate:
                    show_sql: true
            """.trimIndent()
        )
        assertJpaPropertiesMember()
    }

    fun testYamlFlatScalarMapKeyResolvesToJdbcSettings() {
        myFixture.configureByText(
            "application.yaml",
            """
            spring:
              jpa:
                properties:
                  hibernate.show_<caret>sql: true
            """.trimIndent()
        )
        assertField("org.hibernate.cfg.JdbcSettings", "SHOW_SQL", "hibernate.show_sql")
    }

    fun testYamlQuotedBracketScalarMapKeyResolvesToJdbcSettings() {
        myFixture.configureByText(
            "application.yaml",
            """
            spring:
              jpa:
                properties:
                  "[hibernate.show_<caret>sql]": true
            """.trimIndent()
        )
        assertField("org.hibernate.cfg.JdbcSettings", "SHOW_SQL", "hibernate.show_sql")
    }

    fun testYamlMapReferencesStayWithinKeyText() {
        myFixture.configureByText(
            "application.yaml",
            """
            spring:
              jpa:
                properties:
                  hiber<caret>nate:
                    show_sql: true
            """.trimIndent()
        )
        val keyValue =
            PsiTreeUtil.getParentOfType(file.findElementAt(myFixture.caretOffset), YAMLKeyValue::class.java)!!
        assertEquals("hibernate", keyValue.keyText)
        val references = keyValue.references
        assertTrue("fixture must contribute map references", references.any { it is YamlKeyMapValueReference })
        val keyRange = keyValue.key!!.textRange
        references.forEach { reference ->
            val absoluteRange = reference.rangeInElement.shiftRight(keyValue.textRange.startOffset)
            assertTrue("reference $absoluteRange must stay inside YAML key $keyRange", keyRange.contains(absoluteRange))
        }
    }

    fun testProjectStringMapKeyResolvesAsOneKey() {
        myFixture.copyFileToProject("LabelsProperties.java")
        configureProperties("app.labels.team.<caret>name=x")
        assertSingleMapKeyRange("team.name")
        assertMapMember("src.LabelsProperties", "labels", "setLabels")
    }

    fun testYamlPojoMemberUnderMapKeyResolves() {
        myFixture.copyFileToProject("BracketMapProperties.java")
        myFixture.configureByText(
            "application.yaml",
            """
                app:
                  publishers:
                    my-registration:
                      owner-<caret>application: my-app
            """.trimIndent()
        )

        assertMapMember("src.BracketMapProperties.PublisherProperties", "setOwnerApplication")
    }
}

class ScalarMapKeyReferenceWithoutHibernateTest : ScalarMapKeyReferenceFixture() {
    fun testUnknownPropertiesHibernateKeyFallsBackWithoutHibernateLibrary() {
        assertNull(
            "fixture must not contain Hibernate",
            myFixture.javaFacade.findClass("org.hibernate.cfg.AvailableSettings", module.moduleWithLibrariesScope)
        )
        configureProperties("spring.jpa.properties.hibernate.no_such_<caret>setting=true")
        assertJpaPropertiesMember()
    }
}
