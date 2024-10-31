package com.esprito.spring.data.completion.kotlin

import com.esprito.spring.test.ExplytJavaLightTestCase
import com.esprito.spring.test.TestLibrary
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
            "TestRepository.kt",
            """
            import org.springframework.data.repository.Repository;

            interface TestRepository : Repository<Entity, Integer> {
            	fun <caret>        
            """.trimIndent()
        )

        val values = getLookupStrings()

        TestCase.assertTrue(values.any { it.startsWith("find") })
        TestCase.assertTrue(values.any { it.startsWith("read") })
        TestCase.assertTrue(values.any { it.startsWith("get") })
        TestCase.assertTrue(values.any { it.startsWith("stream") })
        TestCase.assertTrue(values.any { it.startsWith("count") })
        TestCase.assertTrue(values.any { it.startsWith("exist") })
        TestCase.assertTrue(values.any { it.startsWith("delete") })
    }

    fun testAfterTypeFind() {
        myFixture.configureByText(
            "TestRepository.kt",
            """
            import org.springframework.data.repository.Repository;

            interface TestRepository : Repository<Entity, Integer> {
            	fun find<caret>
            """.trimIndent()
        )

        val values = getLookupStrings()

        TestCase.assertTrue(values.any { it.startsWith("find") })

        TestCase.assertFalse(values.any { it.startsWith("read") })
        TestCase.assertFalse(values.any { it.startsWith("count") })
        TestCase.assertFalse(values.any { it.startsWith("exist") })
        TestCase.assertFalse(values.any { it.startsWith("delete") })
    }

    fun testAfterFindBy() {
        myFixture.configureByText(
            "TestRepository.kt",
            """
            import org.springframework.data.repository.Repository;

            public interface TestRepository : Repository<Entity, Integer> {
            	fun findBy<caret>            
            """.trimIndent()
        )

        val values = getLookupStrings()

        TestCase.assertTrue(values.any { it.startsWith("findById") })
        TestCase.assertTrue(values.any { it.startsWith("findByName") })
    }

    fun testAfterPropertyName() {
        myFixture.configureByText(
            "TestRepository.kt",
            """
            import org.springframework.data.repository.Repository;

            public interface TestRepository : Repository<Entity, Integer> {
            	fun findByName<caret>            
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