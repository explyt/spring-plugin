package com.esprito.jpa

import kotlin.reflect.KProperty

object JpaClasses {
    val entity by "persistence.Entity"
    val mappedSuperclass by "persistence.MappedSuperclass"
    val embeddable by "persistence.Embeddable"

    val entityManager by "persistence.EntityManager"
    val namedQuery by "persistence.NamedQuery"

    class MultiVendorClass(subFqn: String) {
        val javax: String = "javax.$subFqn"
        val jakarta: String = "jakarta.$subFqn"

        val allFqns by lazy { listOf(javax, jakarta) }

        fun check(fqn: String?): Boolean = fqn in allFqns
    }

    private operator fun String.getValue(jpaClasses: JpaClasses, property: KProperty<*>): MultiVendorClass {
        return MultiVendorClass(this)
    }
}
