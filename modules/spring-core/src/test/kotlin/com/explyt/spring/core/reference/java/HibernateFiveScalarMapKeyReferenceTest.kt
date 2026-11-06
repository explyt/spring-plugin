/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.reference.java

import com.explyt.spring.test.TestLibrary

class HibernateFiveScalarMapKeyReferenceTest : ScalarMapKeyReferenceFixture() {
    override val libraries: Array<TestLibrary> = super.libraries + TestLibrary.hibernate_5_6_15

    fun testPropertiesHibernateShowSqlResolvesToAvailableSettings() {
        configureProperties("spring.jpa.properties.hibernate.sh<caret>ow_sql=true")
        assertField("org.hibernate.cfg.AvailableSettings", "SHOW_SQL", "hibernate.show_sql")
    }
}
