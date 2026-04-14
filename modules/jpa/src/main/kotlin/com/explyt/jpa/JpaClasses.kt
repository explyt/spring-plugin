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

    private operator fun String.getValue(jpaClasses: JpaClasses, property: KProperty<*>): MultiVendorClass {
        return MultiVendorClass(this)
    }
}
