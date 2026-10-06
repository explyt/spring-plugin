/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.profile

import com.explyt.spring.core.runconfiguration.SpringBootConfigurationFactory
import com.explyt.spring.core.runconfiguration.SpringBootRunConfiguration
import com.explyt.spring.core.service.ProfilesService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.execution.RunManager
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl


class ProfileGroupsTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    override fun tearDown() {
        try {
            ProfilesService.getInstance(project).updateFromConfiguration(null)
        } finally {
            super.tearDown()
        }
    }

    fun testYamlGroupsInEveryFormActivateTheirMembers() {
        addConfig(
            "application.yaml",
            """
            spring:
              profiles:
                active: production, reporting, finance
                group:
                  production:
                    - billing
                    - audit
                  reporting[0]: ledger
                  finance: invoices, payroll
                  staging: sandbox
            """.trimIndent()
        )

        assertActive("production", "reporting", "finance", "billing", "audit", "ledger", "invoices", "payroll")
        assertInactive("staging", "sandbox")
    }

    fun testPropertiesGroupsSupportCommaListsAndIndexedKeys() {
        addConfig(
            "application.properties",
            """
            spring.profiles.active=production,reporting
            spring.profiles.group.production=billing, audit
            spring.profiles.group.reporting[0]=ledger
            spring.profiles.group.reporting[1]=invoices
            """.trimIndent()
        )

        assertActive("production", "reporting", "billing", "audit", "ledger", "invoices")
    }

    fun testFlowSequencesQuotedScalarsAndDottedKeysAreRead() {
        addConfig(
            "application.yaml",
            """
            spring.profiles:
              active: production, reporting
              group.production: [billing, "audit"]
            spring:
              profiles:
                group:
                  reporting: 'ledger, invoices'
            """.trimIndent()
        )

        assertActive("production", "reporting", "billing", "audit", "ledger", "invoices")
    }

    fun testGroupInADocumentActivatedOnProfileIsIgnored() {
        addConfig(
            "application.yaml",
            """
            spring:
              profiles:
                active: production
            ---
            spring:
              config:
                activate:
                  on-profile: production
              profiles:
                group:
                  production: billing
            """.trimIndent()
        )

        assertActive("production")
        assertInactive("billing")
    }

    fun testGroupsOfSeveralApplicationConfigsAreMerged() {
        addConfig("first/application.properties", "spring.profiles.active=production\nspring.profiles.group.production=billing")
        addConfig("second/application.yaml", "spring:\n  profiles:\n    group:\n      production: [audit]\n")

        assertActive("production", "billing", "audit")
    }


    fun testNestedGroupsExpandTransitively() {
        addConfig(
            "application.yaml",
            """
            spring:
              profiles:
                active: production
                group:
                  production: billing
                  billing: invoicing
            """.trimIndent()
        )

        assertActive("production", "billing", "invoicing")
    }

    fun testCyclicGroupsTerminate() {
        addConfig(
            "application.yaml",
            """
            spring:
              profiles:
                active: a
                group:
                  a: b
                  b: a
            """.trimIndent()
        )

        assertActive("a", "b")
    }

    fun testMembersOfAnInactiveGroupStayInactive() {
        addConfig(
            "application.yaml",
            """
            spring:
              profiles:
                active: production
                group:
                  production: billing
                  staging: sandbox
            """.trimIndent()
        )

        assertActive("production", "billing")
        assertInactive("staging", "sandbox")
    }

    fun testDefaultProfileGroupExpandsWhenNothingIsActive() {
        addConfig(
            "application.yaml",
            """
            spring:
              profiles:
                group:
                  default: local
            """.trimIndent()
        )

        assertActive("default", "local")
    }

    fun testRunConfigurationProfilesAreExpanded() {
        addConfig(
            "application.yaml",
            """
            spring:
              profiles:
                group:
                  production: billing
            """.trimIndent()
        )
        val runManager = RunManager.getInstance(project) as RunManagerImpl
        val runConfiguration =
            SpringBootConfigurationFactory.createTemplateConfiguration(project) as SpringBootRunConfiguration
        runConfiguration.springProfiles = "production"

        ProfilesService.getInstance(project)
            .updateFromConfiguration(RunnerAndConfigurationSettingsImpl(runManager, runConfiguration))

        assertActive("production", "billing")
        assertInactive("default")
    }

    fun testGroupDeclaredOnlyInAProfileSpecificFileNeverExpands() {
        addConfig("application.yaml", "spring:\n  profiles:\n    active: dev\n")
        addConfig(
            "application-dev.yaml",
            """
            spring:
              profiles:
                group:
                  dev: extra
            """.trimIndent()
        )

        assertActive("dev")
        assertInactive("extra")
    }

    fun testProfileGatedConfigurationBecomesABeanThroughItsGroup() {
        addConfig(
            "application.yaml",
            """
            spring:
              profiles:
                active: production
                group:
                  production: billing
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/app/Application.kt",
            """
            package com.app

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class Application
            """.trimIndent()
        )
        addProfileConfiguration("BillingConfig", "billing")
        addProfileConfiguration("StagingConfig", "staging")

        val beanClasses = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .mapNotNull { it.psiClass.qualifiedName }
            .toSet()

        assertTrue("Expected BillingConfig among $beanClasses", "com.app.BillingConfig" in beanClasses)
        assertFalse("StagingConfig must stay inactive, got $beanClasses", "com.app.StagingConfig" in beanClasses)
    }

    private fun addProfileConfiguration(className: String, profile: String) {
        myFixture.addFileToProject(
            "com/app/$className.kt",
            """
            package com.app

            import org.springframework.context.annotation.Configuration
            import org.springframework.context.annotation.Profile

            @Configuration
            @Profile("$profile")
            class $className
            """.trimIndent()
        )
    }

    private fun addConfig(fileName: String, text: String) {
        myFixture.addFileToProject(fileName, text)
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }

    private fun assertActive(vararg profiles: String) {
        val profilesService = ProfilesService.getInstance(project)
        for (profile in profiles) {
            assertTrue(
                "Profile '$profile' must be active",
                profilesService.compute(profile)
            )
        }
    }

    private fun assertInactive(vararg profiles: String) {
        val profilesService = ProfilesService.getInstance(project)
        for (profile in profiles) {
            assertFalse(
                "Profile '$profile' must stay inactive",
                profilesService.compute(profile)
            )
        }
    }
}
