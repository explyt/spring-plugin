/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.util

object ExplytDbConstants {
    const val JDBC_POSTGRES = "org.postgresql:postgresql"
    const val JDBC_MYSQL = "mysql:mysql-connector-java"
    const val JDBC_SQLITE = "org.xerial:sqlite-jdbc"
    const val JDBC_ORACLE_8 = "com.oracle.jdbc:ojdbc8"
    const val JDBC_ORACLE_14 = "com.oracle.jdbc:ojdbc14"
    const val JDBC_H2 = "com.h2database:h2"

    const val LIQUIBASE_CORE = "org.liquibase:liquibase-core"
    const val FLYWAY_CORE = "org.flywaydb:flyway-core"
}