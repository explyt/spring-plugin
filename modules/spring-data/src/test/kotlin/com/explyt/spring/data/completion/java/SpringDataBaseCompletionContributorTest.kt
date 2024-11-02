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

package com.explyt.spring.data.completion.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.completion.CompletionType
import junit.framework.TestCase

class SpringDataBaseCompletionContributorTest : ExplytJavaLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1, TestLibrary.springContext_6_0_7, TestLibrary.springDataJpa_3_1_0
    )

    override fun setUp() {
        super.setUp()
        myFixture.addClass(
            """
            public class Entity {
            		public Integer id;
            		public String name;
                    public String city;
            }
        """.trimIndent()
        )
    }
    
    fun testAfterType() {
        myFixture.configureByText(
            "TestRepository.java",
            """
            import org.springframework.data.repository.Repository;

            public interface TestRepository extends Repository<Entity, Integer> {
            	Entity <caret>
            """.trimIndent()
        )

        val values = getLookupStrings()

        TestCase.assertTrue(values.any { it.startsWith("find") })
        TestCase.assertTrue(values.any { it.startsWith("read") })
        TestCase.assertTrue(values.any { it.startsWith("get") })
        TestCase.assertTrue(values.any { it.startsWith("stream") })

        TestCase.assertFalse(values.any { it.startsWith("count") })
        TestCase.assertFalse(values.any { it.startsWith("exist") })
        TestCase.assertFalse(values.any { it.startsWith("delete") })
    }

    fun testAfterTypeFind() {
        myFixture.configureByText(
            "TestRepository.java",
            """
            import org.springframework.data.repository.Repository;

            public interface TestRepository extends Repository<Entity, Integer> {
            	Entity find<caret>            
            """.trimIndent()
        )

        val values = getLookupStrings()

        TestCase.assertTrue(values.any { it.startsWith("find") })

        TestCase.assertFalse(values.any { it.startsWith("read") })
        TestCase.assertFalse(values.any { it.startsWith("count") })
        TestCase.assertFalse(values.any { it.startsWith("exist") })
        TestCase.assertFalse(values.any { it.startsWith("delete") })
    }

    fun testAfterTypeInt() {
        myFixture.configureByText(
            "TestRepository.java",
            """
            import org.springframework.data.repository.Repository;

            public interface TestRepository extends Repository<Entity, Integer> {
            	int <caret>            
            """.trimIndent()
        )

        val values = getLookupStrings()

        TestCase.assertTrue(values.any { it.startsWith("count") })
        TestCase.assertTrue(values.any { it.startsWith("delete") })

        TestCase.assertFalse(values.any { it.startsWith("exist") })
        TestCase.assertFalse(values.any { it.startsWith("find") })
    }

    fun testAfterTypeBoolean() {
        myFixture.configureByText(
            "TestRepository.java",
            """
            import org.springframework.data.repository.Repository;

            public interface TestRepository extends Repository<Entity, Integer> {
            	Boolean <caret>            
            """.trimIndent()
        )

        val values = getLookupStrings()

        TestCase.assertTrue(values.any { it.startsWith("exist") })

        TestCase.assertFalse(values.any { it.startsWith("count") })
        TestCase.assertFalse(values.any { it.startsWith("delete") })
        TestCase.assertFalse(values.any { it.startsWith("find") })
    }

    fun testAfterTypeVoid() {
        myFixture.configureByText(
            "TestRepository.java",
            """
            import org.springframework.data.repository.Repository;

            public interface TestRepository extends Repository<Entity, Integer> {
            	void <caret>          
            """.trimIndent()
        )

        val values = getLookupStrings()

        TestCase.assertTrue(values.any { it.startsWith("delete") })

        TestCase.assertFalse(values.any { it.startsWith("exist") })
        TestCase.assertFalse(values.any { it.startsWith("count") })
        TestCase.assertFalse(values.any { it.startsWith("find") })
    }

    fun testAfterFindBy() {
        myFixture.configureByText(
            "TestRepository.java",
            """
            import org.springframework.data.repository.Repository;

            public interface TestRepository extends Repository<Entity, Integer> {
            	Entity findBy<caret>            
            """.trimIndent()
        )

        val values = getLookupStrings()

        TestCase.assertTrue(values.any { it.startsWith("findById") })
        TestCase.assertTrue(values.any { it.startsWith("findByName") })
    }

    fun testAfterPropertyName() {
        myFixture.configureByText(
            "TestRepository.java",
            """
            import org.springframework.data.repository.Repository;

            public interface TestRepository extends Repository<Entity, Integer> {
            	Entity findByName<caret>            
            """.trimIndent()
        )

        val values = getLookupStrings()

        TestCase.assertTrue(values.any { it.startsWith("findByNameAnd") })
        TestCase.assertTrue(values.any { it.startsWith("findByNameOr") })
        TestCase.assertTrue(values.any { it.startsWith("findByNameEquals") })
        TestCase.assertTrue(values.any { it.startsWith("findByNameBefore") })
    }

    private fun getLookupStrings(): Set<String> {
        val lookupElements = myFixture.complete(CompletionType.BASIC)
        assertNotNull(lookupElements)
        return lookupElements.mapTo(mutableSetOf()) { it.lookupString }
    }
}