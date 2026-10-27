/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.search

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.service.ProfileActivation
import com.explyt.spring.core.service.ProfilesService
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.ide.actions.searcheverywhere.FoundItemDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import java.util.concurrent.TimeUnit
import javax.swing.Icon

class BeanSearchEverywhereContributorTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    override fun tearDown() {
        try {
            ProfilesService.getInstance(project).updateFromConfiguration(null)
        } finally {
            super.tearDown()
        }
    }

    fun testJavaProfileInactiveComponentIsListedAsInactive() {
        addApplicationAndActiveProfile()
        myFixture.addFileToProject(
            "com/app/DevOnlyService.java",
            """
            package com.app;
            @org.springframework.stereotype.Component
            @org.springframework.context.annotation.Profile("dev")
            public class DevOnlyService {}
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/app/ProdService.java",
            "package com.app; @org.springframework.stereotype.Component public class ProdService {}"
        )

        assertListedAsInactiveNextToActiveControl()
    }

    fun testKotlinProfileInactiveComponentIsListedAsInactive() {
        addApplicationAndActiveProfile()
        myFixture.addFileToProject(
            "com/app/DevOnlyService.kt",
            """
            package com.app
            @org.springframework.stereotype.Component
            @org.springframework.context.annotation.Profile("dev")
            class DevOnlyService
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/app/ProdService.kt",
            "package com.app\n@org.springframework.stereotype.Component class ProdService"
        )

        assertListedAsInactiveNextToActiveControl()
    }

    private fun assertListedAsInactiveNextToActiveControl() {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        assertEquals(
            "Precondition: DevOnlyService is inactive by its profile",
            ProfileActivation.INACTIVE,
            SpringSearchService.getInstance(project).profileActivation(myFixture.findClass("com.app.DevOnlyService"))
        )
        assertTrue("Precondition: a Spring Boot project", SpringCoreUtil.isSpringBootProject(project))

        assertSame(SpringIcons.SpringBean, singleIconOf("prodService"))
        assertSame(SpringIcons.springBeanInactive, singleIconOf("devOnlyService"))
    }

    private fun singleIconOf(beanName: String): Icon? {
        val items = itemsMatching(beanName)
        assertEquals("Exactly one item named $beanName", listOf(beanName), items.map { it.name })
        return items.single().presentation.getIcon(false)
    }

    private fun addApplicationAndActiveProfile() {
        myFixture.addFileToProject("application.properties", "spring.profiles.active=prod")
        myFixture.addFileToProject(
            "com/app/Application.kt",
            "package com.app\n@org.springframework.boot.autoconfigure.SpringBootApplication class Application"
        )
        ModificationTrackerManager.getInstance(project).invalidateAll()
        val profiles = ProfilesService.getInstance(project)
        assertTrue("Precondition: prod is active", profiles.compute("prod"))
        assertFalse("Precondition: dev is not active", profiles.compute("dev"))
    }

    private fun itemsMatching(pattern: String): List<BeanNavigationItem> {
        val contributor = BeanSearchEverywhereContributor(project)
        val indicator = ProgressIndicatorBase()
        val found = ApplicationManager.getApplication().executeOnPooledThread<List<BeanNavigationItem>> {
            val items = mutableListOf<BeanNavigationItem>()
            contributor.fetchWeightedElements(pattern, indicator) { descriptor: FoundItemDescriptor<BeanNavigationItem> ->
                items += descriptor.item
                true
            }
            items
        }
        return found.get(60, TimeUnit.SECONDS)
    }
}
