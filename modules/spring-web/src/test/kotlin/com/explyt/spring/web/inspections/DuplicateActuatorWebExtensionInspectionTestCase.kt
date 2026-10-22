/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections

import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.conditional.ConditionVerdict
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytBaseLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.ActuatorWebExtensions
import com.explyt.spring.web.loader.EndpointAccess
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointExposure
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.SpringWebEndpointsLoader
import com.explyt.spring.web.util.WebApplicationStack
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType

abstract class DuplicateActuatorWebExtensionInspectionTestCase : ExplytBaseLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootActuatorAutoConfigure_4_1_0,
        TestLibrary.springBootHealth_4_1_0,
        TestLibrary.springWebMvc_6_0_7
    )

    protected abstract val fileExtension: String
    protected abstract fun application(): String
    protected abstract fun header(): String
    protected abstract fun endpoint(id: String, className: String): String
    protected abstract fun extension(className: String, endpointClass: String, bean: Boolean = true, gated: Boolean = false): String
    protected abstract fun composedAnnotation(name: String, endpointClass: String): String
    protected abstract fun composedExtension(className: String, annotation: String): String
    protected abstract fun beanSubclass(className: String, baseClass: String): String

    private var testSourceRoot: VirtualFile? = null

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject("com/app/DemoApplication.$fileExtension", application())
    }

    override fun tearDown() {
        try {
            testSourceRoot?.let { PsiTestUtil.removeSourceRoot(module, it) }
        } catch (e: Throwable) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
    }

    fun testTwoActiveExtensionBeansOfAnEndpointExposedByWildcardAreErrors() {
        addProperties(EXPOSE_ALL)
        addFixtures(endpoint("x", "XEndpoint"), extension("XFirstExtension", "XEndpoint"), extension("XSecondExtension", "XEndpoint"))
        assertPreconditions(X_PATH, EndpointExposure.EXPOSED)

        assertCollision(problemsIn(FIXTURES), HighlightSeverity.ERROR, FIRST, SECOND)
    }

    fun testTwoActiveExtensionBeansOfAnEndpointIncludedByIdAreErrors() {
        addProperties("$INCLUDE_KEY=x")
        addFixtures(endpoint("x", "XEndpoint"), extension("XFirstExtension", "XEndpoint"), extension("XSecondExtension", "XEndpoint"))
        assertPreconditions(X_PATH, EndpointExposure.EXPOSED)

        assertCollision(problemsIn(FIXTURES), HighlightSeverity.ERROR, FIRST, SECOND)
    }

    fun testTwoActiveExtensionsMakeTheCollisionAnErrorEvenBesideAnUndecidedOne() {
        addProperties(EXPOSE_ALL, "$EXT_GATING_KEY=\${EXT_ENABLED}")
        addFixtures(
            endpoint("x", "XEndpoint"),
            extension("XFirstExtension", "XEndpoint"),
            extension("XSecondExtension", "XEndpoint"),
            extension("XThirdExtension", "XEndpoint", gated = true)
        )
        assertPreconditions(X_PATH, EndpointExposure.EXPOSED)
        assertBeanModelSays<ConditionVerdict.Active>(FIRST)
        assertBeanModelSays<ConditionVerdict.Active>(SECOND)
        assertBeanModelSays<ConditionVerdict.Undecided>(THIRD)

        assertCollision(problemsIn(FIXTURES), HighlightSeverity.ERROR, FIRST, SECOND, THIRD)
    }

    fun testUndecidedExtensionMakesTheCollisionAWarning() {
        addProperties(EXPOSE_ALL, "$EXT_GATING_KEY=\${EXT_ENABLED}")
        addFixtures(
            endpoint("x", "XEndpoint"),
            extension("XFirstExtension", "XEndpoint"),
            extension("XSecondExtension", "XEndpoint", gated = true)
        )
        assertPreconditions(X_PATH, EndpointExposure.EXPOSED)
        assertBeanModelSays<ConditionVerdict.Active>(FIRST)
        assertBeanModelSays<ConditionVerdict.Undecided>(SECOND)

        assertCollision(problemsIn(FIXTURES), HighlightSeverity.WARNING, FIRST, SECOND)
    }

    fun testInactiveExtensionDoesNotCollide() {
        addProperties(EXPOSE_ALL)
        addFixtures(
            endpoint("x", "XEndpoint"),
            extension("XFirstExtension", "XEndpoint"),
            extension("XSecondExtension", "XEndpoint", gated = true),
            *collidingControl()
        )
        assertPreconditions(X_PATH, EndpointExposure.EXPOSED)
        assertPreconditions(Y_PATH, EndpointExposure.EXPOSED)
        assertBeanModelSays<ConditionVerdict.Inactive>(SECOND)

        assertCollision(problemsIn(FIXTURES), HighlightSeverity.ERROR, Y_FIRST, Y_SECOND)
    }

    fun testNonBeanExtensionClassDoesNotCollide() {
        addProperties(EXPOSE_ALL)
        addFixtures(
            endpoint("x", "XEndpoint"),
            extension("XFirstExtension", "XEndpoint"),
            extension("XSecondExtension", "XEndpoint", bean = false),
            *collidingControl()
        )
        assertPreconditions(X_PATH, EndpointExposure.EXPOSED)
        assertPreconditions(Y_PATH, EndpointExposure.EXPOSED)

        assertCollision(problemsIn(FIXTURES), HighlightSeverity.ERROR, Y_FIRST, Y_SECOND)
    }

    fun testBaseAndSubclassExtensionBeansCollide() {
        addProperties(EXPOSE_ALL)
        addFixtures(endpoint("x", "XEndpoint"), extension("XFirstExtension", "XEndpoint"), beanSubclass("XSubExtension", "XFirstExtension"))
        assertPreconditions(X_PATH, EndpointExposure.EXPOSED)

        val problems = problemsIn(FIXTURES)

        assertFalse("the inherited collision is reported", problems.isEmpty())
        assertEquals(listOf(HighlightSeverity.ERROR), problems.map { it.severity }.distinct())
        assertEquals(listOf(FIRST), problems.map(::ownerOf).distinct())
        assertTrue(problems.all { it.text.startsWith(DIRECT_ANCHOR) })
        assertTrue(
            "the message names the subclass: ${problems.map { it.description }}",
            problems.any { it.description.contains(SUB) }
        )
    }

    fun testSubclassBeanOfANonBeanExtensionDoesNotCollide() {
        addProperties(EXPOSE_ALL)
        addFixtures(
            endpoint("x", "XEndpoint"),
            extension("XFirstExtension", "XEndpoint", bean = false),
            beanSubclass("XSubExtension", "XFirstExtension"),
            *collidingControl()
        )
        assertPreconditions(X_PATH, EndpointExposure.EXPOSED)
        assertPreconditions(Y_PATH, EndpointExposure.EXPOSED)

        assertCollision(problemsIn(FIXTURES), HighlightSeverity.ERROR, Y_FIRST, Y_SECOND)
    }

    fun testComposedAndDirectExtensionsCollide() {
        addProperties(EXPOSE_ALL)
        addFixtures(
            endpoint("x", "XEndpoint"),
            composedAnnotation("MyExt", "XEndpoint"),
            composedExtension("XComposedExtension", "MyExt"),
            extension("XFirstExtension", "XEndpoint")
        )
        assertPreconditions(X_PATH, EndpointExposure.EXPOSED)

        assertCollision(
            problemsIn(FIXTURES), HighlightSeverity.ERROR, COMPOSED, FIRST,
            anchors = mapOf(COMPOSED to "@MyExt", FIRST to DIRECT_ANCHOR)
        )
    }

    fun testExtensionsOfDifferentEndpointsDoNotCollide() {
        addProperties(EXPOSE_ALL)
        addFixtures(endpoint("x", "XEndpoint"), extension("XFirstExtension", "XEndpoint"), *collidingControl())
        assertPreconditions(X_PATH, EndpointExposure.EXPOSED)
        assertPreconditions(Y_PATH, EndpointExposure.EXPOSED)

        assertCollision(problemsIn(FIXTURES), HighlightSeverity.ERROR, Y_FIRST, Y_SECOND)
    }

    fun testUnsetExposureLeavesTheCollisionOfAnUnexposedEndpointSilent() {
        addFixtures(
            endpoint("x", "XEndpoint"),
            extension("XFirstExtension", "XEndpoint"),
            extension("XSecondExtension", "XEndpoint"),
            endpoint("health", "HEndpoint"),
            extension("HFirstExtension", "HEndpoint"),
            extension("HSecondExtension", "HEndpoint")
        )
        assertPreconditions(X_PATH, EndpointExposure.NOT_EXPOSED)
        assertPreconditions(HEALTH_PATH, EndpointExposure.EXPOSED)

        assertCollision(problemsIn(FIXTURES), HighlightSeverity.ERROR, H_FIRST, H_SECOND)
    }

    fun testExcludedEndpointLeavesItsCollisionSilent() {
        addProperties(EXPOSE_ALL, "$EXCLUDE_KEY=x")
        addFixtures(
            endpoint("x", "XEndpoint"),
            extension("XFirstExtension", "XEndpoint"),
            extension("XSecondExtension", "XEndpoint"),
            *collidingControl()
        )
        assertPreconditions(X_PATH, EndpointExposure.NOT_EXPOSED)
        assertPreconditions(Y_PATH, EndpointExposure.EXPOSED)

        assertCollision(problemsIn(FIXTURES), HighlightSeverity.ERROR, Y_FIRST, Y_SECOND)
    }

    fun testAccessNoneDoesNotRescueTheCollision() {
        addProperties(EXPOSE_ALL, "management.endpoint.x.access=NONE")
        addFixtures(endpoint("x", "XEndpoint"), extension("XFirstExtension", "XEndpoint"), extension("XSecondExtension", "XEndpoint"))
        assertPreconditions(X_PATH, EndpointExposure.EXPOSED)
        assertEquals(listOf(EndpointAccess.NONE), actuatorEndpointsAt(X_PATH).map { it.access }.distinct())

        assertCollision(problemsIn(FIXTURES), HighlightSeverity.ERROR, FIRST, SECOND)
    }

    fun testUnknownExposureMakesTheCollisionAWarning() {
        addProperties("$INCLUDE_KEY=\${MANAGEMENT_INCLUDE}")
        addFixtures(endpoint("x", "XEndpoint"), extension("XFirstExtension", "XEndpoint"), extension("XSecondExtension", "XEndpoint"))
        assertPreconditions(X_PATH, EndpointExposure.UNKNOWN)

        val problems = problemsIn(FIXTURES)
        assertCollision(problems, HighlightSeverity.WARNING, FIRST, SECOND)
        assertTrue(problems.all { it.description.contains("if the endpoint is exposed over HTTP and all these extensions are registered") })
    }

    fun testTestSourceExtensionDoesNotCollideWithAProductionExtension() {
        addTestSourceRoot()
        addProperties(EXPOSE_ALL)
        addFixtures(endpoint("x", "XEndpoint"), extension("XFirstExtension", "XEndpoint"), *collidingControl())
        myFixture.addFileToProject(TEST_FIXTURES, header() + "\n\n" + extension("XTestExtension", "XEndpoint"))
        assertPreconditions(X_PATH, EndpointExposure.EXPOSED)
        assertPreconditions(Y_PATH, EndpointExposure.EXPOSED)
        assertTrue(
            "precondition: the extension sits in test sources",
            ProjectFileIndex.getInstance(project).isInTestSourceContent(myFixture.findClass(TEST_EXTENSION).containingFile.virtualFile)
        )

        assertEquals(emptyList<String>(), problemsIn(TEST_FIXTURES).map { it.description })
        assertCollision(problemsIn(FIXTURES), HighlightSeverity.ERROR, Y_FIRST, Y_SECOND)
    }

    fun testDisabledEndpointDoesNotRescueTheCollision() {
        addProperties(EXPOSE_ALL, "management.endpoint.x.enabled=false")
        addFixtures(endpoint("x", "XEndpoint"), extension("XFirstExtension", "XEndpoint"), extension("XSecondExtension", "XEndpoint"))
        assertPreconditions(X_PATH, EndpointExposure.EXPOSED)
        assertEquals(listOf(EndpointAccess.NONE), actuatorEndpointsAt(X_PATH).map { it.access }.distinct())

        assertCollision(problemsIn(FIXTURES), HighlightSeverity.ERROR, FIRST, SECOND)
    }

    fun testDisabledConditionFilteringKeepsExtensionBeansWithNullVerdicts() {
        addProperties(EXPOSE_ALL)
        addFixtures(
            endpoint("x", "XEndpoint"),
            extension("XFirstExtension", "XEndpoint", gated = true),
            extension("XSecondExtension", "XEndpoint", gated = true)
        )
        val settings = SpringToolRunConfigurationsSettingsState.getInstance()
        val wasEnabled = settings.isBeanFilterEnabled
        try {
            settings.isBeanFilterEnabled = false
            ModificationTrackerManager.getInstance(project).invalidateAll()
            assertPreconditions(X_PATH, EndpointExposure.EXPOSED)
            val extensions = ActuatorWebExtensions.appliedTo(myFixture.findClass("com.app.XEndpoint"), module)
            assertEquals(setOf(FIRST, SECOND), extensions.map { it.psiClass.qualifiedName }.toSet())
            assertTrue(extensions.all { it.isBean && it.verdict == null })

            assertCollision(problemsIn(FIXTURES), HighlightSeverity.ERROR, FIRST, SECOND)
        } finally {
            settings.isBeanFilterEnabled = wasEnabled
            ModificationTrackerManager.getInstance(project).invalidateAll()
        }
    }

    fun testTwoUndecidedExtensionsMakeTheCollisionAWarning() {
        addProperties(EXPOSE_ALL, "$EXT_GATING_KEY=\${EXT_ENABLED}")
        addFixtures(
            endpoint("x", "XEndpoint"),
            extension("XFirstExtension", "XEndpoint", gated = true),
            extension("XSecondExtension", "XEndpoint", gated = true)
        )
        assertPreconditions(X_PATH, EndpointExposure.EXPOSED)
        assertBeanModelSays<ConditionVerdict.Undecided>(FIRST)
        assertBeanModelSays<ConditionVerdict.Undecided>(SECOND)

        val problems = problemsIn(FIXTURES)
        assertCollision(problems, HighlightSeverity.WARNING, FIRST, SECOND)
        assertTrue(problems.all { it.description.contains("if the endpoint is exposed over HTTP and all these extensions are registered") })
    }

    fun testTwoProjectExtensionsOfABuiltInEndpointCollide() {
        addProperties(EXPOSE_ALL)
        val healthEndpoint = "org.springframework.boot.health.actuate.endpoint.HealthEndpoint"
        addFixtures(extension("XFirstExtension", healthEndpoint), extension("XSecondExtension", healthEndpoint))
        assertPreconditions(HEALTH_PATH, EndpointExposure.EXPOSED)

        assertCollision(problemsIn(FIXTURES), HighlightSeverity.ERROR, FIRST, SECOND)
    }

    fun testTestSourceExtensionsCollideForATestSourceEndpoint() {
        addTestSourceRoot()
        addProperties(EXPOSE_ALL)
        addFixtures(*collidingControl())
        myFixture.addFileToProject(
            TEST_FIXTURES,
            header() + "\n\n" + listOf(
                endpoint("x", "XEndpoint"),
                extension("XFirstExtension", "XEndpoint"),
                extension("XSecondExtension", "XEndpoint")
            ).joinToString("\n\n")
        )
        assertTrue(
            "precondition: the endpoint sits in test sources",
            ProjectFileIndex.getInstance(project).isInTestSourceContent(myFixture.findClass("com.app.XEndpoint").containingFile.virtualFile)
        )
        assertPreconditions(X_PATH, EndpointExposure.EXPOSED)
        assertPreconditions(Y_PATH, EndpointExposure.EXPOSED)

        assertCollision(problemsIn(TEST_FIXTURES), HighlightSeverity.ERROR, FIRST, SECOND)
        assertCollision(problemsIn(FIXTURES), HighlightSeverity.ERROR, Y_FIRST, Y_SECOND)
    }

    fun testAddingAComposedAnnotationInvalidatesTheCachedAnnotationNames() {
        addProperties(EXPOSE_ALL)
        addFixtures(
            endpoint("x", "XEndpoint"),
            extension("XFirstExtension", "XEndpoint"),
            *collidingControl()
        )
        assertPreconditions(X_PATH, EndpointExposure.EXPOSED)
        assertCollision(problemsIn(FIXTURES), HighlightSeverity.ERROR, Y_FIRST, Y_SECOND)

        myFixture.addFileToProject(
            "com/app/AddedExtension.$fileExtension",
            header() + "\n\n" + composedAnnotation("MyExt", "XEndpoint") + "\n\n" + composedExtension("XComposedExtension", "MyExt")
        )
        ModificationTrackerManager.getInstance(project).invalidateAll()

        assertCollision(
            problemsIn("com/app/AddedExtension.$fileExtension"), HighlightSeverity.ERROR, COMPOSED,
            anchors = mapOf(COMPOSED to "@MyExt"),
            peers = listOf(COMPOSED, FIRST)
        )
    }

    private val FIXTURES get() = "com/app/Fixtures.$fileExtension"
    private val TEST_FIXTURES get() = "$TEST_SOURCES/com/app/TestFixtures.$fileExtension"

    private fun collidingControl(): Array<String> = arrayOf(
        endpoint("y", "YEndpoint"),
        extension("YFirstExtension", "YEndpoint"),
        extension("YSecondExtension", "YEndpoint")
    )

    private fun addProperties(vararg lines: String) {
        myFixture.addFileToProject("application.properties", lines.joinToString("\n"))
    }

    private fun addFixtures(vararg declarations: String) {
        myFixture.addFileToProject(FIXTURES, header() + "\n\n" + declarations.joinToString("\n\n"))
    }

    private fun addTestSourceRoot() {
        val root = myFixture.tempDirFixture.findOrCreateDir(TEST_SOURCES)
        PsiTestUtil.addSourceRoot(module, root, true)
        testSourceRoot = root
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    private fun assertPreconditions(endpointPath: String, exposure: EndpointExposure) {
        val annotation = JavaPsiFacade.getInstance(project).findClass(ENDPOINT_WEB_EXTENSION, GlobalSearchScope.allScope(project))
        assertNotNull("precondition: $ENDPOINT_WEB_EXTENSION resolves", annotation)
        assertTrue(
            "precondition: $ENDPOINT_WEB_EXTENSION is Boot's library class",
            ProjectFileIndex.getInstance(project).isInLibrary(annotation!!.containingFile.virtualFile)
        )
        assertEquals("precondition: servlet web stack", WebApplicationStack.SERVLET, WebApplicationStack.of(module))
        val endpoints = actuatorEndpointsAt(endpointPath)
        assertFalse("precondition: the Actuator loader lists $endpointPath", endpoints.isEmpty())
        assertEquals("precondition: exposure of $endpointPath", listOf(exposure), endpoints.map { it.exposure }.distinct())
    }

    private fun actuatorEndpointsAt(path: String): List<EndpointElement> {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        return SpringWebEndpointsLoader.EP_NAME.getExtensions(project).asSequence()
            .filter { it.getType() == EndpointType.ACTUATOR }
            .filter { it.isApplicable(module) }
            .flatMap { it.searchEndpoints(module) }
            .filter { it.path == path }
            .toList()
    }

    private inline fun <reified T : ConditionVerdict> assertBeanModelSays(className: String) {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        val verdict = SpringSearchService.getInstance(project).conditionVerdictOf(myFixture.findClass(className), module)
        assertInstanceOf(verdict, T::class.java)
    }

    private fun registeredInspection(): LocalInspectionTool {
        val registration = LocalInspectionEP.LOCAL_INSPECTION.extensionList.singleOrNull { it.implementationClass == INSPECTION_CLASS }
        assertNotNull("$INSPECTION_CLASS is registered as a local inspection", registration)
        return registration!!.instantiateTool() as LocalInspectionTool
    }

    private fun problemsIn(path: String): List<HighlightInfo> {
        val inspection = registeredInspection()
        myFixture.enableInspections(inspection)
        myFixture.configureFromTempProjectFile(path)
        return myFixture.doHighlighting().filter { it.inspectionToolId == inspection.shortName }
    }

    private fun ownerOf(problem: HighlightInfo): String? =
        generateSequence(myFixture.file.findElementAt(problem.startOffset)) { it.parent }
            .firstNotNullOfOrNull { it.toUElementOfType<UClass>() }
            ?.qualifiedName

    private fun assertCollision(
        problems: List<HighlightInfo>,
        severity: HighlightSeverity,
        vararg extensions: String,
        anchors: Map<String, String> = extensions.associateWith { DIRECT_ANCHOR },
        peers: List<String> = extensions.toList(),
    ) {
        assertEquals(
            "one problem per colliding extension: ${problems.map { it.description }}",
            extensions.toSet(),
            problems.mapNotNull(::ownerOf).toSet()
        )
        assertSize(extensions.size, problems)
        problems.forEach { problem ->
            val owner = ownerOf(problem)!!
            assertEquals("severity on $owner", severity, problem.severity)
            assertTrue("anchored on the extension annotation of $owner: ${problem.text}", problem.text.startsWith(anchors.getValue(owner)))
            val others = peers.filter { it != owner }.sorted()
            val positions = others.map { problem.description.indexOf(it) }
            assertTrue("message on $owner names $others in order: ${problem.description}", positions.all { it >= 0 })
            assertEquals(positions.sorted(), positions)
        }
    }

    protected companion object {
        const val INSPECTION_CLASS = "com.explyt.spring.web.inspections.DuplicateActuatorWebExtensionInspection"
        const val ENDPOINT_WEB_EXTENSION = "org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension"
        const val INCLUDE_KEY = "management.endpoints.web.exposure.include"
        const val EXCLUDE_KEY = "management.endpoints.web.exposure.exclude"
        const val EXPOSE_ALL = "$INCLUDE_KEY=*"
        const val EXT_GATING_KEY = "x.ext.enabled"
        const val TEST_SOURCES = "testsrc"
        const val DIRECT_ANCHOR = "@EndpointWebExtension"
        const val X_PATH = "/actuator/x"
        const val Y_PATH = "/actuator/y"
        const val HEALTH_PATH = "/actuator/health"
        const val FIRST = "com.app.XFirstExtension"
        const val SECOND = "com.app.XSecondExtension"
        const val THIRD = "com.app.XThirdExtension"
        const val SUB = "com.app.XSubExtension"
        const val COMPOSED = "com.app.XComposedExtension"
        const val TEST_EXTENSION = "com.app.XTestExtension"
        const val Y_FIRST = "com.app.YFirstExtension"
        const val Y_SECOND = "com.app.YSecondExtension"
        const val H_FIRST = "com.app.HFirstExtension"
        const val H_SECOND = "com.app.HSecondExtension"
    }
}
