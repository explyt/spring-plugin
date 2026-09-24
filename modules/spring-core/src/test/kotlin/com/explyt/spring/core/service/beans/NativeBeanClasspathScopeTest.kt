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
import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.internal.InternalExternalProjectInfo
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsDataStorage
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil

/**
 * Choosing the right root is not enough: its beans must be resolved in that application's own classpath.
 *
 * Two applications can declare the same fully-qualified class against different dependencies. A project-wide
 * lookup answers the selected root with the *other* application's declaration - the reply names the right bean
 * and describes the wrong code, which no field of the answer reveals. These tests pin the declaration and the
 * type hierarchy, not merely the bean name, because only those two tell the classes apart.
 */
class NativeBeanClasspathScopeTest : ExplytMultiModuleTestCase() {

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

    fun testBeanClassIsResolvedInsideTheSelectedApplicationClasspath() {
        val (moduleA, moduleB) = twoIsolatedApplications()
        val appA = findClass("com.explyt.demo.a.AppA")!!
        val appB = findClass("com.explyt.demo.b.AppB")!!

        val implInA = classInScope(moduleA, CLOCK_IMPL)
        val implInB = classInScope(moduleB, CLOCK_IMPL)
        assertNotNull("Precondition: app-a must declare $CLOCK_IMPL", implInA)
        assertNotNull("Precondition: app-b must declare $CLOCK_IMPL", implInB)
        assertFalse(
            "Precondition: the two declarations must be distinct files, otherwise the scope proves nothing",
            implInA!!.containingFile.virtualFile == implInB!!.containingFile.virtualFile
        )

        installNativeRoot(appA, moduleA)
        installNativeRoot(appB, moduleB)

        val snapshot = SpringSearchServiceFacade.getInstance(project)
            .getBeanSnapshot(appA, BeanSourcePreference.NATIVE)
        val record = snapshot.records.single()

        // A project-wide lookup sees both declarations of the same FQN and resolves to neither, so the failure
        // it produces is a missing declaration. Naming that here keeps the diagnosis in the message instead of a
        // NullPointerException on the next line.
        assertNotNull(
            "The bean class must resolve inside app-a; a project-wide lookup finds two and resolves none",
            record.declaration
        )
        assertEquals(
            "The declaration must come from the selected application's own module",
            implInA.containingFile.virtualFile,
            record.declaration!!.containingFile.virtualFile
        )
        assertTrue(
            "The resolved type must implement app-a's ClockA, not app-b's ClockB",
            InheritanceUtil.isInheritor(record.declaredType, "com.explyt.demo.a.ClockA")
        )
        assertFalse(
            "app-b's hierarchy must not leak into app-a's snapshot",
            InheritanceUtil.isInheritor(record.declaredType, "com.explyt.demo.b.ClockB")
        )
    }

    /**
     * `addDependencyModule` makes the fixture's main module depend on the new one, so two such modules never see
     * each other - exactly the boundary a project-wide lookup would cross.
     */
    private fun twoIsolatedApplications(): Pair<Module, Module> {
        val moduleA = addDependencyModule("app-a")
        val moduleB = addDependencyModule("app-b")

        addFileToModule(moduleA, "com/explyt/demo/a/AppA.java", application("com.explyt.demo.a", "AppA"))
        addFileToModule(moduleA, "com/explyt/demo/a/ClockA.java", marker("com.explyt.demo.a", "ClockA"))
        addFileToModule(moduleA, "com/explyt/demo/ClockImpl.java", clockImpl("com.explyt.demo.a.ClockA"))

        addFileToModule(moduleB, "com/explyt/demo/b/AppB.java", application("com.explyt.demo.b", "AppB"))
        addFileToModule(moduleB, "com/explyt/demo/b/ClockB.java", marker("com.explyt.demo.b", "ClockB"))
        addFileToModule(moduleB, "com/explyt/demo/ClockImpl.java", clockImpl("com.explyt.demo.b.ClockB"))

        return moduleA to moduleB
    }

    private fun installNativeRoot(application: PsiClass, module: Module) {
        val path = application.navigationElement.containingFile.virtualFile.canonicalPath!!
        project.getService(NativeSettings::class.java).linkProject(NativeProjectSettings().apply {
            externalProjectPath = path
            qualifiedMainClassName = application.qualifiedName
        })
        linkedPaths += path

        val root = DataNode(
            ProjectKeys.PROJECT,
            ProjectData(SYSTEM_ID, module.name, project.basePath!!, path),
            null
        )
        root.createChild(BeanSearch.KEY, BeanSearch(true, path))
        root.createChild(
            SpringBeanData.KEY,
            SpringBeanData("clock", CLOCK_IMPL, "singleton", null, null, SpringBeanType.COMPONENT, false, true, true)
        )
        ExternalProjectsDataStorage.getInstance(project)
            .update(InternalExternalProjectInfo(SYSTEM_ID, path, root))
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }

    private fun classInScope(module: Module, fqn: String): PsiClass? = JavaPsiFacade.getInstance(project)
        .findClass(fqn, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false))

    private fun findClass(fqn: String): PsiClass? = JavaPsiFacade.getInstance(project)
        .findClass(fqn, GlobalSearchScope.projectScope(project))

    private fun application(packageName: String, name: String): String = """
        package $packageName;

        import org.springframework.boot.autoconfigure.SpringBootApplication;

        @SpringBootApplication
        public class $name {}
    """.trimIndent()

    private fun marker(packageName: String, name: String): String = """
        package $packageName;

        public interface $name {}
    """.trimIndent()

    private fun clockImpl(markerFqn: String): String = """
        package com.explyt.demo;

        import org.springframework.stereotype.Component;

        @Component
        public class ClockImpl implements $markerFqn {}
    """.trimIndent()

    private companion object {
        const val CLOCK_IMPL = "com.explyt.demo.ClockImpl"
    }
}
