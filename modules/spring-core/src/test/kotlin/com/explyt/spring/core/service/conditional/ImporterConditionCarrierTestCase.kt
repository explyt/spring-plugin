/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.service.conditional.kotlin.KotlinConditionFixtures
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember

abstract class ImporterConditionCarrierTestCase : BeanConditionTestCase() {

    private val kotlin: Boolean get() = fixtures is KotlinConditionFixtures

    fun testImportedClassOfAnInactiveConfigurationIsInactiveWithTheImporterAsCarrier() {
        addProperties("importer.marker=present")
        addImportedService()
        addImporter(GATED_IMPORTER, gated = true)
        assertPropertyUndefined("importer.enabled")
        assertImporterInactive(GATED_IMPORTER)
        assertRegisteredOnlyByImport(IMPORTED_SERVICE)

        val evidence = inactiveEvidence(verdictOf(beanClass(IMPORTED_SERVICE)))

        assertEquals(CONDITIONAL_ON_PROPERTY, evidence.annotationFqn)
        assertEquals(GATED_IMPORTER, evidence.carrierFqn)
        assertEquals(ConditionReason.NOT_MATCHED, evidence.reason)
        assertTrue("$IMPORTED_SERVICE must be excluded", IMPORTED_SERVICE in excludedClasses())
        assertFalse("$IMPORTED_SERVICE must not be active", IMPORTED_SERVICE in activeClasses())
    }

    fun testFactoryMethodOfAnImportedClassFollowsItsInactiveImporter() {
        addProperties("importer.marker=present")
        addImportedService()
        addImporter(GATED_IMPORTER, gated = true)
        val factory = beanClass(IMPORTED_SERVICE).findMethodsByName("importedTool", false).single()
        assertPropertyUndefined("importer.enabled")
        assertImporterInactive(GATED_IMPORTER)
        assertRegisteredOnlyByImport(IMPORTED_SERVICE)
        assertAnnotatedBy(factory, SpringCoreClasses.BEAN)

        val evidence = inactiveEvidence(verdictOf(factory))

        assertEquals(CONDITIONAL_ON_PROPERTY, evidence.annotationFqn)
        assertEquals(GATED_IMPORTER, evidence.carrierFqn)
    }

    fun testClassImportedByAnInactiveAndAnActiveConfigurationIsActive() {
        addProperties("importer.marker=present")
        addImportedService()
        addImporter(GATED_IMPORTER, gated = true)
        addImporter(ACTIVE_IMPORTER, gated = false)
        assertPropertyUndefined("importer.enabled")
        assertImporterInactive(GATED_IMPORTER)
        assertEquals("Precondition: $ACTIVE_IMPORTER is active", ConditionVerdict.Active, verdictOf(beanClass(ACTIVE_IMPORTER)))
        assertRegisteredOnlyByImport(IMPORTED_SERVICE)

        assertEquals(ConditionVerdict.Active, verdictOf(beanClass(IMPORTED_SERVICE)))
        assertTrue("$IMPORTED_SERVICE must be active", IMPORTED_SERVICE in activeClasses())
    }

    fun testClassImportedByAnActiveConfigurationIsActive() {
        addImportedService()
        addImporter(ACTIVE_IMPORTER, gated = false)
        assertEquals("Precondition: $ACTIVE_IMPORTER is active", ConditionVerdict.Active, verdictOf(beanClass(ACTIVE_IMPORTER)))
        assertRegisteredOnlyByImport(IMPORTED_SERVICE)

        assertEquals(ConditionVerdict.Active, verdictOf(beanClass(IMPORTED_SERVICE)))
        assertTrue("$IMPORTED_SERVICE must be active", IMPORTED_SERVICE in activeClasses())
    }

    fun testClassImportedByAnUndecidedConfigurationIsUndecided() {
        addProperties("importer.enabled=\${IMPORTER_ENABLED}")
        addImportedService()
        addImporter(GATED_IMPORTER, gated = true)
        assertPropertyDefined("importer.enabled")
        assertPropertyUndefined("IMPORTER_ENABLED")
        val importerEvidence = undecidedEvidence(verdictOf(beanClass(GATED_IMPORTER))).single()
        assertEquals("Precondition: the importer is undecided", ConditionReason.PROPERTY_UNRESOLVABLE, importerEvidence.reason)
        assertRegisteredOnlyByImport(IMPORTED_SERVICE)

        val evidence = undecidedEvidence(verdictOf(beanClass(IMPORTED_SERVICE))).single()

        assertEquals(CONDITIONAL_ON_PROPERTY, evidence.annotationFqn)
        assertEquals(GATED_IMPORTER, evidence.carrierFqn)
        assertEquals(ConditionReason.PROPERTY_UNRESOLVABLE, evidence.reason)
    }

    fun testPropertiesEnabledByAnInactiveConfigurationAreInactiveWithTheEnablerAsCarrier() {
        addProperties("importer.marker=present")
        addImportedProperties()
        addPropertiesEnabler(GATED_ENABLER, gated = true)
        assertPropertyUndefined("importer.enabled")
        assertImporterInactive(GATED_ENABLER)
        assertAnnotatedBy(beanClass(GATED_ENABLER), SpringCoreClasses.ENABLE_CONFIGURATION_PROPERTIES)
        assertNotScanned(IMPORTED_PROPERTIES)

        val evidence = inactiveEvidence(verdictOf(beanClass(IMPORTED_PROPERTIES)))

        assertEquals(CONDITIONAL_ON_PROPERTY, evidence.annotationFqn)
        assertEquals(GATED_ENABLER, evidence.carrierFqn)
        assertEquals(ConditionReason.NOT_MATCHED, evidence.reason)
        assertFalse("$IMPORTED_PROPERTIES must not be active", IMPORTED_PROPERTIES in activeClasses())
    }

    fun testPropertiesEnabledByAnActiveConfigurationAreActive() {
        addImportedProperties()
        addPropertiesEnabler(ACTIVE_ENABLER, gated = false)
        assertEquals("Precondition: $ACTIVE_ENABLER is active", ConditionVerdict.Active, verdictOf(beanClass(ACTIVE_ENABLER)))
        assertAnnotatedBy(beanClass(ACTIVE_ENABLER), SpringCoreClasses.ENABLE_CONFIGURATION_PROPERTIES)
        assertNotScanned(IMPORTED_PROPERTIES)

        assertEquals(ConditionVerdict.Active, verdictOf(beanClass(IMPORTED_PROPERTIES)))
        assertTrue("$IMPORTED_PROPERTIES must be active", IMPORTED_PROPERTIES in activeClasses())
    }

    private fun addImportedService() {
        addSource(
            "com.imported", "ImportedTool",
            "public class ImportedTool {}",
            "class ImportedTool"
        )
        addSource(
            "com.imported", "ImportedService",
            "public class ImportedService { @Bean public ImportedTool importedTool() { return new ImportedTool(); } }",
            "class ImportedService { @Bean fun importedTool(): ImportedTool = ImportedTool() }"
        )
    }

    private fun addImportedProperties() = addSource(
        "com.imported", "ImportedProperties",
        "@ConfigurationProperties(\"imported\") public class ImportedProperties { private String name; " +
            "public String getName() { return name; } public void setName(String name) { this.name = name; } }",
        "@ConfigurationProperties(\"imported\") class ImportedProperties { var name: String? = null }"
    )

    private fun addImporter(qualifiedName: String, gated: Boolean) {
        val name = qualifiedName.substringAfterLast('.')
        addSource(
            "com.app", name,
            "@Configuration ${javaGate(gated)} @Import(ImportedService.class) public class $name {}",
            "@Configuration ${kotlinGate(gated)} @Import(ImportedService::class) class $name"
        )
    }

    private fun addPropertiesEnabler(qualifiedName: String, gated: Boolean) {
        val name = qualifiedName.substringAfterLast('.')
        addSource(
            "com.app", name,
            "@Configuration ${javaGate(gated)} @EnableConfigurationProperties(ImportedProperties.class) public class $name {}",
            "@Configuration ${kotlinGate(gated)} @EnableConfigurationProperties(ImportedProperties::class) class $name"
        )
    }

    private fun javaGate(gated: Boolean) =
        if (gated) "@ConditionalOnProperty(name = \"importer.enabled\", havingValue = \"true\")" else ""

    private fun kotlinGate(gated: Boolean) =
        if (gated) "@ConditionalOnProperty(name = [\"importer.enabled\"], havingValue = \"true\")" else ""

    private fun addSource(packageName: String, name: String, javaDeclaration: String, kotlinDeclaration: String) {
        val imports = listOf(
            "org.springframework.boot.autoconfigure.condition.*",
            "org.springframework.boot.context.properties.*",
            "org.springframework.context.annotation.*",
            "com.imported.*"
        )
        val source = if (kotlin) {
            "package $packageName\n${imports.joinToString("\n") { "import $it" }}\n$kotlinDeclaration"
        } else {
            "package $packageName;\n${imports.joinToString("\n") { "import $it;" }}\n$javaDeclaration"
        }
        val path = "${packageName.replace('.', '/')}/$name.${if (kotlin) "kt" else "java"}"
        myFixture.addFileToProject(path, source)
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }

    private fun assertImporterInactive(qualifiedName: String) {
        val importer = beanClass(qualifiedName)
        assertAnnotatedBy(importer, CONDITIONAL_ON_PROPERTY)
        val evidence = inactiveEvidence(verdictOf(importer))
        assertEquals("Precondition: $qualifiedName is inactive by its own condition", qualifiedName, evidence.carrierFqn)
    }

    private fun assertRegisteredOnlyByImport(qualifiedName: String) {
        assertNotScanned(qualifiedName)
        assertTrue(
            "Precondition: $qualifiedName is reached through @Import",
            qualifiedName in PackageScanService.getInstance(project).getAllPackages().importQualified
        )
    }

    private fun assertNotScanned(qualifiedName: String) {
        val psiClass = beanClass(qualifiedName)
        assertFalse(
            "Precondition: $qualifiedName carries no stereotype",
            AnnotationUtil.isAnnotated(psiClass, SpringCoreClasses.COMPONENT, AnnotationUtil.CHECK_HIERARCHY)
        )
        val packageName = qualifiedName.substringBeforeLast('.')
        val scanned = PackageScanService.getInstance(project).getAllPackages().getPackages(module)
        assertTrue("Precondition: the application scans packages, got $scanned", scanned.isNotEmpty())
        assertFalse(
            "Precondition: no scanned package $scanned covers $packageName",
            scanned.any { packageName == it || packageName.startsWith("$it.") }
        )
    }

    private fun activeClasses(): Set<String> {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        return classRegistrations(SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module))
    }

    private fun excludedClasses(): Set<String> {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        return classRegistrations(SpringSearchServiceFacade.getInstance(project).getExcludedBeansClasses(module))
    }

    private fun classRegistrations(beans: Collection<com.explyt.spring.core.service.PsiBean>): Set<String> =
        beans.filter { it.psiMember is PsiClass }.mapNotNullTo(sortedSetOf()) { it.psiClass.qualifiedName }

    private fun verdictOf(member: PsiMember): ConditionVerdict? {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        return SpringSearchService.getInstance(project).conditionVerdictOf(member, module)
    }

    private fun inactiveEvidence(verdict: ConditionVerdict?): ConditionEvidence {
        assertInstanceOf(verdict, ConditionVerdict.Inactive::class.java)
        return (verdict as ConditionVerdict.Inactive).condition
    }

    private fun undecidedEvidence(verdict: ConditionVerdict?): List<ConditionEvidence> {
        assertInstanceOf(verdict, ConditionVerdict.Undecided::class.java)
        return (verdict as ConditionVerdict.Undecided).conditions
    }

    private companion object {
        const val IMPORTED_SERVICE = "com.imported.ImportedService"
        const val IMPORTED_PROPERTIES = "com.imported.ImportedProperties"
        const val GATED_IMPORTER = "com.app.GatedImporter"
        const val ACTIVE_IMPORTER = "com.app.ActiveImporter"
        const val GATED_ENABLER = "com.app.GatedPropertiesEnabler"
        const val ACTIVE_ENABLER = "com.app.ActivePropertiesEnabler"
    }
}

abstract class Boot4ImporterConditionCarrierTestCase : ImporterConditionCarrierTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_4_1_0)
}
