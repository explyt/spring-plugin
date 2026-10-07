/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking

/**
 * A relationship mapped through `@JoinTable` reports the join table it declares (#507).
 *
 * Only an explicit annotation is reported: the JPA default join table name depends on the primary table names and
 * on the physical naming strategy, so a relationship without `@JoinTable` carries no `joinTable` key at all.
 */
class SpringDataEntityJoinTableTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "mcp/"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.jakarta_persistence_3_1_0,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    fun testAPetclinicManyToManyReportsItsJoinTable() = runBlocking<Unit> {
        addSpecialtyEntity()
        myFixture.addFileToProject(
            "com/example/vets/Vet.java", """
            package com.example.vets;

            import java.util.Set;
            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;
            import jakarta.persistence.JoinColumn;
            import jakarta.persistence.JoinTable;
            import jakarta.persistence.ManyToMany;

            @Entity
            public class Vet {
                @Id
                private Long id;

                @ManyToMany
                @JoinTable(name = "vet_specialties", joinColumns = @JoinColumn(name = "vet_id"),
                        inverseJoinColumns = @JoinColumn(name = "specialty_id"))
                private Set<Specialty> specialties;
            }
            """.trimIndent()
        )

        val specialties = relationshipField("com.example.vets.Vet", "specialties", "MANY_TO_MANY")

        assertEquals(
            json("""{"name":"vet_specialties","joinColumns":[{"name":"vet_id"}],"inverseJoinColumns":[{"name":"specialty_id"}]}"""),
            joinTableOf(specialties)
        )
    }

    fun testCompositeJoinColumnsAreReportedInDeclarationOrder() = runBlocking<Unit> {
        addSpecialtyEntity()
        myFixture.addFileToProject(
            "com/example/vets/Clinic.java", """
            package com.example.vets;

            import java.util.List;
            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;
            import jakarta.persistence.JoinColumn;
            import jakarta.persistence.JoinTable;
            import jakarta.persistence.OneToMany;

            @Entity
            public class Clinic {
                @Id
                private Long id;

                @OneToMany
                @JoinTable(
                        name = "clinic_specialties",
                        joinColumns = {@JoinColumn(name = "clinic_region"), @JoinColumn(name = "clinic_code")},
                        inverseJoinColumns = {@JoinColumn(name = "specialty_id"), @JoinColumn(name = "specialty_rev")})
                private List<Specialty> specialties;
            }
            """.trimIndent()
        )

        val specialties = relationshipField("com.example.vets.Clinic", "specialties", "ONE_TO_MANY")

        assertEquals(
            json(
                """{"name":"clinic_specialties",
                   "joinColumns":[{"name":"clinic_region"},{"name":"clinic_code"}],
                   "inverseJoinColumns":[{"name":"specialty_id"},{"name":"specialty_rev"}]}"""
            ),
            joinTableOf(specialties)
        )
    }

    fun testQuotedJoinTableNamesAreReportedWithoutTheirDelimitersAndFlagged() = runBlocking<Unit> {
        addSpecialtyEntity()
        myFixture.addFileToProject(
            "com/example/vets/Surgeon.java", """
            package com.example.vets;

            import java.util.Set;
            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;
            import jakarta.persistence.JoinColumn;
            import jakarta.persistence.JoinTable;
            import jakarta.persistence.ManyToMany;

            @Entity
            public class Surgeon {
                @Id
                private Long id;

                @ManyToMany
                @JoinTable(name = "`SurgeonSpecialties`", joinColumns = @JoinColumn(name = "\"SurgeonId\""),
                        inverseJoinColumns = @JoinColumn(name = "[SpecialtyId]"))
                private Set<Specialty> specialties;
            }
            """.trimIndent()
        )

        val specialties = relationshipField("com.example.vets.Surgeon", "specialties", "MANY_TO_MANY")

        assertEquals(
            json(
                """{"name":"SurgeonSpecialties","quoted":true,
                   "joinColumns":[{"name":"SurgeonId","quoted":true}],
                   "inverseJoinColumns":[{"name":"SpecialtyId","quoted":true}]}"""
            ),
            joinTableOf(specialties)
        )
    }

    fun testAKotlinJoinTableWithArrayLiteralsIsReported() = runBlocking<Unit> {
        addSpecialtyEntity()
        myFixture.addFileToProject(
            "com/example/vets/Nurse.kt", """
            package com.example.vets

            import jakarta.persistence.Entity
            import jakarta.persistence.Id
            import jakarta.persistence.JoinColumn
            import jakarta.persistence.JoinTable
            import jakarta.persistence.ManyToMany

            @Entity
            class Nurse {
                @Id
                var id: Long? = null

                @ManyToMany
                @JoinTable(
                    name = "nurse_specialties",
                    joinColumns = [JoinColumn(name = "nurse_id")],
                    inverseJoinColumns = [JoinColumn(name = "specialty_id")]
                )
                var specialties: MutableSet<Specialty> = mutableSetOf()
            }
            """.trimIndent()
        )

        val specialties = relationshipField("com.example.vets.Nurse", "specialties", "MANY_TO_MANY")

        assertEquals(
            json("""{"name":"nurse_specialties","joinColumns":[{"name":"nurse_id"}],"inverseJoinColumns":[{"name":"specialty_id"}]}"""),
            joinTableOf(specialties)
        )
    }

    fun testAnInverseManyToManyWithoutJoinTableReportsOnlyMappedBy() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/vets/Specialty.java", """
            package com.example.vets;

            import java.util.Set;
            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;
            import jakarta.persistence.ManyToMany;

            @Entity
            public class Specialty {
                @Id
                private Long id;

                @ManyToMany(mappedBy = "specialties")
                private Set<Specialty> vets;
            }
            """.trimIndent()
        )

        val vets = relationshipField("com.example.vets.Specialty", "vets", "MANY_TO_MANY")

        assertEquals("specialties", vets["mappedBy"].asText())
        assertFalse("The inverse side declares no join table, got $vets", vets.has("joinTable"))
    }

    fun testAnOwningManyToManyWithoutJoinTableInventsNoDefaultName() = runBlocking<Unit> {
        addSpecialtyEntity()
        myFixture.addFileToProject(
            "com/example/vets/Intern.java", """
            package com.example.vets;

            import java.util.Set;
            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;
            import jakarta.persistence.ManyToMany;

            @Entity
            public class Intern {
                @Id
                private Long id;

                @ManyToMany
                private Set<Specialty> specialties;
            }
            """.trimIndent()
        )

        val specialties = relationshipField("com.example.vets.Intern", "specialties", "MANY_TO_MANY")

        assertFalse("A default join table name must not be guessed, got $specialties", specialties.has("joinTable"))
    }

    fun testAnOrdinaryColumnKeepsExactlyItsKeys() = runBlocking<Unit> {
        addSpecialtyEntity()

        val name = field(fieldsOf("com.example.vets.Specialty"), "name")

        assertEquals(
            setOf("name", "type", "column", "primaryKey", "nullable", "relationship", "joinColumn", "mappedBy"),
            keysOf(name)
        )
    }

    private fun addSpecialtyEntity() {
        myFixture.addFileToProject(
            "com/example/vets/Specialty.java", """
            package com.example.vets;

            import jakarta.persistence.Column;
            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;

            @Entity
            public class Specialty {
                @Id
                private Long id;

                @Column(name = "name")
                private String name;
            }
            """.trimIndent()
        )
    }

    private suspend fun relationshipField(className: String, fieldName: String, relationship: String): JsonNode {
        val found = field(fieldsOf(className), fieldName)
        assertEquals("Precondition: $fieldName must be a $relationship, got $found", relationship, found["relationship"]?.asText())
        return found
    }

    private fun joinTableOf(relationshipField: JsonNode): JsonNode? =
        relationshipField["joinTable"].also {
            assertNotNull("No joinTable reported for ${relationshipField["name"].asText()}, got $relationshipField", it)
        }

    private suspend fun fieldsOf(className: String): JsonNode {
        val page = mapper.readTree(
            toolset.getSpringDataEntities(
                projectPath = project.basePath ?: "",
                className = className,
                includeDetails = true
            )
        )
        assertEquals("Precondition: the entity must be found, got $page", 1, page["totalCount"].asInt())
        return page["entities"][0]["fields"]
    }

    private fun json(text: String): JsonNode = mapper.readTree(text)

    private fun keysOf(node: JsonNode): Set<String> = node.fieldNames().asSequence().toSet()

    private fun field(fields: JsonNode, name: String): JsonNode = fields.single { it["name"].asText() == name }
}
