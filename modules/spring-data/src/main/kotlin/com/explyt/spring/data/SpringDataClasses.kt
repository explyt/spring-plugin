/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.data


object SpringDataClasses {
    const val QUERY = "org.springframework.data.jpa.repository.Query"

    const val PARAM = "org.springframework.data.repository.query.Param"

    const val SPRING_DATA_REST_RESOURCE = "org.springframework.data.rest.core.annotation.RepositoryRestResource"
    const val REPOSITORY = "org.springframework.data.repository.Repository"
    const val JPA_CONTEXT = "org.springframework.data.jpa.repository.JpaContext"
    const val REPOSITORY_ANNOTATION = "org.springframework.data.repository.RepositoryDefinition"
    const val REPOSITORY_CRUD = "org.springframework.data.repository.CrudRepository"
    const val REPOSITORY_JPA = "org.springframework.data.jpa.repository.JpaRepository"
    const val ENABLE_JPA_REPOSITORY = "org.springframework.data.jpa.repository.config.EnableJpaRepositories"
    const val JDBC_TEMPLATE = "org.springframework.jdbc.core.JdbcTemplate"
    const val JDBC_TEMPLATE_NAMED_PARAMETER = "org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate"
    const val JDBC_CLIENT = "org.springframework.jdbc.core.simple.JdbcClient"
    const val JDBC_CLIENT_STATEMENT = "org.springframework.jdbc.core.simple.JdbcClient.StatementSpec"

    const val DOMAIN_PACKAGE_PREFIX = "org.springframework.data.domain."

    const val DATA_COMMON_MAVEN = "org.springframework.boot:spring-boot-data-commons"
}