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
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope

/**
 * A recorded bean whose class is still declared in the project, implementing the queried type, but outside the
 * selected application's classpath - a module the application stopped depending on after the import.
 *
 * Its type cannot be read in the application's scope, so it cannot be matched there; and the declaration that
 * still exists says it is a subtype, so it cannot be ruled out either. The answer stays undecided rather than
 * claiming the one healthy implementation is the only bean.
 */
class StaleNativeSnapshotSubtypeTest : ExplytMultiModuleTestCase() {

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

    fun testABeanRecordedAsASubtypeDeclaredOutsideTheClasspathKeepsTheAnswerUndecided() {
        val application = mainModuleClass(APPLICATION, application())
        mainModuleClass(ORDER_STORE, "package com.explyt.demo; public interface OrderStore {}")
        mainModuleClass(ORDER_STORE_IMPL, component("JdbcOrderStore"))
        val archive = addDependentModule("archive")
        addFileToModule(
            archive, "com/explyt/demo/archive/ArchivedOrderStore.java",
            "package com.explyt.demo.archive; public class ArchivedOrderStore implements com.explyt.demo.OrderStore {}"
        )
        assertNull(
            "Precondition: the subtype must not be on the application's classpath",
            JavaPsiFacade.getInstance(project).findClass(
                ARCHIVED_ORDER_STORE, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
            )
        )
        assertNotNull(
            "Precondition: the subtype must still be declared in the project",
            JavaPsiFacade.getInstance(project).findClass(ARCHIVED_ORDER_STORE, GlobalSearchScope.allScope(project))
        )

        installNativeRoot(application, bean("orderStoreImpl", ORDER_STORE_IMPL), bean("archivedOrderStore", ARCHIVED_ORDER_STORE))
        val snapshot = SpringSearchServiceFacade.getInstance(project)
            .getBeanSnapshot(application, BeanSourcePreference.NATIVE)

        val result = ScopedBeanMatcher(project).lookup(snapshot, BeanLookupSelector(ORDER_STORE, null))

        assertEquals(BeanOutcome.INDETERMINATE, result.outcome)
        assertEquals(1, result.match.unresolvedCount)
        assertEquals(listOf("orderStoreImpl"), result.match.records.map { it.name })
        assertTrue(ScopedBeanMatcher.TYPE_NOT_RESOLVABLE_IN_SCOPE in result.match.limitations)
    }

    /** The same stale class is not an answer to a query about an unrelated type. */
    fun testTheSameStaleBeanDoesNotAffectAnUnrelatedQuery() {
        val application = mainModuleClass(APPLICATION, application())
        mainModuleClass(ORDER_STORE, "package com.explyt.demo; public interface OrderStore {}")
        mainModuleClass("com.explyt.demo.Clock", "package com.explyt.demo; public interface Clock {}")
        mainModuleClass("com.explyt.demo.SystemClock", component("SystemClock", "Clock"))
        val archive = addDependentModule("archive")
        addFileToModule(
            archive, "com/explyt/demo/archive/ArchivedOrderStore.java",
            "package com.explyt.demo.archive; public class ArchivedOrderStore implements com.explyt.demo.OrderStore {}"
        )

        installNativeRoot(
            application, bean("systemClock", "com.explyt.demo.SystemClock"), bean("archivedOrderStore", ARCHIVED_ORDER_STORE)
        )
        val snapshot = SpringSearchServiceFacade.getInstance(project)
            .getBeanSnapshot(application, BeanSourcePreference.NATIVE)

        val result = ScopedBeanMatcher(project).lookup(snapshot, BeanLookupSelector("com.explyt.demo.Clock", null))

        assertEquals(BeanOutcome.SINGLE, result.outcome)
        assertEquals(0, result.match.unresolvedCount)
    }

    private fun mainModuleClass(fqn: String, text: String): PsiClass {
        addFileToModule(module, fqn.replace('.', '/') + ".java", text)
        return JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))
            ?: error("No PSI for $fqn")
    }

    private fun application() = """
        package com.explyt.demo;

        import org.springframework.boot.autoconfigure.SpringBootApplication;

        @SpringBootApplication
        public class App {}
    """.trimIndent()

    private fun component(name: String, implements: String = "OrderStore") = """
        package com.explyt.demo;

        import org.springframework.stereotype.Component;

        @Component
        public class $name implements $implements {}
    """.trimIndent()

    private fun bean(beanName: String, className: String) =
        SpringBeanData(beanName, className, "singleton", null, null, SpringBeanType.COMPONENT, false, true, true)

    private fun installNativeRoot(application: PsiClass, vararg beans: SpringBeanData) {
        val path = application.navigationElement.containingFile.virtualFile.canonicalPath!!
        project.getService(NativeSettings::class.java).linkProject(NativeProjectSettings().apply {
            externalProjectPath = path
            qualifiedMainClassName = application.qualifiedName
        })
        linkedPaths += path

        val root = DataNode(ProjectKeys.PROJECT, ProjectData(SYSTEM_ID, module.name, project.basePath!!, path), null)
        root.createChild(BeanSearch.KEY, BeanSearch(true, path))
        beans.forEach { root.createChild(SpringBeanData.KEY, it) }
        ExternalProjectsDataStorage.getInstance(project).update(InternalExternalProjectInfo(SYSTEM_ID, path, root))
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }

    private companion object {
        const val APPLICATION = "com.explyt.demo.App"
        const val ORDER_STORE = "com.explyt.demo.OrderStore"
        const val ORDER_STORE_IMPL = "com.explyt.demo.JdbcOrderStore"
        const val ARCHIVED_ORDER_STORE = "com.explyt.demo.archive.ArchivedOrderStore"
    }
}
