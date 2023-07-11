package com.esprito.jpa

import kotlin.reflect.KProperty

object JpaClasses {
    val entity by "persistence.Entity"
    val mappedSuperclass by "persistence.MappedSuperclass"
    val embeddable by "persistence.Embeddable"

    // todo move to spring-data module
    val query = "org.springframework.data.jpa.repository.Query"

    class MultiVendorClass(subFqn: String) {
        val javax: String = "javax.$subFqn"
        val jakarta: String = "jakarta.$subFqn"

        val allFqns by lazy { listOf(javax, jakarta) }
    }

    private operator fun String.getValue(jpaClasses: JpaClasses, property: KProperty<*>): MultiVendorClass {
        return MultiVendorClass(this)
    }
}
