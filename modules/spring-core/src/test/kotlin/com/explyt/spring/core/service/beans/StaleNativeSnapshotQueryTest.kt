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
import com.explyt.spring.test.ExplytJavaLightTestCase
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
 * A recorded context goes stale bean by bean: a class renamed or removed since the import leaves a record whose
 * type no longer resolves, next to healthy records whose types still do.
 *
 * Those stale records must not decide answers about other types. A query for a healthy local interface with one
 * implementation is answered `SINGLE` even though the same snapshot holds a broken bean elsewhere, while a stale
 * record that the model recorded as the queried type, or as a type still declared as its subtype, keeps the answer
 * honestly undecided. The records here are read from a real native root by the real reader, so the type names
 * they carry are the ones an import produces, not ones a hand-built fixture chose.
 */
class StaleNativeSnapshotQueryTest : ExplytJavaLightTestCase() {

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

    fun testAStaleUnrelatedBeanDoesNotMakeAHealthyQueryIndeterminate() {
        val application = orderStoreApplication()
        installNativeRoot(
            application,
            component("orderStoreImpl", ORDER_STORE_IMPL),
            component("legacyReportJob", "com.explyt.demo.jobs.LegacyReportJob")
        )
        val snapshot = nativeSnapshot(application)
        assertStaleRecordIsUnreadable(snapshot, "legacyReportJob")

        val result = ScopedBeanMatcher(project).lookup(snapshot, BeanLookupSelector(ORDER_STORE, null))

        assertEquals(BeanOutcome.SINGLE, result.outcome)
        assertEquals(MatchCompleteness.COMPLETE, result.match.completeness)
        assertEquals(0, result.match.unresolvedCount)
        assertEquals(listOf("orderStoreImpl"), result.match.records.map { it.name })
        assertFalse(
            "The stale bean's limitation belongs to that bean, not to this answer, got ${result.match.limitations}",
            NativeBeanSnapshotReader.TYPE_NOT_ON_APPLICATION_CLASSPATH in result.match.limitations
        )
    }

    /**
     * A factory bean exported without its return type was never given a type by the model: its configuration
     * class is gone, so nothing says what it produces, and it may well be an answer the classpath lost.
     */
    fun testAStaleFactoryBeanWithoutARecordedTypeKeepsTheAnswerUndecided() {
        val application = orderStoreApplication()
        installNativeRoot(
            application,
            component("orderStoreImpl", ORDER_STORE_IMPL),
            factory("archivedOrderStore", "com.explyt.demo.config.GoneConfig", "archivedOrderStore", returnType = null)
        )
        val snapshot = nativeSnapshot(application)
        assertStaleRecordIsUnreadable(snapshot, "archivedOrderStore")
        assertNull(
            "Precondition: the model recorded no type for the factory bean",
            snapshot.records.single { it.name == "archivedOrderStore" }.recordedTypeFqn
        )

        val result = ScopedBeanMatcher(project).lookup(snapshot, BeanLookupSelector(ORDER_STORE, null))

        assertEquals(BeanOutcome.INDETERMINATE, result.outcome)
        assertEquals(MatchCompleteness.PARTIAL, result.match.completeness)
        assertEquals(1, result.match.unresolvedCount)
        assertEquals(listOf("orderStoreImpl"), result.match.records.map { it.name })
        assertTrue(ScopedBeanMatcher.TYPE_NOT_RESOLVABLE_IN_SCOPE in result.match.limitations)
        assertTrue(
            "The undecided record's own limitation is part of this answer",
            NativeBeanSnapshotReader.TYPE_NOT_ON_APPLICATION_CLASSPATH in result.match.limitations
        )
    }

    /**
     * A factory whose configuration class is gone but whose recorded return type still resolves is read through
     * that type: it is a match, not an undecided record.
     */
    fun testAStaleFactoryBeanWhoseRecordedTypeResolvesIsAMatch() {
        val application = orderStoreApplication()
        installNativeRoot(
            application,
            component("orderStoreImpl", ORDER_STORE_IMPL),
            factory("archivedOrderStore", "com.explyt.demo.config.GoneConfig", "archivedOrderStore", ORDER_STORE)
        )
        val snapshot = nativeSnapshot(application)

        val result = ScopedBeanMatcher(project).lookup(snapshot, BeanLookupSelector(ORDER_STORE, null))

        assertEquals(BeanOutcome.MULTIPLE, result.outcome)
        assertEquals(setOf("orderStoreImpl", "archivedOrderStore"), result.match.records.map { it.name }.toSet())
    }


    /** The injection point of the same healthy type resolves too: it is the same matcher behind both modes. */
    fun testAnInjectionPointOfAHealthyTypeIsResolvedDespiteAStaleBean() {
        val application = orderStoreApplication()
        installNativeRoot(
            application,
            component("orderStoreImpl", ORDER_STORE_IMPL),
            component("legacyReportJob", "com.explyt.demo.jobs.LegacyReportJob")
        )
        val snapshot = nativeSnapshot(application)
        val orderStore = JavaPsiFacade.getElementFactory(project)
            .createType(findClass(ORDER_STORE))

        val match = ScopedBeanMatcher(project).matchType(snapshot.records, orderStore)

        assertEquals(MatchCompleteness.COMPLETE, match.completeness)
        assertEquals(listOf("orderStoreImpl"), match.records.map { it.name })
    }

    fun testTheImportTimeOfTheRootIsKept() {
        val application = orderStoreApplication()
        installNativeRoot(application, component("orderStoreImpl", ORDER_STORE_IMPL), importedAt = IMPORTED_AT)

        val context = NativeBeanSnapshotReader(project).contexts().single()

        assertEquals(IMPORTED_AT, context.importedAt)
    }

    fun testARootNeverImportedSuccessfullyHasNoImportTime() {
        val application = orderStoreApplication()
        installNativeRoot(application, component("orderStoreImpl", ORDER_STORE_IMPL), importedAt = null)

        val context = NativeBeanSnapshotReader(project).contexts().single()

        assertNull("-1 is the platform's 'never', not a time", context.importedAt)
    }

    private fun assertStaleRecordIsUnreadable(snapshot: ScopedBeanSnapshot, name: String) {
        val stale = snapshot.records.single { it.name == name }
        assertNull("Precondition: the stale record's type must not resolve", stale.declaredType)
        assertTrue(
            "Precondition: the reader must report the stale class, got ${stale.limitations}",
            NativeBeanSnapshotReader.TYPE_NOT_ON_APPLICATION_CLASSPATH in stale.limitations
        )
    }

    private fun nativeSnapshot(application: PsiClass): ScopedBeanSnapshot {
        val snapshot = SpringSearchServiceFacade.getInstance(project)
            .getBeanSnapshot(application, BeanSourcePreference.NATIVE)
        assertEquals(BeanModelSource.NATIVE_SNAPSHOT, snapshot.selection.source)
        return snapshot
    }

    private fun orderStoreApplication(): PsiClass {
        myFixture.addClass(
            """
            package com.explyt.demo;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class App {}
            """.trimIndent()
        )
        myFixture.addClass("package com.explyt.demo; public interface OrderStore {}")
        myFixture.addClass(
            """
            package com.explyt.demo;

            import org.springframework.stereotype.Component;

            @Component
            public class JdbcOrderStore implements OrderStore {}
            """.trimIndent()
        )
        return findClass(APPLICATION)
    }

    private fun component(beanName: String, className: String) =
        SpringBeanData(beanName, className, "singleton", null, null, SpringBeanType.COMPONENT, false, true, true)

    private fun factory(beanName: String, declaringClass: String, methodName: String, returnType: String?) =
        SpringBeanData(beanName, declaringClass, "singleton", methodName, returnType, SpringBeanType.OTHER, false, true, true)

    private fun installNativeRoot(
        application: PsiClass,
        vararg beans: SpringBeanData,
        importedAt: Long? = IMPORTED_AT
    ) {
        val path = application.navigationElement.containingFile.virtualFile.canonicalPath!!
        project.getService(NativeSettings::class.java).linkProject(NativeProjectSettings().apply {
            externalProjectPath = path
            qualifiedMainClassName = application.qualifiedName
        })
        linkedPaths += path

        val root = DataNode(ProjectKeys.PROJECT, ProjectData(SYSTEM_ID, "demo", project.basePath!!, path), null)
        root.createChild(BeanSearch.KEY, BeanSearch(true, path))
        beans.forEach { root.createChild(SpringBeanData.KEY, it) }
        val info = InternalExternalProjectInfo(SYSTEM_ID, path, root)
        importedAt?.let {
            info.lastImportTimestamp = it
            info.lastSuccessfulImportTimestamp = it
        }
        ExternalProjectsDataStorage.getInstance(project).update(info)
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }

    private fun findClass(fqn: String): PsiClass = JavaPsiFacade.getInstance(project)
        .findClass(fqn, GlobalSearchScope.allScope(project))
        ?: error("No PSI for $fqn")

    private companion object {
        const val APPLICATION = "com.explyt.demo.App"
        const val ORDER_STORE = "com.explyt.demo.OrderStore"
        const val ORDER_STORE_IMPL = "com.explyt.demo.JdbcOrderStore"
        const val IMPORTED_AT = 1_790_000_000_000L
    }
}
