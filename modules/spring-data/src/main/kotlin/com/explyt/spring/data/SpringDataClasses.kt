/*
 * Copyright © 2024 Explyt Ltd
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

package com.explyt.spring.data


object SpringDataClasses {
    const val QUERY = "org.springframework.data.jpa.repository.Query"

    const val PARAM = "org.springframework.data.repository.query.Param"

    const val SPRING_DATA_REST_RESOURCE = "org.springframework.data.rest.core.annotation.RepositoryRestResource"
    const val SPRING_RESOURCE = "org.springframework.data.repository.Repository"
    const val JPA_CONTEXT = "org.springframework.data.jpa.repository.JpaContext"
    const val REPOSITORY_ANNOTATION = "org.springframework.data.repository.RepositoryDefinition"
    const val REPOSITORY_CRUD = "org.springframework.data.repository.CrudRepository"
    const val REPOSITORY_JPA = "org.springframework.data.jpa.repository.JpaRepository"
    const val ENABLE_JPA_REPOSITORY = "org.springframework.data.jpa.repository.config.EnableJpaRepositories"
    const val JDBC_TEMPLATE = "org.springframework.jdbc.core.JdbcTemplate"

    const val DOMAIN_PACKAGE_PREFIX = "org.springframework.data.domain."
}