/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.core.externalsystem.model.BeanSearch
import com.explyt.spring.core.externalsystem.model.SpringBeanData
import com.explyt.spring.core.externalsystem.model.SpringBeanType
import com.explyt.spring.core.externalsystem.setting.NativeProjectSettings
import com.explyt.spring.core.externalsystem.setting.NativeSettings
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.internal.InternalExternalProjectInfo
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsDataStorage
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope

/**
 * The snapshot must come from **one** chosen model.
 *
 * Two loaded applications declare beans of the same type, so a reader that merges their roots and filters
 * afterwards returns an answer that looks correct and names the wrong application's bean. These tests pin the
 * isolation at the boundary the query actually crosses: the facade.
 */
class BeanSnapshotServiceTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    private val linkedPaths = mutableListOf<String>()

    override fun tearDown() {
        try {
            val settings = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
            linkedPaths.forEach { settings.unlinkExternalProject(it) }
            linkedPaths.clear()
        } finally {
            super.tearDown()
        }
    }

    fun testNativeSnapshotAnswersFromTheSelectedRootOnly() {
        val appA = applicationClass("AppA", "com.explyt.demo.a")
        val appB = applicationClass("AppB", "com.explyt.demo.b")
        installNativeRoot(appA, "clockA")
        installNativeRoot(appB, "clockB")
        assertEquals(
            "Precondition: both roots must be loaded, otherwise isolation is proven vacuously",
            2, NativeBeanSnapshotReader(project).contexts().size
        )

        val snapshot = SpringSearchServiceFacade.getInstance(project)
            .getBeanSnapshot(appA, BeanSourcePreference.NATIVE)

        assertEquals(listOf("clockA"), snapshot.records.map { it.name })
        assertEquals(true, snapshot.records.single().primary)
        assertEquals(BeanModelSource.NATIVE_SNAPSHOT, snapshot.selection.source)
    }

    /**
     * A `@Bean` whose type belongs to the JDK still has a declaration in the project.
     *
     * Deriving the module from the bean *type* loses it - `java.time.Clock` is not in any module - so the record
     * is anchored on the factory method instead.
     */
    fun testStaticSnapshotKeepsTheFactoryOfALibraryTypedBean() {
        val application = applicationClass("App", "com.explyt.demo")
        myFixture.addFileToProject(
            "com/explyt/demo/TimeConfig.java",
            """
            package com.explyt.demo;

            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            import java.time.Clock;

            @Configuration
            public class TimeConfig {
                @Bean(name = {"systemClock", "utcClock"})
                public Clock systemClock() { return Clock.systemUTC(); }
            }
            """.trimIndent()
        )

        val snapshot = SpringSearchServiceFacade.getInstance(project)
            .getBeanSnapshot(application, BeanSourcePreference.STATIC)
        val clock = snapshot.records.singleOrNull { "systemClock" in it.knownNames }

        assertNotNull("The @Bean Clock must be in the static snapshot, got ${snapshot.records.map { it.name }}", clock)
        assertEquals("java.time.Clock", clock!!.typeName)
        assertEquals("systemClock", (clock.declaration as PsiMethod).name)
        assertEquals(BeanKind.BEAN_METHOD, clock.kind)
        assertEquals(
            "The declaring module comes from the factory, not from the JDK type",
            module.name, clock.declarationModule
        )
        assertTrue("Both declared names must be known", clock.knownNames.containsAll(setOf("systemClock", "utcClock")))
        assertEquals(BeanModelSource.STATIC, snapshot.selection.source)
        assertTrue(
            "A static answer must say it is a module estimate",
            BeanContextSelector.STATIC_CONTEXT_APPROXIMATE in snapshot.limitations
        )
    }

    /**
     * Compact enumeration must not pay for declaration evidence nobody asked for.
     *
     * The bean here is deliberately an active one: a `@Profile`-gated bean never reaches the snapshot at all, so
     * it could not tell a lazy read apart from a record the active model filtered out.
     */
    fun testEnumerationDoesNotExpandDeclarationEvidence() {
        val application = applicationClass("App", "com.explyt.demo")
        myFixture.addFileToProject(
            "com/explyt/demo/QualifiedConfig.java",
            """
            package com.explyt.demo;

            import org.springframework.beans.factory.annotation.Qualifier;
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            import java.time.Clock;

            @Configuration
            public class QualifiedConfig {
                @Bean
                @Qualifier("utc")
                public Clock utcClock() { return Clock.systemUTC(); }
            }
            """.trimIndent()
        )

        val snapshot = SpringSearchServiceFacade.getInstance(project)
            .getBeanSnapshot(application, BeanSourcePreference.STATIC)
        val clock = snapshot.records.singleOrNull { it.name == "utcClock" }
        assertNotNull("Precondition: the bean must be enumerated at all", clock)

        assertNull("Enumeration must not read conditions", clock!!.details.conditions)
        assertNull("Enumeration must not read qualifiers", clock.details.qualifiers)

        val details = BeanDetailsReader.read(clock)
        assertEquals(
            listOf("org.springframework.beans.factory.annotation.Qualifier"),
            details.qualifiers?.map { it.annotationClass }
        )
        assertEquals("utc", details.qualifiers?.single()?.attributes?.get("value"))
    }

    /**
     * The same query must answer the same way regardless of which tab is focused.
     *
     * An editor-driven stamp would tell a caller paging through results that the model changed when only the UI
     * did, and an editor-driven source would silently switch which application answered.
     */
    fun testTheAnswerDoesNotDependOnTheSelectedEditor() {
        val application = applicationClass("App", "com.explyt.demo")
        val facade = SpringSearchServiceFacade.getInstance(project)
        val other = myFixture.addFileToProject(
            "com/explyt/demo/Unrelated.java",
            "package com.explyt.demo;\n\npublic class Unrelated {}"
        )

        val before = facade.getBeanSnapshot(application, BeanSourcePreference.STATIC)

        myFixture.openFileInEditor(other.virtualFile)
        assertEquals(
            "Precondition: the editor must really have been switched",
            other.virtualFile,
            myFixture.editor.virtualFile
        )

        val after = facade.getBeanSnapshot(application, BeanSourcePreference.STATIC)

        assertEquals("The model stamp must not follow the editor", before.modelStamp, after.modelStamp)
        assertEquals(before.selection.source, after.selection.source)
        assertEquals(before.records.map { it.id }, after.records.map { it.id })
    }

    private fun applicationClass(name: String, packageName: String): PsiClass {
        myFixture.addFileToProject(
            "${packageName.replace('.', '/')}/$name.kt",
            """
            package $packageName

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class $name
            """.trimIndent()
        )
        return JavaPsiFacade.getInstance(project)
            .findClass("$packageName.$name", GlobalSearchScope.projectScope(project))
            ?: error("No PSI for $packageName.$name")
    }

    private fun installNativeRoot(application: PsiClass, beanName: String) {
        val path = application.navigationElement.containingFile.virtualFile.canonicalPath!!
        project.getService(NativeSettings::class.java).linkProject(NativeProjectSettings().apply {
            externalProjectPath = path
            qualifiedMainClassName = application.qualifiedName
        })
        linkedPaths += path

        val root = DataNode(ProjectKeys.PROJECT, ProjectData(SYSTEM_ID, application.name!!, project.basePath!!, path), null)
        root.createChild(BeanSearch.KEY, BeanSearch(true, path))
        root.createChild(
            SpringBeanData.KEY,
            SpringBeanData(beanName, "java.time.Clock", "singleton", null, null, SpringBeanType.OTHER, true, true, false)
        )
        ExternalProjectsDataStorage.getInstance(project)
            .update(InternalExternalProjectInfo(SYSTEM_ID, path, root))
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }
}
