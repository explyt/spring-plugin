/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.profile

import com.explyt.spring.core.service.ProfileActivation
import com.explyt.spring.core.service.ProfilesService
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.TestLibrarySourceRoot
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
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

    fun testJavaUnresolvedProfileConstantIsUndecided() {
        assertJavaConstantProfile("UNKNOWN_PROFILE", "UNKNOWN_PROFILE", null, true, ProfileActivation.UNDECIDED)
    }

    fun testKotlinUnresolvedProfileConstantIsUndecided() {
        assertKotlinConstantProfile("UNKNOWN_PROFILE", "UNKNOWN_PROFILE", null, true, ProfileActivation.UNDECIDED)
    }

    fun testJavaResolvedActiveProfileConstantIsActive() {
        assertJavaConstantProfile("Profiles.DEV", "DEV", "dev", true, ProfileActivation.ACTIVE)
    }

    fun testKotlinResolvedActiveProfileConstantIsActive() {
        assertKotlinConstantProfile("Profiles.DEV", "DEV", "dev", true, ProfileActivation.ACTIVE)
    }

    fun testJavaResolvedInactiveProfileConstantIsInactive() {
        assertJavaConstantProfile("Profiles.PROD", "PROD", "prod", false, ProfileActivation.INACTIVE)
    }

    fun testKotlinResolvedInactiveProfileConstantIsInactive() {
        assertKotlinConstantProfile("Profiles.PROD", "PROD", "prod", false, ProfileActivation.INACTIVE)
    }

    fun testJavaUnresolvedConstantOrInactiveLiteralIsUndecided() {
        assertJavaConstantProfile(
            "{UNKNOWN_PROFILE, \"prod\"}",
            "UNKNOWN_PROFILE",
            null,
            true,
            ProfileActivation.UNDECIDED
        )
    }

    fun testKotlinUnresolvedConstantOrInactiveLiteralIsUndecided() {
        assertKotlinConstantProfile(
            "UNKNOWN_PROFILE, \"prod\"",
            "UNKNOWN_PROFILE",
            null,
            true,
            ProfileActivation.UNDECIDED
        )
    }

    fun testJavaResolvedActiveConstantOrInactiveLiteralIsActive() {
        assertJavaConstantProfile("{Profiles.DEV, \"prod\"}", "DEV", "dev", true, ProfileActivation.ACTIVE)
    }

    fun testKotlinResolvedActiveConstantOrInactiveLiteralIsActive() {
        assertKotlinConstantProfile("Profiles.DEV, \"prod\"", "DEV", "dev", true, ProfileActivation.ACTIVE)
    }

    fun testJavaConcatenatedNegatedProfileConstantKeepsBean() {
        assertJavaConstantProfile("\"!\" + Profiles.PROD", "PROD", "!prod", true, ProfileActivation.ACTIVE)
    }

    fun testKotlinConcatenatedNegatedProfileConstantKeepsBean() {
        assertKotlinConstantProfile("\"!\" + Profiles.PROD", "PROD", "!prod", true, ProfileActivation.ACTIVE)
    }

    fun testJavaComponentWithoutProfileIsActive() {
        activate("dev")
        val file = addJavaConfiguration("NoProfileConfig", "") as PsiJavaFile

        assertNull(
            "Precondition: no profile annotation",
            file.classes.single().getAnnotation("org.springframework.context.annotation.Profile")
        )
        assertProfileActivation(file.classes.single(), ProfileActivation.ACTIVE)
        assertBeanActive("NoProfileConfig")
    }

    fun testJavaEmptyProfileArrayIsActive() {
        activate("dev")
        val file = addJavaConfiguration("EmptyProfileContractConfig", "@Profile({})") as PsiJavaFile

        assertSourceProfileValues(file.classes.single(), emptyList())
        assertProfileActivation(file.classes.single(), ProfileActivation.ACTIVE)
        assertBeanActive("EmptyProfileContractConfig")
    }

    fun testKotlinComponentWithoutProfileIsActive() {
        assertKotlinProfileBoundary("", false)
    }

    fun testKotlinEmptyProfileArrayIsActive() {
        assertKotlinProfileBoundary("@Profile(value = [])", true)
    }

    private fun assertKotlinProfileBoundary(profile: String, hasProfile: Boolean) {
        activate("dev")
        addApplication()
        val file = myFixture.addFileToProject(
            "com/app/BoundaryConfig.kt",
            """
            package com.app
            import org.springframework.context.annotation.Configuration
            import org.springframework.context.annotation.Profile
            @Configuration
            $profile
            class BoundaryConfig
            """.trimIndent()
        ) as KtFile
        val klass = file.declarations.single()
        if (hasProfile) assertSourceProfileValues(klass, emptyList())
        else assertTrue(
            "Precondition: no Profile annotation",
            klass.toUElementOfType<UClass>()!!.uAnnotations.none { it.qualifiedName == "org.springframework.context.annotation.Profile" })
        assertProfileActivation(myFixture.findClass("com.app.BoundaryConfig"), ProfileActivation.ACTIVE)
        assertBeanActive("BoundaryConfig")
    }

    fun testJavaMetaAnnotationWithResolvedProfileConstantIsActive() {
        assertResolvedMetaProfile(false)
    }

    fun testKotlinMetaAnnotationWithResolvedProfileConstantIsActive() {
        assertResolvedMetaProfile(true)
    }

    private fun assertResolvedMetaProfile(kotlin: Boolean) {
        activate("dev")
        if (kotlin) addApplication()
        val annotationFile = if (kotlin) {
            myFixture.addFileToProject(
                "com/app/Profiles.kt",
                "package com.app\nobject Profiles { const val DEV = \"dev\" }"
            )
            myFixture.addFileToProject(
                "com/app/Dev.kt",
                "package com.app\nimport org.springframework.context.annotation.Profile\n@Profile(Profiles.DEV) annotation class Dev"
            )
        } else {
            myFixture.addFileToProject(
                "com/app/Profiles.java",
                "package com.app; public class Profiles { public static final String DEV = \"dev\"; }"
            )
            myFixture.addFileToProject(
                "com/app/Dev.java",
                "package com.app; import org.springframework.context.annotation.Profile; @Profile(Profiles.DEV) public @interface Dev {}"
            )
        }
        val source = when (annotationFile) {
            is KtFile -> annotationFile.declarations.single()
            is PsiJavaFile -> annotationFile.classes.single()
            else -> error("Unexpected fixture")
        }
        assertSourceProfileValues(source, listOf("dev"))
        if (kotlin) {
            myFixture.addFileToProject(
                "com/app/ResolvedMetaConfig.kt",
                "package com.app\nimport org.springframework.context.annotation.Configuration\n@Dev @Configuration class ResolvedMetaConfig"
            )
        } else addJavaConfiguration("ResolvedMetaConfig", "@Dev")
        assertProfileActivation(myFixture.findClass("com.app.ResolvedMetaConfig"), ProfileActivation.ACTIVE)
        assertBeanActive("ResolvedMetaConfig")
    }

    fun testImportedLibraryProfileWithoutModuleIsUndecidedButNotVisible() {
        activate("dev")
        val library = TestLibrarySourceRoot.create(module, testRootDisposable, "profile-constant-library")
        val libraryFile = library.addFile(
            "com/app/LibraryProfileConfig.java",
            """
            package com.app;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.context.annotation.Profile;
            @Configuration @Profile("prod") public class LibraryProfileConfig {}
            """.trimIndent()
        ) as PsiJavaFile
        val libraryClass = libraryFile.classes.single()
        assertNull(
            "Precondition: library class has no project module",
            ModuleUtilCore.findModuleForPsiElement(libraryClass)
        )
        assertSourceProfileValues(libraryClass, listOf("prod"))
        addJavaConfiguration(
            "LibraryImportConfig",
            "@org.springframework.context.annotation.Import(LibraryProfileConfig.class)"
        )
        assertProfileActivation(libraryClass, ProfileActivation.UNDECIDED)
        assertBeanInactive("LibraryProfileConfig")
    }

    fun testJavaMetaAnnotationWithUnresolvedProfileIsUndecided() {
        activate("dev")
        val annotationFile = myFixture.addFileToProject(
            "com/app/Dev.java",
            """
            package com.app;

            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import org.springframework.context.annotation.Profile;

            @Retention(RetentionPolicy.RUNTIME)
            @Profile({UNKNOWN_PROFILE, "prod"})
            public @interface Dev {
            }
            """.trimIndent()
        )
        val file = addJavaConfiguration("MetaConstantConfig", "@Dev") as PsiJavaFile

        assertSourceProfileValues((annotationFile as PsiJavaFile).classes.single(), listOf(null, "prod"))
        assertProfileActivation(file.classes.single(), ProfileActivation.UNDECIDED)
        assertBeanActive("MetaConstantConfig")
    }

    fun testKotlinMetaAnnotationWithUnresolvedProfileIsUndecided() {
        activate("dev")
        addApplication()
        val annotationFile = myFixture.addFileToProject(
            "com/app/Dev.kt",
            """
            package com.app

            import org.springframework.context.annotation.Profile

            @Profile(UNKNOWN_PROFILE, "prod")
            annotation class Dev
            """.trimIndent()
        )
        val file = myFixture.addFileToProject(
            "com/app/MetaConstantConfig.kt",
            """
            package com.app

            import org.springframework.context.annotation.Configuration

            @Dev
            @Configuration
            class MetaConstantConfig
            """.trimIndent()
        ) as KtFile
        assertSourceProfileValues((annotationFile as KtFile).declarations.single(), listOf(null, "prod"))

        assertProfileActivation(myFixture.findClass("com.app.MetaConstantConfig"), ProfileActivation.UNDECIDED)
        assertBeanActive("MetaConstantConfig")
    }

    fun testKotlinStringTemplateProfileConstantIsActive() {
        assertKotlinExtraProfile(
            "\"!" + '$' + "{Profiles.PROD}\"",
            "object Profiles { const val PROD = \"prod\" }",
            ProfileActivation.ACTIVE
        )
    }

    fun testKotlinTopLevelProfileConstantIsActive() {
        assertKotlinExtraProfile("DEV", "const val DEV = \"dev\"", ProfileActivation.ACTIVE)
    }

    fun testKotlinCompanionProfileConstantIsActive() {
        assertKotlinExtraProfile(
            "Profiles.DEV",
            "class Profiles { companion object { const val DEV = \"dev\" } }",
            ProfileActivation.ACTIVE
        )
    }

    fun testKotlinNamedProfileValueWithConstantIsActive() {
        assertKotlinExtraProfile(
            "value = [\"prod\", Profiles.DEV]",
            "object Profiles { const val DEV = \"dev\" }",
            ProfileActivation.ACTIVE
        )
    }

    fun testJavaStaticImportedProfileConstantIsActive() {
        activate("dev")
        addApplication()
        myFixture.addFileToProject(
            "com/app/Profiles.java",
            "package com.app; public class Profiles { public static final String DEV = \"dev\"; }"
        )
        val file = myFixture.addFileToProject(
            "com/app/StaticImportConfig.java",
            """
            package com.app;
            import static com.app.Profiles.DEV;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.context.annotation.Profile;
            @Configuration @Profile(DEV) public class StaticImportConfig {}
            """.trimIndent()
        ) as PsiJavaFile
        assertSourceProfileValues(file.classes.single(), listOf("dev"))
        assertProfileActivation(file.classes.single(), ProfileActivation.ACTIVE)
        assertBeanActive("StaticImportConfig")
    }

    private fun assertKotlinExtraProfile(profileValue: String, constants: String, expected: ProfileActivation) {
        activate("dev")
        addApplication()
        myFixture.addFileToProject("com/app/Profiles.kt", "package com.app\n$constants")
        val file = myFixture.addFileToProject(
            "com/app/ExtraProfileConfig.kt",
            """
            package com.app
            import org.springframework.context.annotation.Configuration
            import org.springframework.context.annotation.Profile
            @Configuration
            @Profile($profileValue)
            class ExtraProfileConfig
            """.trimIndent()
        ) as KtFile
        val expectedValues = when {
            profileValue.startsWith("value") -> listOf("prod", "dev")
            profileValue.contains("PROD") -> listOf("!prod")
            else -> listOf("dev")
        }
        assertSourceProfileValues(file.declarations.single(), expectedValues)
        assertProfileActivation(myFixture.findClass("com.app.ExtraProfileConfig"), expected)
        assertBeanActive("ExtraProfileConfig")
    }

    private fun assertJavaConstantProfile(
        profileValue: String,
        referenceName: String,
        expectedValue: String?,
        expectedActive: Boolean,
        expectedActivation: ProfileActivation
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
        assertProfileActivation(file.classes.single(), expectedActivation)
    }

    private fun assertKotlinConstantProfile(
        profileValue: String,
        referenceName: String,
        expectedValue: String?,
        expectedActive: Boolean,
        expectedActivation: ProfileActivation
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
        assertProfileActivation(myFixture.findClass("com.app.ConstantProfileConfig"), expectedActivation)
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

    private fun assertProfileActivation(member: PsiMember, expected: ProfileActivation) {
        assertEquals(expected, SpringSearchService.getInstance(project).profileActivation(member))
    }

    private fun assertSourceProfileValues(source: com.intellij.psi.PsiElement, expected: List<String?>) {
        val annotated = source.toUElementOfType<UClass>()!!
        val annotation =
            annotated.uAnnotations.single { it.qualifiedName == "org.springframework.context.annotation.Profile" }
        val attribute = annotation.findDeclaredAttributeValue("value")!!
        val values = if (attribute is UCallExpression) attribute.valueArguments else listOf(attribute)
        assertEquals("Precondition: complete source profile values", expected, values.map { it.evaluate() as? String })
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
