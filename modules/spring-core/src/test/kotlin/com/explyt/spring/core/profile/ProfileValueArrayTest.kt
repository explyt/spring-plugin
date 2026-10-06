/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.profile

import com.explyt.spring.core.service.ProfilesService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary

class ProfileValueArrayTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    private val allProfiles = setOf("dev", "local", "prod", "test")

    override fun tearDown() {
        try {
            ProfilesService.getInstance(project).updateFromConfiguration(null)
        } finally {
            super.tearDown()
        }
    }

    fun testJavaArrayIsActiveWhenItsFirstProfileIsActive() {
        activate("dev")
        addJavaConfiguration("DevOrLocalConfig", "@Profile({\"dev\", \"local\"})")

        assertBeanActive("DevOrLocalConfig")
    }

    fun testJavaArrayIsActiveWhenItsSecondProfileIsActive() {
        activate("local")
        addJavaConfiguration("DevOrLocalConfig", "@Profile({\"dev\", \"local\"})")

        assertBeanActive("DevOrLocalConfig")
    }

    fun testJavaArrayIsInactiveWhenNoneOfItsProfilesIsActive() {
        activate("prod")
        addJavaConfiguration("DevOrLocalConfig", "@Profile({\"dev\", \"local\"})")

        assertBeanInactive("DevOrLocalConfig")
    }

    fun testNegatedArrayElementActivatesWhenItsProfileIsAbsent() {
        activate("dev")
        addJavaConfiguration("NotProdOrTestConfig", "@Profile({\"!prod\", \"test\"})")

        assertBeanActive("NotProdOrTestConfig")
    }

    fun testNegatedArrayIsInactiveWhenNoElementMatches() {
        activate("prod")
        addJavaConfiguration("NotProdOrTestConfig", "@Profile({\"!prod\", \"test\"})")

        assertBeanInactive("NotProdOrTestConfig")
    }

    fun testAndExpressionNeedsEveryProfile() {
        activate("dev")
        addJavaConfiguration("DevAndLocalConfig", "@Profile(\"dev & local\")")

        assertBeanInactive("DevAndLocalConfig")
    }

    fun testAndExpressionIsActiveWhenEveryProfileIsActive() {
        activate("dev", "local")
        addJavaConfiguration("DevAndLocalConfig", "@Profile(\"dev & local\")")

        assertBeanActive("DevAndLocalConfig")
    }

    fun testDirectAndMetaProfileAnnotationsMatchWhenAnyOfThemMatches() {
        activate("dev")
        myFixture.addFileToProject(
            "com/app/Dev.java",
            """
            package com.app;

            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import org.springframework.context.annotation.Profile;

            @Retention(RetentionPolicy.RUNTIME)
            @Profile("dev")
            public @interface Dev {
            }
            """.trimIndent()
        )
        addJavaConfiguration("DevMetaOrLocalConfig", "@Dev @Profile(\"local\")")

        assertBeanActive("DevMetaOrLocalConfig")
    }

    fun testKotlinVarargIsActiveWhenOneOfItsProfilesIsActive() {
        activate("dev")
        addApplication()
        myFixture.addFileToProject(
            "com/app/KotlinDevOrLocalConfig.kt",
            """
            package com.app

            import org.springframework.context.annotation.Configuration
            import org.springframework.context.annotation.Profile

            @Configuration
            @Profile("dev", "local")
            class KotlinDevOrLocalConfig
            """.trimIndent()
        )

        assertBeanActive("KotlinDevOrLocalConfig")
    }

    private fun activate(vararg profiles: String) {
        myFixture.addFileToProject(
            "application.yaml",
            "spring:\n  profiles:\n    active: ${profiles.joinToString(", ")}\n"
        )
        ModificationTrackerManager.getInstance(project).invalidateAll()

        val profilesService = ProfilesService.getInstance(project)
        val active = allProfiles.filterTo(sortedSetOf()) { profilesService.compute(it) }
        assertEquals("Precondition: active profiles", profiles.toSortedSet(), active)
    }

    private fun addApplication() {
        myFixture.addFileToProject(
            "com/app/Application.kt",
            """
            package com.app

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class Application
            """.trimIndent()
        )
    }

    private fun addJavaConfiguration(className: String, profileAnnotations: String) {
        addApplication()
        myFixture.addFileToProject(
            "com/app/$className.java",
            """
            package com.app;

            import org.springframework.context.annotation.Configuration;
            import org.springframework.context.annotation.Profile;

            @Configuration
            $profileAnnotations
            public class $className {
            }
            """.trimIndent()
        )
    }

    private fun activeBeanClasses(): Set<String> =
        SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .mapNotNullTo(mutableSetOf()) { it.psiClass.qualifiedName }

    private fun assertBeanActive(className: String) {
        val beanClasses = activeBeanClasses()
        assertTrue("Precondition: Application must be a bean, got $beanClasses", "com.app.Application" in beanClasses)
        assertTrue("Expected com.app.$className among $beanClasses", "com.app.$className" in beanClasses)
    }

    private fun assertBeanInactive(className: String) {
        val beanClasses = activeBeanClasses()
        assertTrue("Precondition: Application must be a bean, got $beanClasses", "com.app.Application" in beanClasses)
        assertFalse("com.app.$className must stay inactive, got $beanClasses", "com.app.$className" in beanClasses)
    }
}
