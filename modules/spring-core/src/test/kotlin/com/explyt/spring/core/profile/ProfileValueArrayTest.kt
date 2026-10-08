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
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.toUElementOfType

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

    fun testEmptyProfileListDoesNotHideTheBean() {
        activate("dev")
        addJavaConfiguration("EmptyProfileConfig", "@Profile({})")

        assertBeanActive("EmptyProfileConfig")
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

    fun testJavaUnresolvedProfileConstantKeepsBean() {
        assertJavaConstantProfile("UNKNOWN_PROFILE", "UNKNOWN_PROFILE", null, true)
    }

    fun testKotlinUnresolvedProfileConstantKeepsBean() {
        assertKotlinConstantProfile("UNKNOWN_PROFILE", "UNKNOWN_PROFILE", null, true)
    }

    fun testJavaResolvedActiveProfileConstantKeepsBean() {
        assertJavaConstantProfile("Profiles.DEV", "DEV", "dev", true)
    }

    fun testKotlinResolvedActiveProfileConstantKeepsBean() {
        assertKotlinConstantProfile("Profiles.DEV", "DEV", "dev", true)
    }

    fun testJavaResolvedInactiveProfileConstantHidesBean() {
        assertJavaConstantProfile("Profiles.PROD", "PROD", "prod", false)
    }

    fun testKotlinResolvedInactiveProfileConstantHidesBean() {
        assertKotlinConstantProfile("Profiles.PROD", "PROD", "prod", false)
    }

    fun testJavaUnresolvedConstantOrInactiveLiteralKeepsBean() {
        assertJavaConstantProfile("{UNKNOWN_PROFILE, \"prod\"}", "UNKNOWN_PROFILE", null, true)
    }

    fun testKotlinUnresolvedConstantOrInactiveLiteralKeepsBean() {
        assertKotlinConstantProfile("UNKNOWN_PROFILE, \"prod\"", "UNKNOWN_PROFILE", null, true)
    }

    fun testJavaResolvedActiveConstantOrInactiveLiteralKeepsBean() {
        assertJavaConstantProfile("{Profiles.DEV, \"prod\"}", "DEV", "dev", true)
    }

    fun testKotlinResolvedActiveConstantOrInactiveLiteralKeepsBean() {
        assertKotlinConstantProfile("Profiles.DEV, \"prod\"", "DEV", "dev", true)
    }

    fun testJavaConcatenatedNegatedProfileConstantKeepsBean() {
        assertJavaConstantProfile("\"!\" + Profiles.PROD", "PROD", "!prod", true)
    }

    fun testKotlinConcatenatedNegatedProfileConstantKeepsBean() {
        assertKotlinConstantProfile("\"!\" + Profiles.PROD", "PROD", "!prod", true)
    }

    private fun assertJavaConstantProfile(
        profileValue: String,
        referenceName: String,
        expectedValue: String?,
        expectedActive: Boolean
    ) {
        activate("dev")
        myFixture.addFileToProject(
            "com/app/Profiles.java",
            """
            package com.app;

            public class Profiles {
                public static final String DEV = "dev";
                public static final String PROD = "prod";
            }
            """.trimIndent()
        )
        val file = addJavaConfiguration("ConstantProfileConfig", "@Profile($profileValue)") as PsiJavaFile
        val value = file.classes.single().getAnnotation("org.springframework.context.annotation.Profile")!!
            .findDeclaredAttributeValue("value")!!
        val values = if (value is PsiArrayInitializerMemberValue) value.initializers.toList() else listOf(value)
        assertProfileConstantPrecondition(
            file,
            referenceName,
            values.map { it.toUElementOfType<UExpression>()!! },
            expectedValue
        )
        assertConstantBeanActivation(expectedActive)
    }

    private fun assertKotlinConstantProfile(
        profileValue: String,
        referenceName: String,
        expectedValue: String?,
        expectedActive: Boolean
    ) {
        activate("dev")
        addApplication()
        myFixture.addFileToProject(
            "com/app/Profiles.kt",
            """
            package com.app

            object Profiles {
                const val DEV = "dev"
                const val PROD = "prod"
            }
            """.trimIndent()
        )
        val file = myFixture.addFileToProject(
            "com/app/ConstantProfileConfig.kt",
            """
            package com.app

            import org.springframework.context.annotation.Configuration
            import org.springframework.context.annotation.Profile

            @Configuration
            @Profile($profileValue)
            class ConstantProfileConfig
            """.trimIndent()
        ) as KtFile
        val annotation = (file.declarations.single() as KtClass).annotationEntries
            .single { it.shortName?.asString() == "Profile" }
        val values = annotation.valueArguments.map { it.getArgumentExpression()!!.toUElementOfType<UExpression>()!! }
        assertProfileConstantPrecondition(file, referenceName, values, expectedValue)
        assertConstantBeanActivation(expectedActive)
    }

    private fun assertProfileConstantPrecondition(
        file: PsiFile,
        referenceName: String,
        values: List<UExpression>,
        expectedValue: String?
    ) {
        val leaf = file.findElementAt(file.text.indexOf(referenceName))!!
        val reference = when (file) {
            is PsiJavaFile -> PsiTreeUtil.getParentOfType(leaf, PsiReferenceExpression::class.java)!!
            is KtFile -> PsiTreeUtil.getParentOfType(leaf, KtNameReferenceExpression::class.java)!!.mainReference
            else -> error("Unsupported profile fixture: ${file.javaClass.name}")
        }
        if (expectedValue == null) {
            assertNull("Precondition: $referenceName must be unresolved", reference.resolve())
        } else {
            assertNotNull("Precondition: $referenceName must resolve", reference.resolve())
        }
        assertEquals("Precondition: annotation constant value", expectedValue, values.first().evaluate())
        if (values.size > 1) {
            assertEquals("Precondition: OR array has two elements", 2, values.size)
            assertEquals("Precondition: remaining profile is inactive prod", "prod", values.last().evaluate())
        }
    }

    private fun assertConstantBeanActivation(expectedActive: Boolean) {
        if (expectedActive) assertBeanActive("ConstantProfileConfig") else assertBeanInactive("ConstantProfileConfig")
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

    private fun addJavaConfiguration(className: String, profileAnnotations: String): PsiFile {
        addApplication()
        return myFixture.addFileToProject(
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
