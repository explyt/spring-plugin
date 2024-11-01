/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.security.references.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiPolyVariantReference

private const val TEST_DATA_PATH = "references"

class WithUserDetailsAnnotationReferenceTest : ExplytJavaLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springSecurityTest_6_0_7
    )

    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    fun testVariants() {
        val vf = myFixture.configureByText(
            "MainWithUser.java",
            """
            import org.springframework.security.core.userdetails.UserDetails;
            import org.springframework.security.core.userdetails.UserDetailsService;
            import org.springframework.security.core.userdetails.UsernameNotFoundException;
            import org.springframework.security.test.context.support.WithUserDetails;
            import org.springframework.stereotype.Component;
            
            @Component
            public class MainWithUser {

                @WithUserDetails(value="admin@example.com", userDetailsServiceBeanName = "adm<caret>")
                public void getMessageAdmin() {
                }
            }

        """.trimIndent()
        )

        myFixture.copyFileToProject("UserDetailsConfiguration.java")

        myFixture.configureFromExistingVirtualFile(vf.virtualFile)
        myFixture.complete(CompletionType.BASIC)

        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull(lookupElementStrings)
        assertEquals(
            setOf(
                "adminMain",
                "adminSecond"
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
            public class MainWithUser {

                @WithUserDetails(value="admin@example.com", userDetailsServiceBeanName = "adm<caret>in")
                public void getMessageAdmin() {
                }

                @WithUserDetails(value="test@example.com", userDetailsServiceBeanName = "test")
                public void getTest() {
                }
            }

        """.trimIndent()
        )

        myFixture.copyFileToProject("UserDetailsConfiguration2.java")

        val ref = file.findReferenceAt(myFixture.caretOffset) as? PsiPolyVariantReference
        assertNotNull(ref)

        val multiResolve = ref!!.multiResolve(true)
        assertEquals(2, multiResolve.size)
        multiResolve
            .asSequence()
            .map { it.element as PsiMember }
            .forEach { assertTrue((it.name == "admin" || it.name == "adminUserDetailsService")) }
    }


}