/*
 * Copyright © 2025 Explyt Ltd
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