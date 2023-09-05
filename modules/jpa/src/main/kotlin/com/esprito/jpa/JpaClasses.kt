package com.esprito.jpa

import com.esprito.util.MultiVendorClass
import kotlin.reflect.KProperty

object JpaClasses {
    val entity by "persistence.Entity"
    val mappedSuperclass by "persistence.MappedSuperclass"
    val embeddable by "persistence.Embeddable"

    val entityManager by "persistence.EntityManager"
    val namedQuery by "persistence.NamedQuery"

    private operator fun String.getValue(jpaClasses: JpaClasses, property: KProperty<*>): MultiVendorClass {
        return MultiVendorClass(this)
    }
}
