package com.esprito.spring.security.references.kotlin

import com.esprito.spring.test.ExplytKotlinLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiPolyVariantReference

private const val TEST_DATA_PATH = "references"

class WithUserDetailsAnnotationReferenceTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springSecurityTest_6_0_7
    )

    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    fun testVariants() {
        val vf = myFixture.configureByText(
            "MainWithUser.kt",
            """
            import org.springframework.security.core.userdetails.UserDetails;
            import org.springframework.security.core.userdetails.UserDetailsService;
            import org.springframework.security.core.userdetails.UsernameNotFoundException;
            import org.springframework.security.test.context.support.WithUserDetails;
            import org.springframework.stereotype.Component;
            
            @Component
            class WithUserKotlin {
                @WithUserDetails(value = "admin@example.com", userDetailsServiceBeanName = "kotlinA<caret>")
                fun getMessageAdmin() {
                }
            }
        """.trimIndent()
        )

        myFixture.copyFileToProject("UserDetailsConfiguration.kt")

        myFixture.configureFromExistingVirtualFile(vf.virtualFile)
        myFixture.complete(CompletionType.BASIC)

        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull(lookupElementStrings)
        assertEquals(
            setOf(
                "kotlinAdmin",
                "kotlinAdminUserDetailsService"
            ), lookupElementStrings!!.toSet()
        )
    }

    fun testResolve() {
        myFixture.configureByText(
            "MainWithUser.java",
            """
            import org.springframework.security.core.userdetails.UserDetails;
            import org.springframework.security.core.userdetails.UserDetailsService;
            import org.springframework.security.core.userdetails.UsernameNotFoundException;
            import org.springframework.security.test.context.support.WithUserDetails;
            import org.springframework.stereotype.Component;
            
            @Component
            class WithUserKotlin {
                @WithUserDetails(value = "admin@example.com", userDetailsServiceBeanName = "kotlin<caret>Admin")
                fun getMessageAdmin() {
                }
            }
        """.trimIndent()
        )

        myFixture.copyFileToProject("UserDetailsConfiguration2.kt")

        val ref = file.findReferenceAt(myFixture.caretOffset) as? PsiPolyVariantReference
        assertNotNull(ref)

        val multiResolve = ref!!.multiResolve(true)
        assertEquals(2, multiResolve.size)
        multiResolve
            .asSequence()
            .map { it.element as PsiMember }
            .forEach { assertTrue((it.name == "kotlinAdmin" || it.name == "kotlinAdminUserDetailsService")) }
    }

}