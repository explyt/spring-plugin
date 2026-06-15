/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.jpa

import com.explyt.util.MultiVendorClass
import kotlin.reflect.KProperty

object JpaClasses {
    val entity by "persistence.Entity"
    val mappedSuperclass by "persistence.MappedSuperclass"
    val embeddable by "persistence.Embeddable"

    val entityManager by "persistence.EntityManager"
    val namedQuery by "persistence.NamedQuery"
    val namedNativeQuery by "persistence.NamedNativeQuery"

    val table by "persistence.Table"
    val column by "persistence.Column"
    val id by "persistence.Id"
    val embeddedId by "persistence.EmbeddedId"
    val transient by "persistence.Transient"
    val joinColumn by "persistence.JoinColumn"
    val oneToOne by "persistence.OneToOne"
    val oneToMany by "persistence.OneToMany"
    val manyToOne by "persistence.ManyToOne"
    val manyToMany by "persistence.ManyToMany"

    private operator fun String.getValue(jpaClasses: JpaClasses, property: KProperty<*>): MultiVendorClass {
        return MultiVendorClass(this)
    }
}
